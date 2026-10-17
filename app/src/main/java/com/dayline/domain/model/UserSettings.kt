package com.dayline.domain.model

import java.time.LocalTime

data class UserSettings(
    val morningSummaryTime: LocalTime = LocalTime.of(8, 0),
    val morningSummaryEnabled: Boolean = true,
    val snoozeMinutes: Int = 10,
    val calendarImportEnabled: Boolean = false
)
