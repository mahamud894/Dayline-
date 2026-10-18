package com.dayline.data.backup

import androidx.room.withTransaction
import com.dayline.data.local.DaylineDatabase
import com.dayline.data.local.HabitCompletionEntity
import com.dayline.data.local.HabitEntity
import com.dayline.data.local.TaskEntity
import com.dayline.domain.model.UserSettings
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class BackupFormatException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * Exports everything the user owns (tasks, habits, habit history, settings) as one JSON
 * document, and restores such a document by replacing all current data.
 */
class DaylineBackup(
    private val database: DaylineDatabase,
    private val readSettings: suspend () -> UserSettings,
    private val writeSettings: suspend (UserSettings) -> Unit,
    private val clock: Clock = Clock.systemDefaultZone(),
    /** Runs before data is replaced, with the ids of the tasks about to disappear. */
    private val beforeRestore: suspend (oldTaskIds: List<String>) -> Unit = {},
    /** Runs after data is replaced, to rebuild reminders and schedules. */
    private val afterRestore: suspend (UserSettings) -> Unit = {}
) {
    suspend fun export(): String {
        val json = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", Instant.now(clock).toString())
            .put("tasks", JSONArray(database.taskDao().getAll().map(::taskToJson)))
            .put("habits", JSONArray(database.habitDao().getAll().map(::habitToJson)))
            .put(
                "habitCompletions",
                JSONArray(database.habitCompletionDao().getAll().map(::completionToJson))
            )
            .put("settings", settingsToJson(readSettings()))
        return json.toString(2)
    }

    /** Validates the whole file first; on any problem nothing is changed. */
    suspend fun restore(json: String) {
        val backup = decode(json)
        beforeRestore(database.taskDao().getAll().map(TaskEntity::id))
        database.withTransaction {
            database.habitCompletionDao().deleteAll()
            database.habitDao().deleteAll()
            database.taskDao().deleteAll()
            database.taskDao().insertAll(backup.tasks)
            database.habitDao().insertAll(backup.habits)
            database.habitCompletionDao().insertAll(backup.completions)
        }
        writeSettings(backup.settings)
        afterRestore(backup.settings)
    }

    private class Decoded(
        val tasks: List<TaskEntity>,
        val habits: List<HabitEntity>,
        val completions: List<HabitCompletionEntity>,
        val settings: UserSettings
    )

    private fun decode(json: String): Decoded {
        val root = try {
            JSONObject(json)
        } catch (exception: JSONException) {
            throw BackupFormatException("This file isn't a Dayline backup.", exception)
        }
        if (root.optString("format") != FORMAT) {
            throw BackupFormatException("This file isn't a Dayline backup.")
        }
        val version = root.optInt("version", -1)
        if (version !in 1..VERSION) {
            throw BackupFormatException(
                "This backup was made by a newer version of Dayline. Update the app first."
            )
        }
        return try {
            val tasks = root.getJSONArray("tasks").objects().map(::taskFromJson)
            val habits = root.getJSONArray("habits").objects().map(::habitFromJson)
            val completions =
                root.getJSONArray("habitCompletions").objects().map(::completionFromJson)
            // Round-trip through the domain models so bad enums or dates fail here.
            tasks.forEach(TaskEntity::toDomain)
            habits.forEach(HabitEntity::toDomain)
            val habitIds = habits.map(HabitEntity::id).toSet()
            completions.forEach { completion ->
                LocalDate.parse(completion.localDate)
                if (completion.habitId !in habitIds) {
                    throw BackupFormatException("The backup has history for a missing habit.")
                }
            }
            Decoded(tasks, habits, completions, settingsFromJson(root.getJSONObject("settings")))
        } catch (exception: BackupFormatException) {
            throw exception
        } catch (exception: Exception) {
            throw BackupFormatException("This backup file is damaged or incomplete.", exception)
        }
    }

    private fun taskToJson(task: TaskEntity) = JSONObject()
        .put("id", task.id)
        .put("title", task.title)
        .put("notes", task.notes ?: JSONObject.NULL)
        .put("dueAtEpochMillis", task.dueAtEpochMillis ?: JSONObject.NULL)
        .put("priority", task.priority)
        .put("status", task.status)
        .put("quietModeOverrideAllowed", task.quietModeOverrideAllowed)
        .put("completedAtEpochMillis", task.completedAtEpochMillis ?: JSONObject.NULL)

    private fun taskFromJson(json: JSONObject) = TaskEntity(
        id = json.getString("id"),
        title = json.getString("title"),
        notes = json.nullableString("notes"),
        dueAtEpochMillis = json.nullableLong("dueAtEpochMillis"),
        priority = json.getString("priority"),
        status = json.getString("status"),
        quietModeOverrideAllowed = json.getBoolean("quietModeOverrideAllowed"),
        completedAtEpochMillis = json.nullableLong("completedAtEpochMillis")
    )

    private fun habitToJson(habit: HabitEntity) = JSONObject()
        .put("id", habit.id)
        .put("title", habit.title)
        .put("scheduleType", habit.scheduleType)
        .put("selectedDays", habit.selectedDays ?: JSONObject.NULL)
        .put("intervalDays", habit.intervalDays ?: JSONObject.NULL)
        .put("anchorDate", habit.anchorDate ?: JSONObject.NULL)
        .put("createdAtLocalDate", habit.createdAtLocalDate)
        .put("currentStreak", habit.currentStreak)
        .put("bestStreak", habit.bestStreak)

    private fun habitFromJson(json: JSONObject) = HabitEntity(
        id = json.getString("id"),
        title = json.getString("title"),
        scheduleType = json.getString("scheduleType"),
        selectedDays = json.nullableString("selectedDays"),
        intervalDays = json.nullableLong("intervalDays")?.toInt(),
        anchorDate = json.nullableString("anchorDate"),
        createdAtLocalDate = json.getString("createdAtLocalDate"),
        currentStreak = json.getInt("currentStreak"),
        bestStreak = json.getInt("bestStreak")
    )

    private fun completionToJson(completion: HabitCompletionEntity) = JSONObject()
        .put("habitId", completion.habitId)
        .put("localDate", completion.localDate)

    private fun completionFromJson(json: JSONObject) = HabitCompletionEntity(
        habitId = json.getString("habitId"),
        localDate = json.getString("localDate")
    )

    private fun settingsToJson(settings: UserSettings) = JSONObject()
        .put("morningSummaryTime", settings.morningSummaryTime.toString())
        .put("morningSummaryEnabled", settings.morningSummaryEnabled)
        .put("snoozeMinutes", settings.snoozeMinutes)
        .put("calendarImportEnabled", settings.calendarImportEnabled)

    private fun settingsFromJson(json: JSONObject) = UserSettings(
        morningSummaryTime = LocalTime.parse(json.getString("morningSummaryTime")),
        morningSummaryEnabled = json.getBoolean("morningSummaryEnabled"),
        snoozeMinutes = json.getInt("snoozeMinutes").also {
            if (it <= 0) throw BackupFormatException("The backup has an invalid snooze time.")
        },
        calendarImportEnabled = json.getBoolean("calendarImportEnabled")
    )

    private fun JSONArray.objects(): List<JSONObject> = List(length(), ::getJSONObject)

    private fun JSONObject.nullableString(key: String): String? =
        if (isNull(key)) null else getString(key)

    private fun JSONObject.nullableLong(key: String): Long? =
        if (isNull(key)) null else getLong(key)

    companion object {
        const val FORMAT = "dayline-backup"
        const val VERSION = 1
    }
}
