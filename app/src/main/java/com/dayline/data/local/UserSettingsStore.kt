package com.dayline.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dayline.domain.model.UserSettings
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dayline_settings"
)

class UserSettingsStore(private val context: Context) {
    val settings: Flow<UserSettings> = context.userSettingsDataStore.data.map { preferences ->
        UserSettings(
            morningSummaryTime = LocalTime.parse(
                preferences[SUMMARY_TIME] ?: UserSettings().morningSummaryTime.toString()
            ),
            morningSummaryEnabled = preferences[SUMMARY_ENABLED] ?: true,
            snoozeMinutes = preferences[SNOOZE_MINUTES] ?: UserSettings().snoozeMinutes,
            calendarImportEnabled = preferences[CALENDAR_IMPORT_ENABLED] ?: false
        )
    }

    suspend fun save(settings: UserSettings) {
        context.userSettingsDataStore.edit { preferences ->
            preferences[SUMMARY_TIME] = settings.morningSummaryTime.toString()
            preferences[SUMMARY_ENABLED] = settings.morningSummaryEnabled
            preferences[SNOOZE_MINUTES] = settings.snoozeMinutes
            preferences[CALENDAR_IMPORT_ENABLED] = settings.calendarImportEnabled
        }
    }

    private companion object {
        val SUMMARY_TIME = stringPreferencesKey("summary_time")
        val SUMMARY_ENABLED = booleanPreferencesKey("summary_enabled")
        val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
        val CALENDAR_IMPORT_ENABLED = booleanPreferencesKey("calendar_import_enabled")
    }
}
