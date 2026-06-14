package me.neko.nzhelper.ui.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.neko.nzhelper.R
import me.neko.nzhelper.ui.util.NotificationUtil

/**
 * 前台计时服务
 */
class TimerService : Service() {
    private val binder = LocalBinder()
    private val _elapsedSec = MutableStateFlow(0)
    val elapsedSec: StateFlow<Int> = _elapsedSec.asStateFlow()

    // 暴露运行状态，供 UI 同步
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var startTimeMs: Long = 0L
    private var accumulatedSec: Int = 0

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        override fun run() {
            val nowMs = System.currentTimeMillis()
            _elapsedSec.value = accumulatedSec + ((nowMs - startTimeMs) / 1000).toInt()
            updateNotification(_elapsedSec.value)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer()
            ACTION_PAUSE -> pauseTimer()
            ACTION_STOP -> stopTimer()
        }
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startTimer() {
        if (startTimeMs == 0L) {
            startTimeMs = System.currentTimeMillis()
            _isRunning.value = true
        }
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)
        val notif = buildNotification(_elapsedSec.value)
        startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun pauseTimer() {
        handler.removeCallbacks(tickRunnable)
        if (startTimeMs != 0L) {
            accumulatedSec = _elapsedSec.value
            startTimeMs = 0L
            _isRunning.value = false
            updateNotification(accumulatedSec)
        }
    }

    @Suppress("DEPRECATION")
    private fun stopTimer() {
        handler.removeCallbacks(tickRunnable)
        stopForeground(true)
        stopSelf()
        accumulatedSec = 0
        startTimeMs = 0L
        _elapsedSec.value = 0
        _isRunning.value = false
    }

    private fun buildNotification(elapsed: Int): Notification {
        return NotificationCompat.Builder(this, NotificationUtil.CHANNEL_ID)
            .setContentTitle(if (_isRunning.value) "计时进行中" else "计时已暂停")
            .setContentText(formatTime(elapsed))
            .setSmallIcon(R.drawable.baseline_access_alarm_24)
            .setOngoing(_isRunning.value)
            .build()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun updateNotification(elapsed: Int) {
        val notif = buildNotification(elapsed)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID, notif)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "me.neko.nzhelper.ACTION_START"
        const val ACTION_PAUSE = "me.neko.nzhelper.ACTION_PAUSE"
        const val ACTION_STOP = "me.neko.nzhelper.ACTION_STOP"
        const val NOTIF_ID = 1001
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return buildString {
            if (h > 0) append(String.format("%02d:", h))
            append(String.format("%02d:%02d", m, s))
        }
    }
}