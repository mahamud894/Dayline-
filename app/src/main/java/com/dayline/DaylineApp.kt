package com.dayline

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DaylineApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationFactory.createChannels()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.reminderScheduler.restorePending()
            val settings = container.userSettingsStore.settings.first()
            container.morningSummaryScheduler.schedule(settings)
        }
    }
}
