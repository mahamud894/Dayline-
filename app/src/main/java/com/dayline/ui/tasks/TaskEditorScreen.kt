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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.dayline.ui.common.DeleteWithConfirmation
import com.dayline.ui.common.UiText
import com.dayline.R
import androidx.compose.ui.res.stringResource

@Composable
fun TaskEditorScreen(
    task: Task?,
    saveError: UiText?,
    onBack: () -> Unit,
    onClearError: () -> Unit,
    onSave: (
        title: String,
        notes: String,
        dueAtText: String,
        priority: TaskPriority,
        quietModeOverrideAllowed: Boolean
    ) -> Unit,
    onDelete: () -> Unit = {}
) {
    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var notes by rememberSaveable(task?.id) { mutableStateOf(task?.notes.orEmpty()) }
    var deadline by rememberSaveable(task?.id) {
        mutableStateOf(task?.dueAtEpochMillis?.let(::toLocalDeadline))
    }
    var priority by rememberSaveable(task?.id) {
        mutableStateOf(task?.priority ?: TaskPriority.NORMAL)
    }
    var quietModeOverrideAllowed by rememberSaveable(task?.id) {
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
            Text(stringResource(R.string.action_back), color = Color(0xFFC7A2FF))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(if (task == null) R.string.task_editor_eyebrow_new else R.string.task_editor_eyebrow_edit),
                color = Color(0xFFB7FF4A),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(if (task == null) R.string.task_editor_title_new else R.string.task_editor_title_edit),
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
            label = { Text(stringResource(R.string.task_field_title)) },
            singleLine = true,
            shape = RoundedCornerShape(18.dp)
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.task_field_notes)) },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(18.dp)
        )
        DeadlineField(
            deadline = deadline,
            onDeadlineChange = {
                deadline = it
                onClearError()
            }
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.task_priority),
                color = Color(0xFFC2C7D3),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = priority == TaskPriority.NORMAL,
                    onClick = { priority = TaskPriority.NORMAL },
                    label = { Text(stringResource(R.string.priority_normal)) },
                    modifier = Modifier.testTag("priority-normal")
                )
                FilterChip(
                    selected = priority == TaskPriority.HIGH,
                    onClick = { priority = TaskPriority.HIGH },
                    label = { Text(stringResource(R.string.priority_high)) },
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
                Text(stringResource(R.string.task_urgent_alerts), color = Color(0xFFF4F5FF), fontWeight = FontWeight.Medium)
                Text(
                    stringResource(R.string.task_urgent_alerts_detail),
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
            Text(text = it.resolve(), color = Color(0xFFFF8296), fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                onSave(
                    title,
                    notes,
                    deadline?.let(::formatDeadlineForSave).orEmpty(),
                    priority,
                    quietModeOverrideAllowed
                )
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
            Text(stringResource(R.string.task_save), fontWeight = FontWeight.Bold)
        }
        if (task != null) {
            DeleteWithConfirmation(
                label = stringResource(R.string.task_delete),
                dialogTitle = stringResource(R.string.task_delete_title),
                dialogText = stringResource(R.string.task_delete_text, task.title),
                onConfirm = onDelete
            )
        }
    }
}

private fun toLocalDeadline(epochMillis: Long): java.time.LocalDateTime =
    java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
