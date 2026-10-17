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
import com.dayline.domain.model.Habit

@Composable
internal fun HabitCard(
    habit: Habit,
    completedToday: Boolean,
    onComplete: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
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
                        text = "Done today",
                        modifier = Modifier.semantics {
                            contentDescription = "Completed today: ${habit.title}"
                        },
                        color = Color(0xFF59F5D8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    TextButton(
                        onClick = onComplete,
                        modifier = Modifier.semantics {
                            contentDescription = "Complete habit: ${habit.title}"
                        }
                    ) {
                        Text("Mark done", color = Color(0xFF59F5D8))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${habit.currentStreak} day streak",
                    color = Color(0xFFC2C7D3),
                    fontSize = 12.sp
                )
                if (onEdit != null) {
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.semantics {
                            contentDescription = "Edit habit: ${habit.title}"
                        }
                    ) {
                        Text("Edit", color = Color(0xFFC7A2FF))
                    }
                }
            }
        }
    }
}

internal fun com.dayline.domain.model.HabitSchedule.displayName(): String =
    when (this) {
        com.dayline.domain.model.HabitSchedule.Daily -> "Daily"
        is com.dayline.domain.model.HabitSchedule.Weekdays -> when {
            days == setOf(
                java.time.DayOfWeek.SATURDAY,
                java.time.DayOfWeek.SUNDAY
            ) -> "Weekends"
            days == setOf(
                java.time.DayOfWeek.MONDAY,
                java.time.DayOfWeek.TUESDAY,
                java.time.DayOfWeek.WEDNESDAY,
                java.time.DayOfWeek.THURSDAY,
                java.time.DayOfWeek.FRIDAY
            ) -> "Weekdays"
            else -> days.sortedBy(java.time.DayOfWeek::getValue)
                .joinToString { it.name.lowercase().replaceFirstChar(Char::uppercase) }
        }
        is com.dayline.domain.model.HabitSchedule.EveryDays ->
            if (interval == 1) "Every day" else "Every $interval days"
    }
