package com.neomaverick.stride.sync

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.neomaverick.stride.BuildConfig
import com.neomaverick.stride.data.Habit
import com.neomaverick.stride.data.HabitLog
import com.neomaverick.stride.data.Workout
import kotlinx.coroutines.tasks.await

/** Backup of the local data. The app is offline-first: every write lands in Room first and sync is best-effort. */
interface CloudSync {
    val enabled: Boolean
    suspend fun pushHabit(h: Habit) {}
    suspend fun deleteHabit(id: Long) {}
    suspend fun pushLog(log: HabitLog, done: Boolean) {}
    suspend fun pushWorkout(w: Workout) {}

    companion object {
        /** Firestore when the project keys are set at build time, otherwise a no-op so the demo needs no account. */
        fun create(context: Context, deviceId: String): CloudSync =
            if (BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()) FirestoreSync(context, deviceId) else Off
    }

    object Off : CloudSync { override val enabled = false }
}

/** Mirrors each user's data under users/{id}/... in Cloud Firestore (which also queues writes while offline). */
class FirestoreSync(context: Context, private val userId: String) : CloudSync {
    override val enabled = true
    private val db: FirebaseFirestore by lazy {
        val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .build(),
        )
        FirebaseFirestore.getInstance(app)
    }
    private fun user() = db.collection("users").document(userId)

    private suspend fun safe(what: String, block: suspend () -> Unit) {
        try { block() } catch (e: Exception) { Log.w("Stride", "sync $what failed; will retry on next change", e) }
    }

    override suspend fun pushHabit(h: Habit): Unit = safe("habit") {
        user().collection("habits").document(h.id.toString()).set(
            mapOf("name" to h.name, "emoji" to h.emoji, "color" to h.color, "days" to h.days,
                "reminderMinutes" to h.reminderMinutes, "createdDay" to h.createdDay, "updatedAt" to h.updatedAt),
            SetOptions.merge(),
        ).await()
    }

    override suspend fun deleteHabit(id: Long): Unit = safe("archive") {
        user().collection("habits").document(id.toString()).update("archived", true).await()
    }

    override suspend fun pushLog(log: HabitLog, done: Boolean): Unit = safe("log") {
        val doc = user().collection("logs").document("${log.habitId}_${log.day}")
        if (done) doc.set(mapOf("habitId" to log.habitId, "day" to log.day, "doneAt" to log.doneAt)).await() else doc.delete().await()
    }

    override suspend fun pushWorkout(w: Workout): Unit = safe("workout") {
        user().collection("workouts").document(w.id.toString()).set(
            mapOf("type" to w.type.name, "day" to w.day, "minutes" to w.minutes, "effort" to w.effort, "note" to w.note),
        ).await()
    }
}
