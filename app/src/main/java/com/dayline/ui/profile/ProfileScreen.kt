package com.dayline.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
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
fun ProfileScreen(
    habits: List<Habit>,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bestStreak = habits.maxOfOrNull(Habit::bestStreak) ?: 0
    val currentStreak = habits.sumOf(Habit::currentStreak)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.profile_eyebrow),
                    color = Color(0xFFB7FF4A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.profile_title),
                    color = Color(0xFFF4F5FF),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF142220))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_best_streak),
                        color = Color(0xFF59F5D8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.profile_days, bestStreak),
                        modifier = Modifier.testTag("best-streak-value"),
                        color = Color(0xFFF4F5FF),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.profile_best_hint),
                        color = Color(0xFFC2C7D3),
                        fontSize = 13.sp
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171D2A))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.profile_current_rhythm), color = Color(0xFFC7A2FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = stringResource(R.string.profile_active_days, currentStreak),
                        color = Color(0xFFF4F5FF),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.profile_habit_count, habits.size),
                        color = Color(0xFFC2C7D3),
                        fontSize = 13.sp
                    )
                }
            }
        }
        item {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open-settings")
            ) {
                Text(stringResource(R.string.settings_title))
            }
        }
    }
}
