package com.dayline.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.dayline.DaylineApp
import com.dayline.MainActivity
import com.dayline.domain.model.Habit
import com.dayline.domain.model.HabitSchedule
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class HabitFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createsEditsAndCompletesHabitsAndShowsRetainedBestStreak() {
        val today = LocalDate.now()
        val bestStreakHabit = Habit(
            id = "habit-flow-best-streak",
            title = "Best streak fixture",
            schedule = HabitSchedule.Daily,
            createdAtLocalDate = today.minusDays(3),
            currentStreak = 2,
            bestStreak = 7
        )
        runBlocking {
            (composeRule.activity.application as DaylineApp)
                .container
                .habitRepository
                .saveHabit(bestStreakHabit)
                .also {
                    (composeRule.activity.application as DaylineApp)
                        .container
                        .habitRepository
                        .refreshStreaks(today)
                }
        }

        val dailyTitle = "Stretch ${System.currentTimeMillis()}"
        composeRule.onNodeWithTag("nav-habits").performClick()
        composeRule.onNodeWithTag("add-habit").performClick()
        composeRule.onNodeWithTag("habit-title").performTextInput(dailyTitle)
        composeRule.onNodeWithTag("save-habit").performScrollTo().performClick()
        composeRule.onNodeWithText(dailyTitle).performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Complete habit: $dailyTitle")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Done today").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Edit habit: $dailyTitle")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("schedule-selected-days").performClick()
        composeRule.onNodeWithTag("habit-day-SATURDAY").performClick()
        composeRule.onNodeWithTag("habit-day-SUNDAY").performClick()
        composeRule.onNodeWithTag("save-habit").performScrollTo().performClick()
        composeRule.onNodeWithText("Weekends").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("add-habit").performClick()
        composeRule.onNodeWithTag("habit-title").performTextInput("Journal")
        composeRule.onNodeWithTag("schedule-every-x-days").performClick()
        composeRule.onNodeWithTag("habit-interval")
            .performScrollTo()
            .performTextClearance()
        composeRule.onNodeWithTag("habit-interval").performTextInput("3")
        composeRule.onNodeWithTag("save-habit").performScrollTo().performClick()
        composeRule.onNodeWithText("Every 3 days").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("nav-today").performClick()
        composeRule.onNodeWithText("Hard Deadlines").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Daily Habits").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("nav-profile").performClick()
        composeRule.onNodeWithTag("best-streak-value").assertIsDisplayed()
        composeRule.onNodeWithText("7 days").assertIsDisplayed()
    }
}
