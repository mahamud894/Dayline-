package com.dayline.platform.summary

import com.dayline.data.HabitRepository
import com.dayline.data.TaskRepository
import com.dayline.domain.summary.rankDailySummary
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first

class RepositoryDailySummaryDelivery(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val notifications: SummaryNotificationController,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = clock.zone
) : DailySummaryDelivery {
    override suspend fun deliver(date: LocalDate) {
        val now = ZonedDateTime.now(clock).withZoneSameInstant(zoneId)
        val items = rankDailySummary(
            tasks = taskRepository.observeTasks().first(),
            habits = habitRepository.observeHabits().first(),
            now = now,
            zoneId = zoneId,
            summaryDate = date
        )
        notifications.showSummary(date, items)
    }
}
