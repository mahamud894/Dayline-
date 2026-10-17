package com.dayline.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dayline.data.HabitRepository
import com.dayline.domain.model.Habit
import com.dayline.domain.model.HabitSchedule
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HabitViewModel(private val repository: HabitRepository) : ViewModel() {
    val habits: StateFlow<List<Habit>> = repository.observeHabits().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val mutableToday = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = mutableToday.asStateFlow()

    private val mutableCompletedHabitIds = MutableStateFlow<Set<String>>(emptySet())
    val completedHabitIds: StateFlow<Set<String>> = mutableCompletedHabitIds.asStateFlow()

    private val mutableSaveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = mutableSaveError.asStateFlow()

    init {
        viewModelScope.launch { refreshToday() }
        viewModelScope.launch {
            while (isActive) {
                delay(millisUntilNextMidnight())
                refreshToday()
            }
        }
    }

    fun saveHabit(
        existing: Habit?,
        title: String,
        schedule: HabitSchedule,
        onSaved: () -> Unit
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            mutableSaveError.value = "Add a name before saving."
            return
        }
        mutableSaveError.value = null

        viewModelScope.launch {
            repository.saveHabit(
                Habit(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    title = cleanTitle,
                    schedule = schedule,
                    createdAtLocalDate = existing?.createdAtLocalDate ?: today.value,
                    currentStreak = existing?.currentStreak ?: 0,
                    bestStreak = existing?.bestStreak ?: 0
                )
            )
            repository.refreshStreaks(today.value)
            onSaved()
        }
    }

    fun completeHabit(id: String) {
        viewModelScope.launch {
            val date = today.value
            repository.completeForDate(id, date)
            mutableCompletedHabitIds.value = repository.observeCompletedHabitIds(date).first()
        }
    }

    fun clearSaveError() {
        mutableSaveError.value = null
    }

    private suspend fun refreshToday() {
        val date = LocalDate.now(ZoneId.systemDefault())
        repository.refreshStreaks(date)
        mutableToday.value = date
        mutableCompletedHabitIds.value = repository.observeCompletedHabitIds(date).first()
    }

    private fun millisUntilNextMidnight(): Long {
        val zoneId = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zoneId)
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zoneId)
        return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1)
    }

    class Factory(private val repository: HabitRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HabitViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return HabitViewModel(repository) as T
        }
    }
}
