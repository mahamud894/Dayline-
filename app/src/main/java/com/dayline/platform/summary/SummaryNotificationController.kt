package com.dayline.platform.summary

import com.dayline.domain.summary.SummaryItem
import java.time.LocalDate

interface SummaryNotificationController {
    fun showSummary(date: LocalDate, items: List<SummaryItem>)
    fun clearSummary()
}
