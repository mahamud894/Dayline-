package com.dayline.platform.summary

import android.content.Context
import com.dayline.domain.model.UserSettings
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface SummaryDeliveryGate {
    suspend fun deliverOnce(date: LocalDate, delivery: suspend () -> Unit): Boolean
}

fun interface DailySummaryDelivery {
    suspend fun deliver(date: LocalDate)
}

class SharedPreferencesSummaryDeliveryGate(context: Context) : SummaryDeliveryGate {
    private val preferences =
        context.applicationContext.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)

    override suspend fun deliverOnce(date: LocalDate, delivery: suspend () -> Unit): Boolean =
        deliveryMutex.withLock {
            val deliveredDates =
                preferences.getStringSet(DELIVERED_DATES, emptySet()).orEmpty().toSet()
            if (date.toString() in deliveredDates) return@withLock false
            delivery()
            val updatedDates = deliveredDates + date.toString()
            check(
                preferences.edit().putStringSet(DELIVERED_DATES, updatedDates).commit()
            ) {
                "Unable to persist the morning summary delivery marker for $date."
            }
            true
        }

    private companion object {
        const val STORE_NAME = "dayline_summary_delivery"
        const val DELIVERED_DATES = "delivered_dates"
        val deliveryMutex = Mutex()
    }
}

class SmartWakeCoordinator(
    private val settings: suspend () -> UserSettings,
    private val gate: SummaryDeliveryGate,
    private val delivery: DailySummaryDelivery,
    private val cancelFallback: suspend () -> Unit,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = clock.zone
) {
    suspend fun onUnlock(
        now: ZonedDateTime = ZonedDateTime.now(clock).withZoneSameInstant(zoneId)
    ): Boolean {
        val userSettings = settings()
        if (!userSettings.morningSummaryEnabled) return false

        val localNow = now.withZoneSameInstant(zoneId).toLocalDateTime()
        val summaryToday = localNow.toLocalDate().atTime(userSettings.morningSummaryTime)
        val summaryDateTime = if (localNow.isAfter(summaryToday)) {
            summaryToday.plusDays(1)
        } else {
            summaryToday
        }
        val cutoff = summaryDateTime.minusHours(1)
        if (localNow.isBefore(cutoff) || localNow.isAfter(summaryDateTime)) return false
        return deliverOnce(summaryDateTime.toLocalDate())
    }

    suspend fun onFallback(
        now: ZonedDateTime = ZonedDateTime.now(clock).withZoneSameInstant(zoneId)
    ): Boolean {
        val userSettings = settings()
        if (!userSettings.morningSummaryEnabled) return false
        val localNow = now.withZoneSameInstant(zoneId).toLocalDateTime()
        val summaryDateTime = localNow.toLocalDate().atTime(userSettings.morningSummaryTime)
        if (localNow.isBefore(summaryDateTime)) return false
        return deliverOnce(summaryDateTime.toLocalDate())
    }

    private suspend fun deliverOnce(date: LocalDate): Boolean {
        val delivered = gate.deliverOnce(date) { delivery.deliver(date) }
        if (delivered) cancelFallback()
        return delivered
    }
}
