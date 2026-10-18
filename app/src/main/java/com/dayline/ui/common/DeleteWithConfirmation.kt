package com.dayline.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag

/** A "Delete" text button that asks before calling [onConfirm]. */
@Composable
fun DeleteWithConfirmation(
    label: String,
    dialogTitle: String,
    dialogText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirming by remember { mutableStateOf(false) }
    TextButton(
        onClick = { confirming = true },
        modifier = modifier
            .fillMaxWidth()
            .testTag("delete-item")
    ) {
        Text(label, color = DangerColor)
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(dialogTitle) },
            text = { Text(dialogText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onConfirm()
                    },
                    modifier = Modifier.testTag("delete-confirm")
                ) { Text("Delete", color = DangerColor) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Cancel") }
            }
        )
    }
}

private val DangerColor = Color(0xFFFF8296)
