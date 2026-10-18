package com.dayline.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dayline.data.backup.BackupFormatException
import com.dayline.data.backup.DaylineBackup
import com.dayline.data.local.UserSettingsStore
import com.dayline.domain.model.UserSettings
import com.dayline.platform.notifications.DaylineNotificationFactory
import com.dayline.platform.summary.MorningSummaryScheduleStatus
import com.dayline.platform.summary.MorningSummaryScheduler
import java.time.LocalTime
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val settingsStore: UserSettingsStore,
    private val summaryScheduler: MorningSummaryScheduler,
    private val notifications: DaylineNotificationFactory,
    private val backup: DaylineBackup
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

    private val mutableBackupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = mutableBackupMessage.asStateFlow()

    private val updateMutex = Mutex()

    init {
        refreshSummaryScheduleStatus()
    }

    fun setSummaryEnabled(enabled: Boolean) {
        updateSettings { it.copy(morningSummaryEnabled = enabled) }
    }

    fun refreshSummaryScheduleStatus() {
        viewModelScope.launch {
            updateMutex.withLock {
                try {
                    mutableScheduleStatus.value =
                        summaryScheduler.schedule(settingsStore.settings.first())
                } catch (exception: Exception) {
                    mutableError.value =
                        "Unable to check summary schedule: ${exception.message ?: "unknown error"}"
                }
            }
        }
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

    /** [write] stores the JSON wherever the user chose; it runs off the main thread. */
    fun exportBackup(write: suspend (String) -> Unit) {
        runBackupJob("Backup saved.", "Couldn't save the backup") {
            write(backup.export())
        }
    }

    fun restoreBackup(read: suspend () -> String) {
        runBackupJob("Backup restored.", "Couldn't restore the backup") {
            backup.restore(read())
        }
    }

    fun clearBackupMessage() {
        mutableBackupMessage.value = null
    }

    private fun runBackupJob(success: String, failure: String, job: suspend () -> Unit) {
        mutableBackupMessage.value = null
        viewModelScope.launch {
            mutableBackupMessage.value = try {
                withContext(Dispatchers.IO) { job() }
                success
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: BackupFormatException) {
                exception.message
            } catch (exception: Exception) {
                "$failure: ${exception.message ?: "unknown error"}"
            }
        }
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
        private val notifications: DaylineNotificationFactory,
    private val backup: DaylineBackup
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsStore, summaryScheduler, notifications, backup) as T
        }
    }
}
