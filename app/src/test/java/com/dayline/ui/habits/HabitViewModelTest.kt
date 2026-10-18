package com.dayline.ui.habits

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.dayline.data.HabitRepository
import com.dayline.domain.model.Habit
import java.time.LocalDate
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModelTest {
    @Test
    fun refreshingAfterTimezoneChangeUsesNewLocalDate() = runTest {
        val originalTimeZone = TimeZone.getDefault()
        val store = ViewModelStore()
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
            val repository = RecordingHabitRepository()
            val viewModel = ViewModelProvider(
                store,
                HabitViewModel.Factory(repository)
            )[HabitViewModel::class.java]
            runCurrent()
            val previousDate = viewModel.today.value

            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Pago_Pago"))
            viewModel.refreshToday()
            runCurrent()

            val expectedDate = LocalDate.now()
            assertNotEquals(previousDate, expectedDate)
            assertEquals(expectedDate, viewModel.today.value)
            assertEquals(expectedDate, repository.refreshedDates.last())
        } finally {
            store.clear()
            Dispatchers.resetMain()
            TimeZone.setDefault(originalTimeZone)
        }
    }

    private class RecordingHabitRepository : HabitRepository {
        val refreshedDates = mutableListOf<LocalDate>()

        override fun observeHabits(): Flow<List<Habit>> = flowOf(emptyList())
        override fun observeCompletedHabitIds(date: LocalDate): Flow<Set<String>> =
            flowOf(emptySet())
        override suspend fun saveHabit(habit: Habit) = Unit
        override suspend fun completeForDate(id: String, date: LocalDate) = Unit
        override suspend fun refreshStreaks(today: LocalDate) {
            refreshedDates += today
        }
        override suspend fun deleteHabit(id: String) = Unit
    }
}
