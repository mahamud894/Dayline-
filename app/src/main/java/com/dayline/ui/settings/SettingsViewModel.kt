package com.dayline.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dayline.data.local.UserSettingsStore
import com.dayline.domain.model.UserSettings
import com.dayline.platform.notifications.DaylineNotificationFactory
import com.dayline.platform.summary.MorningSummaryScheduleStatus
import com.dayline.platform.summary.MorningSummaryScheduler
import java.time.LocalTime
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SettingsViewModel(
    private val settingsStore: UserSettingsStore,
    private val summaryScheduler: MorningSummaryScheduler,
    private val notifications: DaylineNotificationFactory
) : ViewModel() {
    val settings: StateFlow<UserSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings()
    )

    private val mutableScheduleStatus = MutableStateFlow<MorningSummaryScheduleStatus?>(null)
    val scheduleStatus: StateFlow<MorningSummaryScheduleStatus?> =
        mutableScheduleStatus.asStateFlow()

    private val mutableError = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = mutableError.asStateFlow()

    private val updateMutex = Mutex()

    fun setSummaryEnabled(enabled: Boolean) {
        updateSettings { it.copy(morningSummaryEnabled = enabled) }
    }

    fun setCalendarImportEnabled(enabled: Boolean) {
        mutableError.value = null
        viewModelScope.launch {
            updateMutex.withLock {
                try {
                    val updated = settingsStore.settings.first()
                        .copy(calendarImportEnabled = enabled)
                    settingsStore.save(updated)
                } catch (exception: Exception) {
                    mutableError.value =
                        "Unable to update calendar settings: ${exception.message ?: "unknown error"}"
                }
            }
        }
    }

    fun reportCalendarPermissionDenied() {
        mutableError.value =
            "Calendar access wasn't granted. Manual tasks remain available without calendar access."
    }

    fun saveSummaryTime(value: String) {
        val time = try {
            LocalTime.parse(value.trim())
        } catch (_: DateTimeParseException) {
            mutableError.value = "Enter a valid 24-hour time, such as 08:00."
            return
        }
        updateSettings { it.copy(morningSummaryTime = time) }
    }

    fun clearError() {
        mutableError.value = null
    }

    private fun updateSettings(transform: (UserSettings) -> UserSettings) {
        mutableError.value = null
        viewModelScope.launch {
            updateMutex.withLock {
                try {
                    val updated = transform(settingsStore.settings.first())
                    settingsStore.save(updated)
                    mutableScheduleStatus.value = summaryScheduler.schedule(updated)
                    if (!updated.morningSummaryEnabled) notifications.clearSummary()
                } catch (exception: Exception) {
                    mutableError.value =
                        "Unable to save summary settings: ${exception.message ?: "unknown error"}"
                }
            }
        }
    }

    class Factory(
        private val settingsStore: UserSettingsStore,
        private val summaryScheduler: MorningSummaryScheduler,
        private val notifications: DaylineNotificationFactory
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsStore, summaryScheduler, notifications) as T
        }
    }
}
