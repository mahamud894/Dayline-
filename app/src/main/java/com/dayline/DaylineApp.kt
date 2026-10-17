package com.dayline

import android.app.Application

class DaylineApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationFactory.createChannels()
    }
}
