package com.neomaverick.stride.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.neomaverick.stride.MainActivity
import com.neomaverick.stride.R
import com.neomaverick.stride.data.Days
import com.neomaverick.stride.data.Habit
import com.neomaverick.stride.data.StrideDb
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** One WorkManager job per habit reminder; each job reschedules itself for the next due day. */
object Reminders {
    const val CHANNEL = "reminders"

    fun createChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun schedule(context: Context, habit: Habit) {
        val wm = WorkManager.getInstance(context)
        val name = "habit-${habit.id}"
        val minutes = habit.reminderMinutes
        if (minutes == null || habit.archived) { wm.cancelUniqueWork(name); return }
        val delay = delayToNext(habit, minutes, LocalDateTime.now())
        wm.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("id" to habit.id))
                .build())
    }

    fun cancel(context: Context, habitId: Long) = WorkManager.getInstance(context).cancelUniqueWork("habit-$habitId")

    /** Time until the next scheduled day at [minutes] past midnight. */
    fun delayToNext(habit: Habit, minutes: Int, now: LocalDateTime): Duration {
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val at = date.atStartOfDay().plusMinutes(minutes.toLong())
            if (at.isAfter(now) && habit.isDueOn(date.toEpochDay())) return Duration.between(now, at)
        }
        return Duration.ofDays(1)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong("id", -1)
        val db = StrideDb.get(applicationContext)
        val habit = db.habits().active().firstOrNull { it.id == id } ?: return Result.success()
        val today = Days.today()
        val alreadyDone = db.logs().since(today).any { it.habitId == id }
        if (!alreadyDone && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            val open = PendingIntent.getActivity(applicationContext, id.toInt(),
                Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            NotificationManagerCompat.from(applicationContext).notify(id.toInt(),
                NotificationCompat.Builder(applicationContext, Reminders.CHANNEL)
                    .setSmallIcon(R.drawable.ic_stat_stride)
                    .setContentTitle("${habit.emoji} ${habit.name}")
                    .setContentText("Keep your streak going today.")
                    .setContentIntent(open).setAutoCancel(true).build())
        }
        Reminders.schedule(applicationContext, habit)
        return Result.success()
    }
}
