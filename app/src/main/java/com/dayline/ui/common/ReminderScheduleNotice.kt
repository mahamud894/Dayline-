package com.dayline.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.dayline.R
import com.dayline.domain.reminders.ReminderScheduleStatus

@Composable
fun ReminderScheduleNotice(status: ReminderScheduleStatus?) {
    val message = when (status) {
        ReminderScheduleStatus.WORK_MANAGER_FALLBACK ->
            stringResource(R.string.notice_exact_alarm)
        ReminderScheduleStatus.NOTIFICATION_PERMISSION_REQUIRED ->
            stringResource(R.string.notice_notifications)
        else -> null
    }
    if (message != null) {
        Text(
            text = message,
            color = Color(0xFFFFC27A),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("reminder-schedule-notice")
        )
    }
}
