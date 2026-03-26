package com.oref.alert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlertService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val overlayManager: AlertOverlayManager
        get() = (application as OrefApplication).overlayManager
    private val apiClient = OrefApiClient()
    private var lastAlertId = ""

    companion object {
        const val CHANNEL_ID = "oref_alert_channel"
        const val NOTIFICATION_ID = 1001
        const val POLL_INTERVAL_MS = 5_000L

        fun start(context: Context) {
            val intent = Intent(context, AlertService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("מפעיל... בודק התרעות"))
        WatchdogReceiver.schedule(this)  // keep-alive watchdog every 60s
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        overlayManager.hideAlert()
    }

    private fun startPolling() {
        scope.launch {
            while (true) {
                try {
                    val alert = apiClient.getActiveAlerts()

                    withContext(Dispatchers.Main) {
                        if (alert != null) {
                            // Skip general alerts if user disabled them
                            val isGeneral = !alert.isHomeCity(Prefs.homeCity(applicationContext))
                            if (isGeneral && !Prefs.showGeneralAlerts(applicationContext)) {
                                return@withContext
                            }
                            if (alert.id != lastAlertId) {
                                lastAlertId = alert.id
                                updateNotification("התרעה פעילה: ${alert.citiesText}")
                            }
                            overlayManager.showAlert(alert)
                        } else {
                            // No active alert
                            if (lastAlertId.isNotEmpty()) {
                                lastAlertId = ""
                                overlayManager.hideAlert()
                                updateNotification("אין התרעות פעילות")
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Network error - keep going
                }

                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "פיקוד העורף - התרעות",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "שירות ניטור התרעות פיקוד העורף"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("פיקוד העורף")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
