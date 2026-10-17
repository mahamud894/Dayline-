package com.dayline.data

import com.dayline.data.local.DaylineDatabase
import com.dayline.data.local.TaskEntity
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun saveTask(task: Task)
    suspend fun setTaskStatus(id: String, status: TaskStatus, atEpochMillis: Long)
    suspend fun getTask(id: String): Task?
}

class RoomTaskRepository(private val database: DaylineDatabase) : TaskRepository {
    override fun observeTasks(): Flow<List<Task>> =
        database.taskDao().observeAll().map { tasks -> tasks.map(TaskEntity::toDomain) }

    override suspend fun saveTask(task: Task) {
        database.taskDao().upsert(TaskEntity.fromDomain(task))
    }

    override suspend fun setTaskStatus(id: String, status: TaskStatus, atEpochMillis: Long) {
        val completedAt = atEpochMillis.takeIf { status == TaskStatus.COMPLETED }
        check(database.taskDao().updateStatus(id, status.name, completedAt) == 1) {
            "Task not found: $id"
        }
    }

    override suspend fun getTask(id: String): Task? =
        database.taskDao().getById(id)?.toDomain()
}
