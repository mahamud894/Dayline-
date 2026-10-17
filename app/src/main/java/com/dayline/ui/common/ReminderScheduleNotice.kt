package com.dayline.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.dayline.domain.reminders.ReminderScheduleStatus

@Composable
fun ReminderScheduleNotice(status: ReminderScheduleStatus?) {
    val message = when (status) {
        ReminderScheduleStatus.WORK_MANAGER_FALLBACK ->
            "Android may delay deadline reminders. Allow exact alarms for on-time delivery."
        ReminderScheduleStatus.NOTIFICATION_PERMISSION_REQUIRED ->
            "Allow notifications to receive deadline reminders."
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
