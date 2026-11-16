package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

class FestiveAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Navidad Buzzer"
        val message = intent.getStringExtra(EXTRA_MESSAGE)
            ?: "¡Es hora de encender tus villancicos en el ESP32!"
        FestiveNotificationManager.showNotification(context, title, message, NOTIF_ID_DAILY)
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val NOTIF_ID_DAILY = 2512
    }
}

object FestiveNotificationManager {
    private const val CHANNEL_ID = "navidad_buzzer_channel"
    private const val CHANNEL_NAME = "Alertas Navideñas ESP32"
    private const val NOTIF_ID_TIMER = 2513
    private const val NOTIF_ID_TEST = 2514

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recordatorios navideños y avisos de apagado automático del ESP32"
                enableLights(true)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showTestNotification(context: Context, title: String, message: String) {
        showNotification(
            context,
            title.ifBlank { "Navidad Buzzer" },
            message.ifBlank { "¡Tu ESP32 navideño está listo para sonar!" },
            NOTIF_ID_TEST
        )
    }

    fun showAutoOffCompletedNotification(context: Context) {
        showNotification(
            context,
            "Apagado automático completado",
            "El buzzer del ESP32 y las luces navideñas se han apagado suavemente.",
            NOTIF_ID_TIMER
        )
    }

    fun showNotification(context: Context, title: String, message: String, notifId: Int) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        }
    }

    fun scheduleDailyReminder(
        context: Context,
        enabled: Boolean,
        hour: Int,
        minute: Int,
        title: String,
        message: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, FestiveAlarmReceiver::class.java).apply {
            putExtra(FestiveAlarmReceiver.EXTRA_TITLE, title)
            putExtra(FestiveAlarmReceiver.EXTRA_MESSAGE, message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            FestiveAlarmReceiver.NOTIF_ID_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        if (!enabled) return

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
