package com.dayline.platform.summary

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dayline.DaylineApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmartWakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isUnlock = intent.action == Intent.ACTION_USER_PRESENT
        val isFallback = intent.action == MorningSummaryScheduler.ACTION_SUMMARY_FALLBACK
        if (!isUnlock && !isFallback) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val coordinator = (context.applicationContext as DaylineApp)
                    .container
                    .smartWakeCoordinator
                if (isUnlock) coordinator.onUnlock() else coordinator.onFallback()
            } catch (exception: Exception) {
                Log.e(TAG, "Unable to deliver the daily Dayline summary.", exception)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "DaylineSmartWake"
    }
}
