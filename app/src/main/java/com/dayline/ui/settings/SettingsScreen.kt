package com.dayline.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.dayline.R
import androidx.compose.ui.res.stringResource
import com.dayline.domain.model.UserSettings
import com.dayline.ui.common.UiText
import com.dayline.platform.summary.MorningSummaryScheduleStatus

@Composable
fun SettingsScreen(
    settings: UserSettings,
    calendarPermissionGranted: Boolean,
    scheduleStatus: MorningSummaryScheduleStatus?,
    error: UiText?,
    onSummaryEnabledChange: (Boolean) -> Unit,
    onSaveSummaryTime: (String) -> Unit,
    onCalendarImportChange: (Boolean) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenDndSettings: () -> Unit,
    onOpenCalendarPermissionSettings: () -> Unit,
    modifier: Modifier = Modifier,
    backupMessage: UiText? = null,
    onExportBackup: () -> Unit = {},
    onRestoreBackup: () -> Unit = {},
    alarmFullScreenAllowed: Boolean = true,
    isXiaomiDevice: Boolean = false,
    onOpenFullScreenAlarmSettings: () -> Unit = {},
    onOpenXiaomiPermissions: () -> Unit = {},
    onOpenXiaomiAutostart: () -> Unit = {}
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
            text = stringResource(R.string.settings_eyebrow),
            color = Color(0xFFB7FF4A),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.settings_title),
            color = Color(0xFFF4F5FF),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Button(onClick = onBack, modifier = Modifier.testTag("settings-back")) {
            Text(stringResource(R.string.settings_back))
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
                            stringResource(R.string.settings_summary),
                            color = Color(0xFFF4F5FF),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.settings_summary_detail),
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
                    label = { Text(stringResource(R.string.settings_summary_time)) },
                    supportingText = { Text(stringResource(R.string.settings_summary_time_example)) },
                    singleLine = true
                )
                Button(
                    onClick = { onSaveSummaryTime(summaryTime) },
                    enabled = settings.morningSummaryEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save-summary-time")
                ) {
                    Text(stringResource(R.string.settings_summary_save))
                }
                Text(
                    text = when (scheduleStatus) {
                        MorningSummaryScheduleStatus.EXACT -> stringResource(R.string.summary_status_exact)
                        MorningSummaryScheduleStatus.WORK_MANAGER_FALLBACK ->
                            stringResource(R.string.summary_status_fallback)
                        MorningSummaryScheduleStatus.DISABLED -> stringResource(R.string.summary_status_disabled)
                        MorningSummaryScheduleStatus.NOTIFICATION_PERMISSION_REQUIRED ->
                            stringResource(R.string.summary_status_notifications)
                        null -> stringResource(R.string.summary_status_default)
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
                            stringResource(R.string.settings_calendar),
                            color = Color(0xFFF4F5FF),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.settings_calendar_detail),
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
                        stringResource(R.string.settings_calendar_unavailable),
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
                        Text(stringResource(R.string.settings_calendar_manage))
                    }
                }
            }
        }
        AlarmReliabilityCard(
            fullScreenAllowed = alarmFullScreenAllowed,
            isXiaomi = isXiaomiDevice,
            onOpenFullScreenSettings = onOpenFullScreenAlarmSettings,
            onOpenXiaomiPermissions = onOpenXiaomiPermissions,
            onOpenXiaomiAutostart = onOpenXiaomiAutostart
        )
        BackupCard(
            message = backupMessage,
            onExport = onExportBackup,
            onRestore = onRestoreBackup
        )
        error?.let {
            Text(
                text = it.resolve(),
                color = Color(0xFFFF8D9A),
                modifier = Modifier.testTag("settings-error")
            )
        }
        Button(
            onClick = onOpenNotificationSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_notifications))
        }
        Button(
            onClick = onOpenExactAlarmSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_exact_alarms))
        }
        Button(
            onClick = onOpenDndSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.settings_dnd))
        }
        Text(
            text = stringResource(R.string.settings_system_note),
            color = Color(0xFFC2C7D3),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun BackupCard(
    message: UiText?,
    onExport: () -> Unit,
    onRestore: () -> Unit
) {
    var confirmingRestore by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171D2A))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.backup_title), color = Color(0xFFF4F5FF), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.backup_detail),
                color = Color(0xFFC2C7D3),
                style = MaterialTheme.typography.bodySmall
            )
            Button(
                onClick = onExport,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export-backup")
            ) {
                Text(stringResource(R.string.backup_export))
            }
            OutlinedButton(
                onClick = { confirmingRestore = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("restore-backup")
            ) {
                Text(stringResource(R.string.backup_restore), color = Color(0xFFC7A2FF))
            }
            message?.let {
                Text(
                    text = it.resolve(),
                    color = Color(0xFFB7FF4A),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("backup-message")
                )
            }
        }
    }
    if (confirmingRestore) {
        AlertDialog(
            onDismissRequest = { confirmingRestore = false },
            title = { Text(stringResource(R.string.backup_restore_title)) },
            text = {
                Text(
                    stringResource(R.string.backup_restore_text)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingRestore = false
                        onRestore()
                    },
                    modifier = Modifier.testTag("restore-confirm")
                ) { Text(stringResource(R.string.backup_choose_file)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingRestore = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun AlarmReliabilityCard(
    fullScreenAllowed: Boolean,
    isXiaomi: Boolean,
    onOpenFullScreenSettings: () -> Unit,
    onOpenXiaomiPermissions: () -> Unit,
    onOpenXiaomiAutostart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171D2A))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.alarm_reliability_title), color = Color(0xFFF4F5FF), fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.alarm_reliability_detail),
                color = Color(0xFFC2C7D3),
                style = MaterialTheme.typography.bodySmall
            )
            if (!fullScreenAllowed) {
                Text(
                    stringResource(R.string.alarm_full_screen_off),
                    color = Color(0xFFFFC27A),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("full-screen-alarm-status")
                )
                Button(
                    onClick = onOpenFullScreenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open-full-screen-alarm-settings")
                ) {
                    Text(stringResource(R.string.alarm_full_screen_allow))
                }
            }
            if (isXiaomi) {
                Text(
                    stringResource(R.string.alarm_xiaomi_tip),
                    color = Color(0xFFFFC27A),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("xiaomi-alarm-tip")
                )
                Button(onClick = onOpenXiaomiPermissions, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.alarm_xiaomi_permissions))
                }
                OutlinedButton(onClick = onOpenXiaomiAutostart, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.alarm_xiaomi_autostart), color = Color(0xFFC7A2FF))
                }
            }
        }
    }
}
