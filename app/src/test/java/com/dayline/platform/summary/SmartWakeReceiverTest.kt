package com.dayline.platform.summary

import com.dayline.domain.model.UserSettings
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartWakeReceiverTest {
    @Test
    fun configuredNineOClockSummaryUsesEightOClockUnlockCutoff() = runTest {
        val fixture = Fixture(LocalTime.of(9, 0))

        assertFalse(fixture.coordinator.onUnlock(at("2026-10-18T07:59:59")))
        assertTrue(fixture.coordinator.onUnlock(at("2026-10-18T08:00:00")))

        assertEquals(listOf(LocalDate.of(2026, 10, 18)), fixture.deliveredDates)
        assertEquals(1, fixture.fallbackCancellations)
    }

    @Test
    fun fallbackAtConfiguredTimeDeliversWhenNoUnlockOccurred() = runTest {
        val fixture = Fixture(LocalTime.of(9, 0))

        assertFalse(fixture.coordinator.onFallback(at("2026-10-18T08:59:59")))
        assertTrue(fixture.coordinator.onFallback(at("2026-10-18T09:00:00")))
        assertFalse(fixture.coordinator.onUnlock(at("2026-10-18T09:00:00")))

        assertEquals(listOf(LocalDate.of(2026, 10, 18)), fixture.deliveredDates)
    }

    @Test
    fun simultaneousUnlockAndFallbackDeliverOnlyOnce() = runTest {
        val fixture = Fixture(LocalTime.of(9, 0))
        val now = at("2026-10-18T09:00:00")

        val unlock = async { fixture.coordinator.onUnlock(now) }
        val fallback = async { fixture.coordinator.onFallback(now) }

        assertEquals(1, listOf(unlock.await(), fallback.await()).count { it })
        assertEquals(listOf(LocalDate.of(2026, 10, 18)), fixture.deliveredDates)
        assertEquals(1, fixture.fallbackCancellations)
    }

    @Test
    fun changingSummaryTimeChangesUnlockCutoffImmediately() = runTest {
        val fixture = Fixture(LocalTime.of(9, 0))
        fixture.settings = fixture.settings.copy(morningSummaryTime = LocalTime.of(10, 0))

        assertFalse(fixture.coordinator.onUnlock(at("2026-10-18T08:59:59")))
        assertTrue(fixture.coordinator.onUnlock(at("2026-10-18T09:00:00")))
        assertEquals(listOf(LocalDate.of(2026, 10, 18)), fixture.deliveredDates)
    }

    @Test
    fun anotherLocalDateCanDeliverItsOwnSummary() = runTest {
        val fixture = Fixture(LocalTime.of(9, 0))

        assertTrue(fixture.coordinator.onFallback(at("2026-10-18T09:00:00")))
        assertTrue(fixture.coordinator.onFallback(at("2026-10-19T09:00:00")))

        assertEquals(
            listOf(LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 19)),
            fixture.deliveredDates
        )
    }

    @Test
    fun midnightCrossingUnlockWindowTargetsFollowingLocalDate() = runTest {
        val fixture = Fixture(LocalTime.of(0, 30))

        assertTrue(fixture.coordinator.onUnlock(at("2026-10-18T23:30:00")))

        assertEquals(listOf(LocalDate.of(2026, 10, 19)), fixture.deliveredDates)
    }

    @Test
    fun disabledSummaryDoesNotDeliver() = runTest {
        val fixture = Fixture(LocalTime.of(9, 0))
        fixture.settings = fixture.settings.copy(morningSummaryEnabled = false)

        assertFalse(fixture.coordinator.onFallback(at("2026-10-18T09:00:00")))
        assertTrue(fixture.deliveredDates.isEmpty())
    }

    private class Fixture(summaryTime: LocalTime) {
        var settings = UserSettings(morningSummaryTime = summaryTime)
        val deliveredDates = mutableListOf<LocalDate>()
        var fallbackCancellations = 0
        private val gate = InMemorySummaryDeliveryGate()
        val coordinator = SmartWakeCoordinator(
            settings = { settings },
            gate = gate,
            delivery = { date -> deliveredDates += date },
            cancelFallback = { fallbackCancellations++ },
            clock = Clock.fixed(Instant.parse("2026-10-18T00:00:00Z"), ZoneId.of("UTC")),
            zoneId = ZoneId.of("UTC")
        )
    }

    private class InMemorySummaryDeliveryGate : SummaryDeliveryGate {
        private val dates = mutableSetOf<LocalDate>()
        private val mutex = Mutex()

        override suspend fun deliverOnce(date: LocalDate, delivery: suspend () -> Unit): Boolean =
            mutex.withLock {
                if (date in dates) return@withLock false
                delivery()
                dates += date
                true
            }
    }

    private fun at(dateTime: String): ZonedDateTime =
        LocalDateTime.parse(dateTime).atZone(ZoneId.of("UTC"))
}
