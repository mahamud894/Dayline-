package com.dayline.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayline.AppContainer
import com.dayline.domain.model.Task
import com.dayline.ui.tasks.TaskEditorScreen
import com.dayline.ui.tasks.TaskViewModel
import com.dayline.ui.tasks.TasksScreen
import com.dayline.ui.today.TodayScreen
import com.dayline.ui.today.TodayViewModel

private enum class Destination(val label: String) {
    TODAY("Today"),
    TASKS("Tasks"),
    HABITS("Habits"),
    PROFILE("Profile")
}

@Composable
fun DaylineNavHost(container: AppContainer) {
    val taskViewModel: TaskViewModel = viewModel(
        factory = remember(container) { TaskViewModel.Factory(container.taskRepository) }
    )
    val todayViewModel: TodayViewModel = viewModel(
        factory = remember(container) { TodayViewModel.Factory(container.habitRepository) }
    )
    val tasks by taskViewModel.tasks.collectAsState()
    val habits by todayViewModel.habits.collectAsState()
    val saveError by taskViewModel.saveError.collectAsState()
    var selected by rememberSaveable { mutableStateOf(Destination.TODAY) }
    var isEditorVisible by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingTask = editingTaskId?.let { id -> tasks.firstOrNull { it.id == id } }

    if (isEditorVisible) {
        TaskEditorScreen(
            task = editingTask,
            saveError = saveError,
            onBack = {
                isEditorVisible = false
                taskViewModel.clearSaveError()
            },
            onClearError = taskViewModel::clearSaveError,
            onSave = { title, notes, dueAtText, priority, quietModeOverrideAllowed ->
                taskViewModel.saveTask(
                    existing = editingTask,
                    title = title,
                    notes = notes,
                    dueAtText = dueAtText,
                    priority = priority,
                    quietModeOverrideAllowed = quietModeOverrideAllowed
                ) {
                    isEditorVisible = false
                    editingTaskId = null
                }
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color(0xFF0B0D14),
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF111620)) {
                    Destination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = selected == destination,
                            onClick = { selected = destination },
                            modifier = Modifier.testTag("nav-${destination.label.lowercase()}"),
                            icon = {
                                Text(
                                    text = when (destination) {
                                        Destination.TODAY -> "◎"
                                        Destination.TASKS -> "▤"
                                        Destination.HABITS -> "✦"
                                        Destination.PROFILE -> "◉"
                                    },
                                    fontSize = 18.sp
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selected == Destination.TODAY) {
                    FloatingActionButton(
                        onClick = {
                            editingTaskId = null
                            taskViewModel.clearSaveError()
                            isEditorVisible = true
                        },
                        modifier = Modifier
                            .testTag("add-task-fab")
                            .semantics { contentDescription = "Add task" },
                        shape = CircleShape,
                        containerColor = Color(0xFFB7FF4A),
                        contentColor = Color(0xFF10150B)
                    ) {
                        Text("+", fontSize = 30.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        ) { contentPadding ->
            when (selected) {
                Destination.TODAY -> TodayScreen(
                    tasks = tasks,
                    habits = habits,
                    modifier = Modifier.padding(contentPadding)
                )
                Destination.TASKS -> TasksScreen(
                    tasks = tasks,
                    onEditTask = { task ->
                        editingTaskId = task.id
                        taskViewModel.clearSaveError()
                        isEditorVisible = true
                    },
                    onCompleteTask = { taskViewModel.completeTask(it.id) },
                    modifier = Modifier.padding(contentPadding)
                )
                Destination.HABITS -> PlaceholderScreen("Your routines are getting ready.")
                Destination.PROFILE -> PlaceholderScreen("Your progress lives here.")
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("DAYLINE", color = Color(0xFFB7FF4A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(message, modifier = Modifier.padding(top = 8.dp), color = Color(0xFFF4F5FF), fontSize = 22.sp)
    }
}
