package com.dayline.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskPriority
import com.dayline.ui.common.DeleteWithConfirmation
import com.dayline.domain.model.TaskStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun TaskCard(
    task: Task,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171D2A))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = task.title,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFF4F5FF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (task.priority == TaskPriority.HIGH) {
                    Text(
                        text = "HIGH",
                        color = Color(0xFFB7FF4A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = formatTaskDueAt(task.dueAtEpochMillis),
                color = Color(0xFFC2C7D3),
                fontSize = 13.sp
            )
            task.notes?.let {
                Text(text = it, color = Color(0xFFC2C7D3), fontSize = 13.sp)
            }
            if (task.status == TaskStatus.COMPLETED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Completed",
                        color = Color(0xFF59F5D8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    onDelete?.let {
                        DeleteWithConfirmation(
                            label = "Delete",
                            dialogTitle = "Delete this task?",
                            dialogText = "\"${task.title}\" will be removed.",
                            onConfirm = it,
                            modifier = Modifier.semantics {
                                contentDescription = "Delete task: ${task.title}"
                            }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.semantics {
                            contentDescription = "Edit task: ${task.title}"
                        }
                    ) {
                        Text("Edit", color = Color(0xFFC7A2FF))
                    }
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.semantics {
                            contentDescription = "Complete task: ${task.title}"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB7FF4A),
                            contentColor = Color(0xFF10150B)
                        )
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

internal fun formatTaskDueAt(epochMillis: Long?): String {
    if (epochMillis == null) return "No deadline"
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a"))
}
