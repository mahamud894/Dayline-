package com.dayline.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.dayline.R
import com.dayline.ui.common.UiText
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

    private val mutableError = MutableStateFlow<UiText?>(null)
    val error: StateFlow<UiText?> = mutableError.asStateFlow()

    private val mutableBackupMessage = MutableStateFlow<UiText?>(null)
    val backupMessage: StateFlow<UiText?> = mutableBackupMessage.asStateFlow()

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
                        UiText(R.string.error_summary_check, exception.message.orEmpty())
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
                        UiText(R.string.error_calendar_update, exception.message.orEmpty())
                }
            }
        }
    }

    fun reportCalendarPermissionDenied() {
        mutableError.value =
            UiText(R.string.error_calendar_denied)
    }

    fun saveSummaryTime(value: String) {
        val time = try {
            LocalTime.parse(value.trim())
        } catch (_: DateTimeParseException) {
            mutableError.value = UiText(R.string.error_summary_time)
            return
        }
        updateSettings { it.copy(morningSummaryTime = time) }
    }

    fun clearError() {
        mutableError.value = null
    }

    /** [write] stores the JSON wherever the user chose; it runs off the main thread. */
    fun exportBackup(write: suspend (String) -> Unit) {
        runBackupJob(R.string.backup_saved, R.string.error_backup_save) {
            write(backup.export())
        }
    }

    fun restoreBackup(read: suspend () -> String) {
        runBackupJob(R.string.backup_restored, R.string.error_backup_restore) {
            backup.restore(read())
        }
    }

    fun clearBackupMessage() {
        mutableBackupMessage.value = null
    }

    private fun runBackupJob(@StringRes success: Int, @StringRes failure: Int, job: suspend () -> Unit) {
        mutableBackupMessage.value = null
        viewModelScope.launch {
            mutableBackupMessage.value = try {
                withContext(Dispatchers.IO) { job() }
                UiText(success)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: BackupFormatException) {
                UiText(exception.reason)
            } catch (exception: Exception) {
                UiText(failure, exception.message.orEmpty())
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
                        UiText(R.string.error_summary_save, exception.message.orEmpty())
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
