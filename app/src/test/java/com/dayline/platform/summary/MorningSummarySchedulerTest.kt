package com.dayline.platform.summary

import androidx.room.Room
import com.dayline.domain.model.UserSettings
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.TimeZone
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MorningSummarySchedulerTest {
    @Test
    fun deniedExactAlarmSchedulesConfiguredLocalTimeThroughFallback() = runTest {
        val zone = ZoneId.of("America/Los_Angeles")
        val now = LocalDate.of(2026, 10, 18).atTime(7, 30).atZone(zone).toInstant()
        val work = RecordingSummaryFallbackWorkEnqueuer()
        val scheduler = MorningSummaryScheduler(
            context = RuntimeEnvironment.getApplication(),
            clock = Clock.fixed(now, zone),
            zoneId = zone,
            exactAlarmAccess = { false },
            notificationAccess = { true },
            workEnqueuer = work
        )

        val status = scheduler.schedule(
            UserSettings(morningSummaryTime = LocalTime.of(9, 0))
        )

        assertEquals(MorningSummaryScheduleStatus.WORK_MANAGER_FALLBACK, status)
        assertEquals(
            LocalDate.of(2026, 10, 18).atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
            work.triggerAtEpochMillis
        )
        assertEquals(1, work.enqueueCount)
    }

    @Test
    fun configuredTimeAlreadyPassedSchedulesForNextLocalDay() = runTest {
        val zone = ZoneId.of("America/Los_Angeles")
        val now = LocalDate.of(2026, 10, 18).atTime(9, 1).atZone(zone).toInstant()
        val work = RecordingSummaryFallbackWorkEnqueuer()
        val scheduler = MorningSummaryScheduler(
            context = RuntimeEnvironment.getApplication(),
            clock = Clock.fixed(now, zone),
            zoneId = zone,
            exactAlarmAccess = { false },
            notificationAccess = { true },
            workEnqueuer = work
        )

        scheduler.schedule(UserSettings(morningSummaryTime = LocalTime.of(9, 0)))

        assertEquals(
            LocalDate.of(2026, 10, 19).atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
            work.triggerAtEpochMillis
        )
    }

    @Test
    fun springForwardGapResolvesConfiguredSummaryToNextValidLocalTime() = runTest {
        val zone = ZoneId.of("America/Los_Angeles")
        val now = LocalDate.of(2026, 3, 8).atTime(1, 0).atZone(zone).toInstant()
        val work = RecordingSummaryFallbackWorkEnqueuer()
        val scheduler = MorningSummaryScheduler(
            context = RuntimeEnvironment.getApplication(),
            clock = Clock.fixed(now, zone),
            zoneId = zone,
            exactAlarmAccess = { false },
            notificationAccess = { true },
            workEnqueuer = work
        )

        scheduler.schedule(
            UserSettings(morningSummaryTime = LocalTime.of(2, 30))
        )

        assertEquals(
            LocalDate.of(2026, 3, 8).atTime(3, 30).atZone(zone).toInstant().toEpochMilli(),
            work.triggerAtEpochMillis
        )
    }

    @Test
    fun schedulingUsesSystemZoneAfterTimezoneChanges() = runTest {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val now = Instant.parse("2026-10-18T08:30:00Z")
            val work = RecordingSummaryFallbackWorkEnqueuer()
            val scheduler = MorningSummaryScheduler(
                context = RuntimeEnvironment.getApplication(),
                clock = Clock.fixed(now, ZoneOffset.UTC),
                exactAlarmAccess = { false },
                notificationAccess = { true },
                workEnqueuer = work
            )

            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            scheduler.schedule(
                UserSettings(morningSummaryTime = LocalTime.of(9, 0))
            )

            assertEquals(
                Instant.parse("2026-10-18T16:00:00Z").toEpochMilli(),
                work.triggerAtEpochMillis
            )
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    @Test
    fun disabledSummaryCancelsFallbackWithoutSchedulingAnother() = runTest {
        val work = RecordingSummaryFallbackWorkEnqueuer()
        val scheduler = MorningSummaryScheduler(
            context = RuntimeEnvironment.getApplication(),
            clock = Clock.fixed(Instant.parse("2026-10-18T07:30:00Z"), ZoneId.of("UTC")),
            zoneId = ZoneId.of("UTC"),
            exactAlarmAccess = { false },
            notificationAccess = { true },
            workEnqueuer = work
        )

        val status = scheduler.schedule(
            UserSettings(morningSummaryEnabled = false)
        )

        assertEquals(MorningSummaryScheduleStatus.DISABLED, status)
        assertEquals(1, work.cancelCount)
        assertEquals(0, work.enqueueCount)
        assertTrue(work.triggerAtEpochMillis == null)
    }

    private class RecordingSummaryFallbackWorkEnqueuer : SummaryFallbackWorkEnqueuer {
        var triggerAtEpochMillis: Long? = null
        var enqueueCount = 0
        var cancelCount = 0

        override fun enqueue(triggerAtEpochMillis: Long) {
            this.triggerAtEpochMillis = triggerAtEpochMillis
            enqueueCount++
        }

        override fun cancel() {
            cancelCount++
        }
    }
}
