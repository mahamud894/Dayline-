package com.dayline.ui.habits

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayline.domain.model.Habit
import com.dayline.domain.model.HabitSchedule
import com.dayline.ui.common.DeleteWithConfirmation
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitEditorScreen(
    habit: Habit?,
    saveError: String?,
    today: LocalDate,
    onBack: () -> Unit,
    onClearError: () -> Unit,
    onSave: (title: String, schedule: HabitSchedule) -> Unit,
    onDelete: () -> Unit = {}
) {
    var title by rememberSaveable(habit?.id) { mutableStateOf(habit?.title.orEmpty()) }
    var scheduleType by rememberSaveable(habit?.id) {
        mutableStateOf(
            when (habit?.schedule) {
                is HabitSchedule.Weekdays -> ScheduleType.SELECTED_DAYS
                is HabitSchedule.EveryDays -> ScheduleType.EVERY_X_DAYS
                else -> ScheduleType.DAILY
            }
        )
    }
    var selectedDays by rememberSaveable(habit?.id) {
        mutableStateOf((habit?.schedule as? HabitSchedule.Weekdays)?.days.orEmpty())
    }
    var interval by rememberSaveable(habit?.id) {
        mutableStateOf(
            (habit?.schedule as? HabitSchedule.EveryDays)?.interval?.toString() ?: "2"
        )
    }
    var localError by rememberSaveable(habit?.id) { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("Back", color = Color(0xFFC7A2FF))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "FLEXIBLE BY DESIGN",
                color = Color(0xFF59F5D8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (habit == null) "New habit" else "Edit habit",
                color = Color(0xFFF4F5FF),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                localError = null
                onClearError()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("habit-title"),
            label = { Text("What do you want to repeat?") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "RHYTHM",
                color = Color(0xFFC2C7D3),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = scheduleType == ScheduleType.DAILY,
                    onClick = {
                        scheduleType = ScheduleType.DAILY
                        localError = null
                    },
                    label = { Text("Daily") },
                    modifier = Modifier.testTag("schedule-daily")
                )
                FilterChip(
                    selected = scheduleType == ScheduleType.SELECTED_DAYS,
                    onClick = {
                        scheduleType = ScheduleType.SELECTED_DAYS
                        localError = null
                    },
                    label = { Text("Selected days") },
                    modifier = Modifier.testTag("schedule-selected-days")
                )
            }
            FilterChip(
                selected = scheduleType == ScheduleType.EVERY_X_DAYS,
                onClick = {
                    scheduleType = ScheduleType.EVERY_X_DAYS
                    localError = null
                },
                label = { Text("Every X days") },
                modifier = Modifier.testTag("schedule-every-x-days")
            )
        }
        when (scheduleType) {
            ScheduleType.DAILY -> Text(
                "A small win, every day.",
                color = Color(0xFFA7DCD2),
                fontSize = 13.sp
            )
            ScheduleType.SELECTED_DAYS -> {
                Text(
                    "Choose the days that fit your week.",
                    color = Color(0xFFA7DCD2),
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DayOfWeek.values().forEach { day ->
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = {
                                selectedDays = if (day in selectedDays) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                                localError = null
                            },
                            label = {
                                Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1))
                            },
                            modifier = Modifier.testTag("habit-day-${day.name}")
                        )
                    }
                }
            }
            ScheduleType.EVERY_X_DAYS -> {
                OutlinedTextField(
                    value = interval,
                    onValueChange = {
                        interval = it.filter(Char::isDigit).take(3)
                        localError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit-interval"),
                    label = { Text("Repeat every") },
                    suffix = { Text("days") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
                Text(
                    text = "Starting ${today.format(DateTimeFormatter.ofPattern("MMM d"))}",
                    color = Color(0xFFA7DCD2),
                    fontSize = 13.sp
                )
            }
        }
        (localError ?: saveError)?.let {
            Text(text = it, color = Color(0xFFFF8296), fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                val anchorDate =
                    (habit?.schedule as? HabitSchedule.EveryDays)?.anchorDate ?: today
                val schedule = when (scheduleType) {
                    ScheduleType.DAILY -> HabitSchedule.Daily
                    ScheduleType.SELECTED_DAYS -> selectedDays
                        .takeIf { it.isNotEmpty() }
                        ?.let { HabitSchedule.Weekdays(it) }
                    ScheduleType.EVERY_X_DAYS -> interval
                        .toIntOrNull()
                        ?.takeIf { it >= 1 }
                        ?.let { HabitSchedule.EveryDays(it, anchorDate) }
                }
                if (schedule == null) {
                    localError = if (scheduleType == ScheduleType.SELECTED_DAYS) {
                        "Choose at least one day."
                    } else {
                        "Enter an interval of at least one day."
                    }
                } else {
                    onSave(title, schedule)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save-habit"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF59F5D8),
                contentColor = Color(0xFF0C1C19)
            )
        ) {
            Text("Save habit", fontWeight = FontWeight.Bold)
        }
        if (habit != null) {
            DeleteWithConfirmation(
                label = "Delete habit",
                dialogTitle = "Delete this habit?",
                dialogText = "\"${habit.title}\" and its streak history will be removed.",
                onConfirm = onDelete
            )
        }
    }
}

private enum class ScheduleType {
    DAILY,
    SELECTED_DAYS,
    EVERY_X_DAYS
}
