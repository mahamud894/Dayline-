package com.dayline.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayline.R
import androidx.compose.ui.res.stringResource
import com.dayline.domain.model.Habit

@Composable
fun HabitsScreen(
    habits: List<Habit>,
    completedHabitIds: Set<String>,
    onAddHabit: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onCompleteHabit: (Habit) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.habits_eyebrow),
                color = Color(0xFF59F5D8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.habits_title),
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
                color = Color(0xFFF4F5FF),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddHabit,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add-habit"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF17332F),
                    contentColor = Color(0xFF59F5D8)
                )
            ) {
                Text(stringResource(R.string.habits_add), fontWeight = FontWeight.SemiBold)
            }
        }
        if (habits.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.habits_empty),
                    modifier = Modifier.padding(vertical = 20.dp),
                    color = Color(0xFFC2C7D3)
                )
            }
        } else {
            items(habits, key = Habit::id) { habit ->
                HabitCard(
                    habit = habit,
                    completedToday = habit.id in completedHabitIds,
                    onComplete = { onCompleteHabit(habit) },
                    onEdit = { onEditHabit(habit) }
                )
            }
        }
    }
}
