package com.dayline.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.dayline.DaylineApp
import com.dayline.MainActivity
import com.dayline.domain.model.UserSettings
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun savesSummaryTimeAndEnablePreference() {
        val app = composeRule.activity.application as DaylineApp
        runBlocking { app.container.userSettingsStore.save(UserSettings()) }

        composeRule.onNodeWithTag("nav-profile").performClick()
        composeRule.onNodeWithTag("open-settings").performClick()
        composeRule.onNodeWithText("Morning summary").assertIsDisplayed()
        composeRule.onNodeWithTag("summary-schedule-status").assertIsDisplayed()
        composeRule.onNodeWithTag("summary-time").performTextClearance()
        composeRule.onNodeWithTag("summary-time").performTextInput("09:15")
        composeRule.onNodeWithTag("save-summary-time").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                app.container.userSettingsStore.settings.first().morningSummaryTime ==
                    LocalTime.of(9, 15)
            }
        }
        composeRule.onNodeWithTag("summary-time").assertTextEquals("09:15")

        composeRule.onNodeWithTag("summary-enabled").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                !app.container.userSettingsStore.settings.first().morningSummaryEnabled
            }
        }
        assertEquals(
            LocalTime.of(9, 15),
            runBlocking { app.container.userSettingsStore.settings.first().morningSummaryTime }
        )
    }

    @Test
    fun calendarImportIsOptionalAndDoesNotReplaceManualTasks() {
        val app = composeRule.activity.application as DaylineApp
        runBlocking { app.container.userSettingsStore.save(UserSettings()) }

        composeRule.onNodeWithTag("nav-profile").performClick()
        composeRule.onNodeWithTag("open-settings").performClick()

        composeRule.onNodeWithTag("calendar-import-enabled").assertIsOff()
        composeRule.onNodeWithText("Optional calendar import").assertIsDisplayed()
        composeRule.onNodeWithText("Read-only. Manual tasks remain available without calendar access.")
            .assertIsDisplayed()
    }
}
