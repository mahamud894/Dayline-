package com.dayline.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority
import com.dayline.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String?,
    val dueAtEpochMillis: Long?,
    val priority: String,
    val status: String,
    val quietModeOverrideAllowed: Boolean,
    val completedAtEpochMillis: Long?
) {
    fun toDomain() = Task(
        id = id,
        title = title,
        notes = notes,
        dueAtEpochMillis = dueAtEpochMillis,
        priority = TaskPriority.valueOf(priority),
        status = TaskStatus.valueOf(status),
        quietModeOverrideAllowed = quietModeOverrideAllowed,
        completedAtEpochMillis = completedAtEpochMillis
    )

    companion object {
        fun fromDomain(task: Task) = TaskEntity(
            id = task.id,
            title = task.title,
            notes = task.notes,
            dueAtEpochMillis = task.dueAtEpochMillis,
            priority = task.priority.name,
            status = task.status.name,
            quietModeOverrideAllowed = task.quietModeOverrideAllowed,
            completedAtEpochMillis = task.completedAtEpochMillis
        )
    }
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueAtEpochMillis IS NULL, dueAtEpochMillis, id")
    fun observeAll(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query("UPDATE tasks SET status = :status, completedAtEpochMillis = :completedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, completedAt: Long?): Int

    @Query(
        "UPDATE tasks SET status = 'COMPLETED', completedAtEpochMillis = :completedAt " +
            "WHERE id = :id AND status IN ('ACTIVE', 'OVERDUE')"
    )
    suspend fun completeIfIncomplete(id: String, completedAt: Long): Int

    @Query(
        "UPDATE tasks SET status = 'OVERDUE' " +
            "WHERE id = :id AND status = 'ACTIVE' AND dueAtEpochMillis <= :nowEpochMillis"
    )
    suspend fun markOverdueIfActive(id: String, nowEpochMillis: Long): Int

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("SELECT * FROM tasks ORDER BY id")
    suspend fun getAll(): List<TaskEntity>

    @Insert
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
