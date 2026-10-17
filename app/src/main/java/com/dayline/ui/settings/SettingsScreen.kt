package com.dayline.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayline.domain.model.UserSettings
import com.dayline.platform.summary.MorningSummaryScheduleStatus

@Composable
fun SettingsScreen(
    settings: UserSettings,
    calendarPermissionGranted: Boolean,
    scheduleStatus: MorningSummaryScheduleStatus?,
    error: String?,
    onSummaryEnabledChange: (Boolean) -> Unit,
    onSaveSummaryTime: (String) -> Unit,
    onCalendarImportChange: (Boolean) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenDndSettings: () -> Unit,
    onOpenCalendarPermissionSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var summaryTime by remember(settings.morningSummaryTime) {
        mutableStateOf(settings.morningSummaryTime.toString())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "TUNE YOUR DAY",
            color = Color(0xFFB7FF4A),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Settings",
            color = Color(0xFFF4F5FF),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Button(onClick = onBack, modifier = Modifier.testTag("settings-back")) {
            Text("Back to profile")
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF171D2A))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Morning summary",
                            color = Color(0xFFF4F5FF),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "One daily glance at your top commitments.",
                            color = Color(0xFFC2C7D3),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = settings.morningSummaryEnabled,
                        onCheckedChange = onSummaryEnabledChange,
                        modifier = Modifier.testTag("summary-enabled")
                    )
                }
                OutlinedTextField(
                    value = summaryTime,
                    onValueChange = {
                        summaryTime = it
                        onClearError()
                    },
                    enabled = settings.morningSummaryEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("summary-time"),
                    label = { Text("Local time (24-hour)") },
                    supportingText = { Text("Example: 08:00") },
                    singleLine = true
                )
                Button(
                    onClick = { onSaveSummaryTime(summaryTime) },
                    enabled = settings.morningSummaryEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save-summary-time")
                ) {
                    Text("Save summary time")
                }
                Text(
                    text = when (scheduleStatus) {
                        MorningSummaryScheduleStatus.EXACT -> "Exact summary alarm scheduled."
                        MorningSummaryScheduleStatus.WORK_MANAGER_FALLBACK ->
                            "Android may delay summaries. Allow exact alarms for on-time delivery."
                        MorningSummaryScheduleStatus.DISABLED -> "Daily summary is turned off."
                        MorningSummaryScheduleStatus.NOTIFICATION_PERMISSION_REQUIRED ->
                            "Allow notifications to receive your daily summary."
                        null -> "Smart Wake starts one hour before this time; the notification is the fallback."
                    },
                    color = Color(0xFFC2C7D3),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("summary-schedule-status")
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF171D2A))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Optional calendar import",
                            color = Color(0xFFF4F5FF),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Read-only. Manual tasks remain available without calendar access.",
                            color = Color(0xFFC2C7D3),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = settings.calendarImportEnabled,
                        onCheckedChange = onCalendarImportChange,
                        modifier = Modifier.testTag("calendar-import-enabled")
                    )
                }
                if (settings.calendarImportEnabled && !calendarPermissionGranted) {
                    Text(
                        "Calendar access is unavailable. Grant permission to show events; your manual tasks are unchanged.",
                        color = Color(0xFFFFC27A),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("calendar-permission-status")
                    )
                    Button(
                        onClick = onOpenCalendarPermissionSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open-calendar-permission-settings")
                    ) {
                        Text("Manage calendar access")
                    }
                }
            }
        }
        error?.let {
            Text(
                text = it,
                color = Color(0xFFFF8D9A),
                modifier = Modifier.testTag("settings-error")
            )
        }
        Button(
            onClick = onOpenNotificationSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Notification settings")
        }
        Button(
            onClick = onOpenExactAlarmSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage exact alarm access")
        }
        Button(
            onClick = onOpenDndSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Do Not Disturb access")
        }
        Text(
            text = "Android controls notification visibility, exact-alarm access, and Do Not Disturb overrides. Dayline uses a delayed WorkManager fallback when exact alarms are unavailable.",
            color = Color(0xFFC2C7D3),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}
