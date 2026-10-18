package com.dayline.platform.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.content.ContextCompat
import com.dayline.DaylineApp
import com.dayline.domain.model.Task
import com.dayline.domain.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Rings a deadline like an alarm clock: looping alarm sound at alarm volume, vibration and a
 * full-screen alarm page. Stops on its own after [RING_DURATION_MILLIS] and leaves the normal
 * overdue notification behind.
 */
class DeadlineAlarmService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var ringingTaskId: String? = null

    private val timeout = Runnable {
        ringingTaskId?.let(::leaveOverdueNotification)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getStringExtra(EXTRA_TASK_ID)
        val title = intent?.getStringExtra(EXTRA_TITLE)
        val dueAt = intent?.getLongExtra(EXTRA_DUE_AT, 0L) ?: 0L
        if (taskId.isNullOrBlank() || title == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = container().notificationFactory
            .buildDeadlineAlarmNotification(taskId, title, dueAt)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ALARM_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(ALARM_NOTIFICATION_ID, notification)
        }

        // A new deadline while another is ringing: the earlier one falls back to its notification.
        ringingTaskId?.takeIf { it != taskId }?.let(::leaveOverdueNotification)
        ringingTaskId = taskId
        mutableRinging.value = taskId
        startSound()
        startVibration()
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, RING_DURATION_MILLIS)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeout)
        stopSoundAndVibration()
        mutableRinging.value = null
        super.onDestroy()
    }

    private fun startSound() {
        stopSound()
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@DeadlineAlarmService, uri)
                setWakeMode(this@DeadlineAlarmService, PowerManager.PARTIAL_WAKE_LOCK)
                isLooping = true
                prepare()
                start()
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to play the deadline alarm sound.", exception)
            null
        }
    }

    private fun startVibration() {
        val vibrator = ContextCompat.getSystemService(this, Vibrator::class.java) ?: return
        this.vibrator = vibrator
        @Suppress("DEPRECATION")
        vibrator.vibrate(
            VibrationEffect.createWaveform(VIBRATION_PATTERN, 0),
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
        )
    }

    private fun stopSound() {
        player?.runCatching {
            stop()
            release()
        }
        player = null
    }

    private fun stopSoundAndVibration() {
        stopSound()
        vibrator?.cancel()
        vibrator = null
    }

    private fun leaveOverdueNotification(taskId: String) {
        val container = container()
        // Own scope: the service is usually stopping right after this call.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            container.taskRepository.getTask(taskId)
                ?.takeIf { it.status == TaskStatus.OVERDUE }
                ?.let(container.notificationFactory::showOverdue)
        }
    }

    private fun container() = (application as DaylineApp).container

    companion object {
        const val ALARM_NOTIFICATION_ID = 3
        const val RING_DURATION_MILLIS = 30_000L
        private const val EXTRA_TASK_ID = "com.dayline.extra.ALARM_TASK_ID"
        private const val EXTRA_TITLE = "com.dayline.extra.ALARM_TITLE"
        private const val EXTRA_DUE_AT = "com.dayline.extra.ALARM_DUE_AT"
        private const val TAG = "DaylineAlarm"
        private val VIBRATION_PATTERN = longArrayOf(0, 700, 500)

        private val mutableRinging = MutableStateFlow<String?>(null)

        /** Id of the task whose alarm is ringing right now, or null. */
        val ringing: StateFlow<String?> = mutableRinging.asStateFlow()

        /** Starts ringing. Throws if Android does not allow a foreground service right now. */
        fun start(context: Context, task: Task) {
            val intent = Intent(context, DeadlineAlarmService::class.java)
                .putExtra(EXTRA_TASK_ID, task.id)
                .putExtra(EXTRA_TITLE, task.title)
                .putExtra(EXTRA_DUE_AT, task.dueAtEpochMillis ?: 0L)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Silences the alarm if it is ringing for [taskId]. Safe to call from the background. */
        fun stop(context: Context, taskId: String) {
            if (mutableRinging.value == taskId) {
                context.stopService(Intent(context, DeadlineAlarmService::class.java))
            }
        }
    }
}
