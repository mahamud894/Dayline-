package com.dayline.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.dayline.DaylineApp
import com.dayline.MainActivity
import com.dayline.domain.model.Habit
import com.dayline.domain.model.HabitSchedule
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority
import com.dayline.domain.model.TaskStatus
import com.dayline.ui.navigation.DaylineNavHost
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccessibilityFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun keyLabelsAndCompletionActionsRemainReachableAtLargeFontScale() {
        val app = composeRule.activity.application as DaylineApp
        val today = LocalDate.now()
        val task = Task(
            id = "accessibility-task-${System.currentTimeMillis()}",
            title = "Accessible deadline",
            notes = null,
            dueAtEpochMillis = today.atTime(23, 59)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            priority = TaskPriority.HIGH,
            status = TaskStatus.ACTIVE,
            quietModeOverrideAllowed = false,
            completedAtEpochMillis = null
        )
        val habit = Habit(
            id = "accessibility-habit-${System.currentTimeMillis()}",
            title = "Accessible routine",
            schedule = HabitSchedule.Daily,
            createdAtLocalDate = today,
            currentStreak = 0,
            bestStreak = 0
        )
        runBlocking {
            app.container.taskRepository.saveTask(task)
            app.container.habitRepository.saveHabit(habit)
        }

        val displayDensity = composeRule.activity.resources.displayMetrics.density
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(displayDensity, fontScale = 1.5f)
            ) {
                DaylineNavHost(app.container)
            }
        }

        composeRule.onNodeWithContentDescription("Add task").assertIsDisplayed()
        val progressDescription = composeRule.onNodeWithTag("today-progress")
            .fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription]
        assertTrue(progressDescription.any { it.startsWith("Progress:") })
        composeRule.onAllNodesWithText("DUE TODAY").fetchSemanticsNodes().also {
            assertTrue("Deadline status should be text-labelled", it.isNotEmpty())
        }
        composeRule.onAllNodesWithText("Daily").fetchSemanticsNodes().also {
            assertTrue("Habit schedule should be text-labelled", it.isNotEmpty())
        }
        composeRule.onNodeWithContentDescription("Complete habit: ${habit.title}")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag("nav-tasks").performClick()
        composeRule.onAllNodesWithText("HIGH").fetchSemanticsNodes().also {
            assertTrue("Priority should be text-labelled", it.isNotEmpty())
        }
        composeRule.onNodeWithContentDescription("Complete task: ${task.title}")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
