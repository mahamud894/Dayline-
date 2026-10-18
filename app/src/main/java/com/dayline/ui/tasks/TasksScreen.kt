package com.dayline.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskStatus
import com.dayline.domain.reminders.ReminderScheduleStatus
import com.dayline.ui.common.ReminderScheduleNotice

@Composable
fun TasksScreen(
    tasks: List<Task>,
    onEditTask: (Task) -> Unit,
    onCompleteTask: (Task) -> Unit,
    reminderScheduleStatus: ReminderScheduleStatus?,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val shown = tasks.filter { it.matchesSearch(query) }
    val activeTasks = shown.filter { it.status != TaskStatus.COMPLETED }
    val completedTasks = shown.filter { it.status == TaskStatus.COMPLETED }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "TASK DECK",
                color = Color(0xFFB7FF4A),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your tasks",
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                color = Color(0xFFF4F5FF),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (tasks.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task-search"),
                    placeholder = { Text("Search tasks") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }
        item {
            Text(
                text = "Open tasks",
                color = Color(0xFFC2C7D3),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            ReminderScheduleNotice(reminderScheduleStatus)
        }
        if (activeTasks.isEmpty()) {
            item {
                Text(
                    text = if (query.isBlank()) {
                        "Nothing waiting in your task deck."
                    } else {
                        "No open tasks match \"${query.trim()}\"."
                    },
                    color = Color(0xFFC2C7D3),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(activeTasks, key = Task::id) { task ->
                TaskCard(
                    task = task,
                    onEdit = { onEditTask(task) },
                    onComplete = { onCompleteTask(task) }
                )
            }
        }
        if (completedTasks.isNotEmpty()) {
            item {
                Text(
                    text = "Completed tasks",
                    modifier = Modifier.padding(top = 12.dp),
                    color = Color(0xFF59F5D8),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(completedTasks, key = Task::id) { task ->
                TaskCard(task = task, onEdit = {}, onComplete = {})
            }
        }
    }
}

/** Every word of [query] must appear in the title or notes, ignoring case. Blank matches all. */
internal fun Task.matchesSearch(query: String): Boolean {
    val words = query.trim().lowercase().split(Regex("\\s+")).filter(String::isNotEmpty)
    if (words.isEmpty()) return true
    val haystack = "$title\n${notes.orEmpty()}".lowercase()
    return words.all(haystack::contains)
}
