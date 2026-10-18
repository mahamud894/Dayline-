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
import com.dayline.MainActivity
import org.junit.Rule
import org.junit.Test

class TaskFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun taskCanBeCreatedEditedRankedAndCompleted() {
        composeRule.onNodeWithTag("add-task-fab").performClick()
        composeRule.onNodeWithTag("task-title").performScrollTo().performTextInput("Client submission")
        // Calendar opens on today, then the clock on the next full hour.
        composeRule.onNodeWithTag("task-due-at").performScrollTo().performClick()
        composeRule.onNodeWithTag("deadline-date-confirm").performClick()
        composeRule.onNodeWithTag("deadline-time-confirm").performClick()
        composeRule.onNodeWithTag("task-due-at-clear").assertIsDisplayed()
        composeRule.onNodeWithTag("priority-high").performScrollTo().performClick()
        composeRule.onNodeWithTag("save-task").performScrollTo().performClick()

        composeRule.onNodeWithTag("nav-tasks").performClick()
        composeRule.onNodeWithText("Client submission").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Edit task: Client submission").performClick()
        composeRule.onNodeWithTag("task-title").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("task-title")
            .performScrollTo()
            .performTextInput("Review client delivery")
        composeRule.onNodeWithTag("save-task").performScrollTo().performClick()

        composeRule.onNodeWithTag("nav-today").performClick()
        composeRule.onNodeWithText("Review client delivery").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("HIGH PRIORITY").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("nav-tasks").performClick()
        composeRule.onNodeWithContentDescription("Complete task: Review client delivery")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Completed").assertIsDisplayed()
        composeRule.onNodeWithText("Review client delivery").assertIsDisplayed()
    }
}
