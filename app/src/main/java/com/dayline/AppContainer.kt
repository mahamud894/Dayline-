package com.dayline

import android.app.Application
import androidx.room.Room
import com.dayline.data.HabitRepository
import com.dayline.data.RoomHabitRepository
import com.dayline.data.RoomTaskRepository
import com.dayline.data.TaskRepository
import com.dayline.data.local.DaylineDatabase
import com.dayline.data.local.UserSettingsStore

class AppContainer(application: Application) {
    private val database = Room.databaseBuilder(
        application,
        DaylineDatabase::class.java,
        "dayline.db"
    ).build()

    val taskRepository: TaskRepository = RoomTaskRepository(database)
    val habitRepository: HabitRepository = RoomHabitRepository(database)
    val userSettingsStore = UserSettingsStore(application)
}
