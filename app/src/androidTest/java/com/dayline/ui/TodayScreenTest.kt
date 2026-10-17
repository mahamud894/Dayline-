package com.dayline.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.dayline.MainActivity
import org.junit.Rule
import org.junit.Test

class TodayScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun todayShowsDistinctCommitmentSectionsProgressAndQuickAdd() {
        composeRule.onNodeWithTag("today-progress").assertIsDisplayed()
        composeRule.onNodeWithText("Next commitment").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Hard Deadlines").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Daily Habits").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("add-task-fab").assertIsDisplayed()
    }
}
