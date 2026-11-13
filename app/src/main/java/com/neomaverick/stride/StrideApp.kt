package com.neomaverick.stride

import android.app.Application
import android.provider.Settings
import com.neomaverick.stride.billing.Premium
import com.neomaverick.stride.data.Repository
import com.neomaverick.stride.data.StrideDb
import com.neomaverick.stride.health.HealthSteps
import com.neomaverick.stride.reminders.Reminders
import com.neomaverick.stride.sync.CloudSync

/** Small manual dependency container: the app is small enough that a DI framework would only add noise. */
class StrideApp : Application() {
    lateinit var repo: Repository; private set
    lateinit var premium: Premium; private set
    lateinit var health: HealthSteps; private set

    override fun onCreate() {
        super.onCreate()
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "local"
        repo = Repository(StrideDb.get(this), CloudSync.create(this, id))
        premium = Premium(this)
        health = HealthSteps(this)
        Reminders.createChannel(this)
    }
}
