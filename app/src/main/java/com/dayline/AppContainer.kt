package com.dayline

import android.app.Application
import androidx.room.Room
import com.dayline.data.HabitRepository
import com.dayline.data.RoomHabitRepository
import com.dayline.data.RoomTaskRepository
import com.dayline.data.TaskRepository
import com.dayline.data.local.DaylineDatabase
import com.dayline.data.local.UserSettingsStore
import com.dayline.platform.notifications.DaylineNotificationFactory
import com.dayline.platform.reminders.AndroidReminderScheduler
import com.dayline.platform.reminders.ReminderActionHandler
import com.dayline.platform.reminders.ReminderDeliveryHandler
import java.time.Clock
import kotlinx.coroutines.flow.first

class AppContainer(application: Application) {
    private val database = Room.databaseBuilder(
        application,
        DaylineDatabase::class.java,
        "dayline.db"
    ).build()

    val taskRepository: TaskRepository = RoomTaskRepository(database)
    val habitRepository: HabitRepository = RoomHabitRepository(database)
    val userSettingsStore = UserSettingsStore(application)
    val notificationFactory = DaylineNotificationFactory(application)
    val reminderScheduler = AndroidReminderScheduler(
        context = application,
        repository = taskRepository,
        notifications = notificationFactory
    )
    val reminderActionHandler = ReminderActionHandler(
        repository = taskRepository,
        scheduler = reminderScheduler,
        notifications = notificationFactory,
        clock = Clock.systemDefaultZone(),
        snoozeMinutes = { userSettingsStore.settings.first().snoozeMinutes }
    )
    val reminderDeliveryHandler = ReminderDeliveryHandler(
        repository = taskRepository,
        scheduler = reminderScheduler,
        notifications = notificationFactory
    )
}
