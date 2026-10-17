package com.dayline.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dayline.data.HabitRepository
import com.dayline.domain.model.Habit
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodayViewModel(private val repository: HabitRepository) : ViewModel() {
    val habits: StateFlow<List<Habit>> = repository.observeHabits().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun completeHabit(id: String, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            repository.completeForDate(id, date)
        }
    }

    class Factory(private val repository: HabitRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(TodayViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return TodayViewModel(repository) as T
        }
    }
}
