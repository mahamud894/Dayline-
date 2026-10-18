package com.dayline.platform.calendar

import android.content.ContentResolver
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CalendarCommitment(
    val id: String,
    val title: String,
    val startAtEpochMillis: Long,
    val endAtEpochMillis: Long?
)

class CalendarAccessUnavailableException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

class AndroidCalendarReader(
    private val contentResolver: ContentResolver,
    private val hasReadCalendarPermission: () -> Boolean
) {
    suspend fun readBetween(
        startEpochMillis: Long,
        endEpochMillis: Long
    ): List<CalendarCommitment> = withContext(Dispatchers.IO) {
        require(startEpochMillis <= endEpochMillis) {
            "The calendar range start must not be after its end."
        }
        if (!hasReadCalendarPermission()) {
            throw CalendarAccessUnavailableException(PERMISSION_UNAVAILABLE)
        }

        val queryUri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(startEpochMillis.toString())
            .appendPath(endEpochMillis.toString())
            .build()
        val cursor = try {
            contentResolver.query(queryUri, PROJECTION, null, null, null)
        } catch (exception: SecurityException) {
            throw CalendarAccessUnavailableException(PERMISSION_UNAVAILABLE, exception)
        } ?: throw CalendarAccessUnavailableException(PROVIDER_UNAVAILABLE)

        cursor.use {
            val eventIdColumn = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleColumn = it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginColumn = it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endColumn = it.getColumnIndexOrThrow(CalendarContract.Instances.END)
            buildList {
                while (it.moveToNext()) {
                    val eventId = it.getLong(eventIdColumn)
                    val start = it.getLong(beginColumn)
                    add(
                        CalendarCommitment(
                            id = "$eventId:$start",
                            title = it.getString(titleColumn)?.takeIf(String::isNotBlank)
                                ?: UNTITLED_EVENT,
                            startAtEpochMillis = start,
                            endAtEpochMillis = if (it.isNull(endColumn)) {
                                null
                            } else {
                                it.getLong(endColumn)
                            }
                        )
                    )
                }
            }.distinctBy(CalendarCommitment::id)
                .sortedWith(compareBy(CalendarCommitment::startAtEpochMillis, CalendarCommitment::id))
        }
    }

    internal companion object {
        const val PERMISSION_UNAVAILABLE =
            "Calendar access is unavailable. Allow calendar access in Settings to import events."
        const val PROVIDER_UNAVAILABLE = "Calendar data is currently unavailable."
        const val UNTITLED_EVENT = "Untitled event"
        val PROJECTION = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END
        )
    }
}
