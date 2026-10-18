package com.dayline.ui.tasks

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeadlinePickerTest {
    @Test
    fun pickerMillisRoundTripToTheSameCalendarDate() {
        val date = LocalDate.of(2026, 10, 19)

        val millis = dateToPickerMillis(date)

        assertEquals(
            LocalDate.of(2026, 10, 19).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            millis
        )
        assertEquals(date, pickerMillisToDate(millis))
    }

    @Test
    fun pickerMillisAreReadAsUtcSoLateEveningDoesNotShiftTheDate() {
        val lateUtc = LocalDate.of(2026, 10, 19)
            .atTime(23, 59)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

        assertEquals(LocalDate.of(2026, 10, 19), pickerMillisToDate(lateUtc))
    }

    @Test
    fun onlyDatesFromTheEarliestAllowedDateAreSelectable() {
        val today = LocalDate.of(2026, 10, 18)

        assertFalse(isDeadlineDateSelectable(dateToPickerMillis(today.minusDays(1)), today))
        assertTrue(isDeadlineDateSelectable(dateToPickerMillis(today), today))
        assertTrue(isDeadlineDateSelectable(dateToPickerMillis(today.plusDays(30)), today))
    }

    @Test
    fun earliestSelectableDateKeepsAnExistingPastDeadlinePickable() {
        val today = LocalDate.of(2026, 10, 18)
        val pastDeadline = LocalDateTime.of(2026, 10, 10, 9, 0)

        assertEquals(today, earliestSelectableDate(today, existing = null))
        assertEquals(LocalDate.of(2026, 10, 10), earliestSelectableDate(today, pastDeadline))
        assertEquals(today, earliestSelectableDate(today, LocalDateTime.of(2026, 11, 1, 9, 0)))
    }

    @Test
    fun defaultTimeIsTheNextFullHourWithoutWrappingPastMidnight() {
        assertEquals(
            LocalTime.of(11, 0),
            defaultDeadlineTime(LocalDateTime.of(2026, 10, 18, 10, 47))
        )
        assertEquals(
            LocalTime.of(23, 59),
            defaultDeadlineTime(LocalDateTime.of(2026, 10, 18, 23, 5))
        )
    }

    @Test
    fun saveFormatMatchesWhatTheViewModelParses() {
        assertEquals(
            "2026-10-19 15:00",
            formatDeadlineForSave(LocalDateTime.of(2026, 10, 19, 15, 0))
        )
    }

    @Test
    fun labelFollowsTwelveOrTwentyFourHourClock() {
        val deadline = LocalDateTime.of(2026, 10, 19, 15, 0)

        assertEquals(
            "Mon, Oct 19 · 3:00 PM",
            formatDeadlineLabel(deadline, is24Hour = false, locale = Locale.US)
        )
        assertEquals(
            "Mon, Oct 19 · 15:00",
            formatDeadlineLabel(deadline, is24Hour = true, locale = Locale.US)
        )
    }
}
