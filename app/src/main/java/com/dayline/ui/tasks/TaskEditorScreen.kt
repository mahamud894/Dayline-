package com.dayline.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority

@Composable
fun TaskEditorScreen(
    task: Task?,
    saveError: String?,
    onBack: () -> Unit,
    onClearError: () -> Unit,
    onSave: (
        title: String,
        notes: String,
        dueAtText: String,
        priority: TaskPriority,
        quietModeOverrideAllowed: Boolean
    ) -> Unit
) {
    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var notes by remember(task?.id) { mutableStateOf(task?.notes.orEmpty()) }
    var dueAtText by remember(task?.id) {
        mutableStateOf(task?.dueAtEpochMillis?.let(::formatTaskDueInput).orEmpty())
    }
    var priority by remember(task?.id) {
        mutableStateOf(task?.priority ?: TaskPriority.NORMAL)
    }
    var quietModeOverrideAllowed by remember(task?.id) {
        mutableStateOf(task?.quietModeOverrideAllowed ?: false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("Back", color = Color(0xFFC7A2FF))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (task == null) "QUICK CAPTURE" else "REFINE TASK",
                color = Color(0xFFB7FF4A),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (task == null) "New task" else "Edit task",
                color = Color(0xFFF4F5FF),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                onClearError()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task-title"),
            label = { Text("What needs doing?") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Notes (optional)") },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(18.dp)
        )
        OutlinedTextField(
            value = dueAtText,
            onValueChange = {
                dueAtText = it
                onClearError()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task-due-at"),
            label = { Text("Deadline (optional)") },
            placeholder = { Text("YYYY-MM-DD HH:MM") },
            supportingText = { Text("Local time, for example 2026-10-18 14:30") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "PRIORITY",
                color = Color(0xFFC2C7D3),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = priority == TaskPriority.NORMAL,
                    onClick = { priority = TaskPriority.NORMAL },
                    label = { Text("Normal") },
                    modifier = Modifier.testTag("priority-normal")
                )
                FilterChip(
                    selected = priority == TaskPriority.HIGH,
                    onClick = { priority = TaskPriority.HIGH },
                    label = { Text("High") },
                    modifier = Modifier.testTag("priority-high")
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Urgent alerts", color = Color(0xFFF4F5FF), fontWeight = FontWeight.Medium)
                Text(
                    "Allow this task to break quiet settings",
                    color = Color(0xFFC2C7D3),
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = quietModeOverrideAllowed,
                onCheckedChange = { quietModeOverrideAllowed = it },
                modifier = Modifier.testTag("quiet-mode-override")
            )
        }
        saveError?.let {
            Text(text = it, color = Color(0xFFFF8296), fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                onSave(title, notes, dueAtText, priority, quietModeOverrideAllowed)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save-task"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB7FF4A),
                contentColor = Color(0xFF10150B)
            )
        ) {
            Text("Save task", fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatTaskDueInput(epochMillis: Long): String =
    java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
