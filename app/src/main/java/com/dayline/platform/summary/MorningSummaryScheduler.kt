package com.dayline.platform.summary

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.dayline.DaylineApp
import com.dayline.domain.model.UserSettings
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

enum class MorningSummaryScheduleStatus {
    EXACT,
    WORK_MANAGER_FALLBACK,
    DISABLED,
    NOTIFICATION_PERMISSION_REQUIRED
}

class MorningSummaryScheduler(
    context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = clock.zone,
    private val exactAlarmAccess: () -> Boolean = {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            requireService<AlarmManager>(context, Context.ALARM_SERVICE)
                .canScheduleExactAlarms()
    },
    private val notificationAccess: () -> Boolean = {
        NotificationManagerCompat.from(context.applicationContext).areNotificationsEnabled()
    },
    private val workEnqueuer: SummaryFallbackWorkEnqueuer =
        WorkManagerSummaryFallbackWorkEnqueuer(context.applicationContext, clock)
) {
    private val appContext = context.applicationContext
    private val alarmManager = requireService<AlarmManager>(appContext, Context.ALARM_SERVICE)

    suspend fun schedule(settings: UserSettings): MorningSummaryScheduleStatus {
        cancel()
        if (!settings.morningSummaryEnabled) return MorningSummaryScheduleStatus.DISABLED
        if (!notificationAccess()) {
            return MorningSummaryScheduleStatus.NOTIFICATION_PERMISSION_REQUIRED
        }

        val now = LocalDateTime.ofInstant(clock.instant(), zoneId)
        var fallbackAt = now.toLocalDate().atTime(settings.morningSummaryTime)
        if (fallbackAt.isBefore(now)) fallbackAt = fallbackAt.plusDays(1)
        val triggerAt = fallbackAt.atZone(zoneId).toInstant().toEpochMilli()
        if (exactAlarmAccess()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                fallbackPendingIntent()
            )
            return MorningSummaryScheduleStatus.EXACT
        }

        workEnqueuer.enqueue(triggerAt)
        return MorningSummaryScheduleStatus.WORK_MANAGER_FALLBACK
    }

    suspend fun cancel() {
        alarmManager.cancel(fallbackPendingIntent())
        workEnqueuer.cancel()
    }

    private fun fallbackPendingIntent() = PendingIntent.getBroadcast(
        appContext,
        FALLBACK_REQUEST_CODE,
        Intent(appContext, SmartWakeReceiver::class.java).apply {
            action = ACTION_SUMMARY_FALLBACK
            data = Uri.parse("dayline://summary/fallback")
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        const val ACTION_SUMMARY_FALLBACK = "com.dayline.action.SUMMARY_FALLBACK"
        const val FALLBACK_REQUEST_CODE = 4101
    }
}

interface SummaryFallbackWorkEnqueuer {
    fun enqueue(triggerAtEpochMillis: Long)
    fun cancel()
}

private class WorkManagerSummaryFallbackWorkEnqueuer(
    context: Context,
    private val clock: Clock
) : SummaryFallbackWorkEnqueuer {
    private val workManager = WorkManager.getInstance(context)

    override fun enqueue(triggerAtEpochMillis: Long) {
        val request = OneTimeWorkRequest.Builder(MorningSummaryWorker::class.java)
            .setInputData(
                Data.Builder()
                    .putLong(MorningSummaryWorker.EXTRA_TRIGGER_AT, triggerAtEpochMillis)
                    .build()
            )
            .setInitialDelay(
                (triggerAtEpochMillis - clock.millis()).coerceAtLeast(0),
                TimeUnit.MILLISECONDS
            )
            .build()
        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun cancel() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "dayline-morning-summary"
    }
}

private inline fun <reified T : Any> requireService(context: Context, name: String): T =
    checkNotNull(ContextCompat.getSystemService(context, T::class.java)) {
        "Android system service is unavailable: $name"
    }

class MorningSummaryWorker(
    context: Context,
    parameters: androidx.work.WorkerParameters
) : androidx.work.CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val triggerAt = inputData.getLong(EXTRA_TRIGGER_AT, Long.MIN_VALUE)
        check(triggerAt != Long.MIN_VALUE) { "Morning summary work is missing its trigger time." }
        (applicationContext as DaylineApp).container.smartWakeCoordinator.onFallback()
        return Result.success()
    }

    companion object {
        const val EXTRA_TRIGGER_AT = "com.dayline.extra.SUMMARY_TRIGGER_AT"
    }
}
