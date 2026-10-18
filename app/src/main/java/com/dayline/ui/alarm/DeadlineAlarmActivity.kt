package com.dayline.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.dayline.DaylineApp
import com.dayline.platform.alarm.DeadlineAlarmService
import com.dayline.platform.reminders.ReminderAction
import com.dayline.ui.theme.DaylineTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Full-screen page shown over the lock screen while a deadline alarm rings. */
class DeadlineAlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        val taskId = intent.getStringExtra(EXTRA_TASK_ID)
        if (taskId.isNullOrBlank()) {
            finish()
            return
        }
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val dueAt = intent.getLongExtra(EXTRA_DUE_AT, 0L)

        // Close once the alarm stops ringing for this task (timeout or a notification action).
        lifecycleScope.launch {
            DeadlineAlarmService.ringing.first { it != taskId }
            if (!isFinishing) finish()
        }

        setContent {
            DaylineTheme {
                DeadlineAlarmScreen(
                    title = title,
                    dueLabel = formatDue(dueAt),
                    onAction = { action -> act(taskId, action) }
                )
            }
        }
    }

    private fun act(taskId: String, action: ReminderAction) {
        val container = (application as DaylineApp).container
        lifecycleScope.launch {
            runCatching { container.reminderActionHandler.handle(taskId, action) }
            DeadlineAlarmService.stop(this@DeadlineAlarmActivity, taskId)
            finish()
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun formatDue(dueAt: Long): String {
        if (dueAt <= 0L) return ""
        val pattern = if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"
        val time = Instant.ofEpochMilli(dueAt).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern(pattern))
        return "Deadline was $time"
    }

    companion object {
        private const val EXTRA_TASK_ID = "com.dayline.extra.ALARM_SCREEN_TASK_ID"
        private const val EXTRA_TITLE = "com.dayline.extra.ALARM_SCREEN_TITLE"
        private const val EXTRA_DUE_AT = "com.dayline.extra.ALARM_SCREEN_DUE_AT"

        fun intent(context: Context, taskId: String, title: String, dueAt: Long): Intent =
            Intent(context, DeadlineAlarmActivity::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_DUE_AT, dueAt)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}

@Composable
private fun DeadlineAlarmScreen(
    title: String,
    dueLabel: String,
    onAction: (ReminderAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF33255C), Color(0xFF0B0D14)))
            )
            .padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "DEADLINE",
            color = Color(0xFFB7FF4A),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            color = Color(0xFFF4F5FF),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 40.sp
        )
        if (dueLabel.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(text = dueLabel, color = Color(0xFFC2C7D3), fontSize = 16.sp)
        }
        Spacer(Modifier.height(56.dp))
        Button(
            onClick = { onAction(ReminderAction.DONE) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("alarm-done"),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFB7FF4A),
                contentColor = Color(0xFF10150B)
            )
        ) {
            Text("Done", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = { onAction(ReminderAction.SNOOZE) },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("alarm-snooze"),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("Snooze", color = Color(0xFFC7A2FF), fontSize = 18.sp)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = { onAction(ReminderAction.DISMISS) },
            modifier = Modifier.testTag("alarm-dismiss")
        ) {
            Text("Dismiss", color = Color(0xFFC2C7D3), fontSize = 16.sp)
        }
    }
}
