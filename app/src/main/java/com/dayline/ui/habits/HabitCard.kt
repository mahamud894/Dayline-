package com.dayline.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayline.R
import androidx.compose.ui.res.stringResource
import com.dayline.domain.model.Habit

@Composable
internal fun HabitCard(
    habit: Habit,
    completedToday: Boolean,
    onComplete: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    val completedDescription = stringResource(R.string.cd_completed_habit, habit.title)
    val completeDescription = stringResource(R.string.cd_complete_habit, habit.title)
    val editDescription = stringResource(R.string.cd_edit_habit, habit.title)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF142220))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(habit.title, color = Color(0xFFF4F5FF), fontWeight = FontWeight.SemiBold)
                    Text(
                        text = habit.schedule.displayName(),
                        color = Color(0xFFA7DCD2),
                        fontSize = 12.sp
                    )
                }
                if (completedToday) {
                    Text(
                        text = stringResource(R.string.habit_done_today),
                        modifier = Modifier.semantics {
                            contentDescription = completedDescription
                        },
                        color = Color(0xFF59F5D8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    TextButton(
                        onClick = onComplete,
                        modifier = Modifier.semantics {
                            contentDescription = completeDescription
                        }
                    ) {
                        Text(stringResource(R.string.habit_mark_done), color = Color(0xFF59F5D8))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.today_streak, habit.currentStreak),
                    color = Color(0xFFC2C7D3),
                    fontSize = 12.sp
                )
                if (onEdit != null) {
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.semantics {
                            contentDescription = editDescription
                        }
                    ) {
                        Text(stringResource(R.string.action_edit), color = Color(0xFFC7A2FF))
                    }
                }
            }
        }
    }
}

@Composable
internal fun com.dayline.domain.model.HabitSchedule.displayName(): String =
    when (this) {
        com.dayline.domain.model.HabitSchedule.Daily -> stringResource(R.string.schedule_daily)
        is com.dayline.domain.model.HabitSchedule.Weekdays -> when {
            days == setOf(
                java.time.DayOfWeek.SATURDAY,
                java.time.DayOfWeek.SUNDAY
            ) -> stringResource(R.string.schedule_weekends)
            days == setOf(
                java.time.DayOfWeek.MONDAY,
                java.time.DayOfWeek.TUESDAY,
                java.time.DayOfWeek.WEDNESDAY,
                java.time.DayOfWeek.THURSDAY,
                java.time.DayOfWeek.FRIDAY
            ) -> stringResource(R.string.schedule_weekdays)
            else -> days.sortedBy(java.time.DayOfWeek::getValue)
                .joinToString { it.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()) }
        }
        is com.dayline.domain.model.HabitSchedule.EveryDays ->
            if (interval == 1) stringResource(R.string.schedule_every_day) else stringResource(R.string.schedule_every_n_days, interval)
    }
