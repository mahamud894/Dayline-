package com.dayline.ui.tasks

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dayline.R
import androidx.compose.ui.res.stringResource
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Deadline field that opens a calendar, then a clock. The value is a local date-time;
 * null means no deadline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlineField(
    deadline: LocalDateTime?,
    onDeadlineChange: (LocalDateTime?) -> Unit,
    modifier: Modifier = Modifier
) {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val datePattern = stringResource(R.string.pattern_deadline_date)
    val pickLabel = stringResource(R.string.deadline_pick_label)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var pickedDate by rememberSaveable { mutableStateOf<LocalDate?>(null) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = deadline?.let { formatDeadlineLabel(it, is24Hour, Locale.getDefault(), datePattern) }
                .orEmpty(),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text(stringResource(R.string.deadline_field)) },
            placeholder = { Text(stringResource(R.string.deadline_placeholder)) },
            trailingIcon = deadline?.let {
                {
                    TextButton(
                        onClick = { onDeadlineChange(null) },
                        modifier = Modifier.testTag("task-due-at-clear")
                    ) {
                        Text(stringResource(R.string.action_clear), color = Color(0xFFC7A2FF))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )
        // Covers the field (but not the Clear button) so a tap anywhere opens the calendar.
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = if (deadline != null) 88.dp else 0.dp)
                .clickable(
                    onClickLabel = pickLabel,
                    role = Role.Button
                ) { showDatePicker = true }
                .testTag("task-due-at")
        )
    }

    if (showDatePicker) {
        val today = LocalDate.now()
        val earliest = earliestSelectableDate(today, deadline)
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = dateToPickerMillis(deadline?.toLocalDate() ?: today),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    isDeadlineDateSelectable(utcTimeMillis, earliest)

                override fun isSelectableYear(year: Int): Boolean = year >= earliest.year
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickedDate = dateState.selectedDateMillis?.let(::pickerMillisToDate)
                        showDatePicker = false
                    },
                    enabled = dateState.selectedDateMillis != null,
                    modifier = Modifier.testTag("deadline-date-confirm")
                ) { Text(stringResource(R.string.action_next)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    pickedDate?.let { date ->
        val initialTime = deadline?.toLocalTime() ?: defaultDeadlineTime(LocalDateTime.now())
        val timeState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = is24Hour
        )
        AlertDialog(
            onDismissRequest = { pickedDate = null },
            title = { Text(stringResource(R.string.deadline_time_title)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeadlineChange(date.atTime(timeState.hour, timeState.minute))
                        pickedDate = null
                    },
                    modifier = Modifier.testTag("deadline-time-confirm")
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickedDate = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

// The date picker works in UTC midnight millis, independent of the device time zone.
internal fun dateToPickerMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun pickerMillisToDate(utcMillis: Long): LocalDate =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

internal fun isDeadlineDateSelectable(utcMillis: Long, earliest: LocalDate): Boolean =
    !pickerMillisToDate(utcMillis).isBefore(earliest)

/** Past dates are blocked, except an existing deadline's own date so it can still be shown. */
internal fun earliestSelectableDate(today: LocalDate, existing: LocalDateTime?): LocalDate {
    val existingDate = existing?.toLocalDate() ?: return today
    return if (existingDate.isBefore(today)) existingDate else today
}

/** Next full hour; late at night, stays on the same day instead of wrapping to midnight. */
internal fun defaultDeadlineTime(now: LocalDateTime): LocalTime {
    val nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
    return if (nextHour.toLocalDate() == now.toLocalDate()) {
        nextHour.toLocalTime()
    } else {
        LocalTime.of(23, 59)
    }
}

internal fun formatDeadlineForSave(deadline: LocalDateTime): String =
    deadline.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

internal fun formatDeadlineLabel(
    deadline: LocalDateTime,
    is24Hour: Boolean,
    locale: Locale,
    datePattern: String = "EEE, MMM d"
): String {
    val time = if (is24Hour) "HH:mm" else "h:mm a"
    return deadline.format(DateTimeFormatter.ofPattern("$datePattern · $time", locale))
}
