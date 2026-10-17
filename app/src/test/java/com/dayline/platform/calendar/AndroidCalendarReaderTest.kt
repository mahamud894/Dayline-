package com.dayline.platform.calendar

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import android.content.pm.ProviderInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AndroidCalendarReaderTest {
    @Test
    fun readsRequestedInstancesSortedByStartTime() = runBlocking {
        val provider = RecordingCalendarProvider(
            listOf(
                calendarRow(id = 2L, title = "Later", start = 200L, end = 250L),
                calendarRow(id = 1L, title = "Earlier", start = 100L, end = 150L)
            )
        )
        val reader = reader(provider) { true }

        val result = reader.readBetween(50L, 300L)

        assertEquals(listOf("Earlier", "Later"), result.map { it.title })
        assertEquals(listOf(100L, 200L), result.map { it.startAtEpochMillis })
        assertEquals(listOf("50", "300"), provider.lastQueryUri!!.pathSegments.takeLast(2))
        assertEquals(
            setOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END
            ),
            provider.lastProjection!!.toSet()
        )
    }

    @Test
    fun missingOrRevokedPermissionIsReportedAsUnavailable() {
        runBlocking {
            var permissionGranted = false
            val provider = RecordingCalendarProvider(emptyList())
            val reader = reader(provider) { permissionGranted }

            assertThrows(CalendarAccessUnavailableException::class.java) {
                runBlocking { reader.readBetween(0L, 1L) }
            }
            assertEquals(0, provider.queryCount)

            permissionGranted = true
            reader.readBetween(0L, 1L)
            permissionGranted = false

            assertThrows(CalendarAccessUnavailableException::class.java) {
                runBlocking { reader.readBetween(0L, 1L) }
            }
        }
    }

    @Test
    fun repeatedReadsAndDuplicateInstancesDoNotDuplicateCommitments() = runBlocking {
        val provider = RecordingCalendarProvider(
            listOf(
                calendarRow(id = 7L, title = "Review", start = 100L, end = 150L),
                calendarRow(id = 7L, title = "Review duplicate", start = 100L, end = 150L)
            )
        )
        val reader = reader(provider) { true }

        val firstRead = reader.readBetween(0L, 200L)
        val secondRead = reader.readBetween(0L, 200L)

        assertEquals(1, firstRead.size)
        assertEquals(firstRead, secondRead)
    }

    @Test
    fun neverWritesToCalendarProvider() = runBlocking {
        val provider = RecordingCalendarProvider(
            listOf(calendarRow(id = 3L, title = "Planning", start = 100L, end = 150L))
        )

        reader(provider) { true }.readBetween(0L, 200L)

        assertTrue(provider.insertCount == 0)
        assertTrue(provider.updateCount == 0)
        assertTrue(provider.deleteCount == 0)
    }

    private fun reader(
        provider: RecordingCalendarProvider,
        hasPermission: () -> Boolean
    ): AndroidCalendarReader {
        provider.attachInfo(
            RuntimeEnvironment.getApplication(),
            ProviderInfo().apply { authority = CalendarContract.AUTHORITY }
        )
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        ShadowContentResolver.registerProviderInternal(CalendarContract.AUTHORITY, provider)
        return AndroidCalendarReader(resolver, hasPermission)
    }

    private fun calendarRow(
        id: Long,
        title: String,
        start: Long,
        end: Long
    ): Map<String, Any> = mapOf(
        CalendarContract.Instances.EVENT_ID to id,
        CalendarContract.Instances.TITLE to title,
        CalendarContract.Instances.BEGIN to start,
        CalendarContract.Instances.END to end
    )

    private class RecordingCalendarProvider(
        private val rows: List<Map<String, Any>>
    ) : ContentProvider() {
        var lastQueryUri: Uri? = null
        var lastProjection: Array<out String>? = null
        var queryCount = 0
        var insertCount = 0
        var updateCount = 0
        var deleteCount = 0

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            queryCount++
            lastQueryUri = uri
            lastProjection = projection
            val columns = requireNotNull(projection)
            return MatrixCursor(columns).apply {
                rows.forEach { row -> addRow(columns.map { row[it] }) }
            }
        }

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? {
            insertCount++
            return null
        }

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
            deleteCount++
            return 0
        }

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
        ): Int {
            updateCount++
            return 0
        }
    }
}
