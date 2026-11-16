package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class ChristmasCountdown(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val isChristmasDay: Boolean,
    val targetYear: Int
)

class ChristmasCountdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun calculateCountdown(nowMillis: Long = System.currentTimeMillis()): ChristmasCountdown {
            val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val month = now.get(Calendar.MONTH)
            val day = now.get(Calendar.DAY_OF_MONTH)

            if (month == Calendar.DECEMBER && day == 25) {
                return ChristmasCountdown(
                    days = 0L,
                    hours = 0L,
                    minutes = 0L,
                    isChristmasDay = true,
                    targetYear = now.get(Calendar.YEAR)
                )
            }

            var targetYear = now.get(Calendar.YEAR)
            if (month == Calendar.DECEMBER && day > 25) {
                targetYear += 1
            }

            val christmas = Calendar.getInstance().apply {
                set(Calendar.YEAR, targetYear)
                set(Calendar.MONTH, Calendar.DECEMBER)
                set(Calendar.DAY_OF_MONTH, 25)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val diffMs = (christmas.timeInMillis - nowMillis).coerceAtLeast(0L)
            val days = TimeUnit.MILLISECONDS.toDays(diffMs)
            val hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60

            return ChristmasCountdown(
                days = days,
                hours = hours,
                minutes = minutes,
                isChristmasDay = false,
                targetYear = targetYear
            )
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val countdown = calculateCountdown()
            val views = RemoteViews(context.packageName, R.layout.widget_christmas_countdown)

            if (countdown.isChristmasDay) {
                views.setTextViewText(R.id.widget_days_number, "¡HOY!")
                views.setTextViewText(R.id.widget_countdown_subtitle, "¡Feliz Navidad! Enciende tu ESP32")
            } else {
                views.setTextViewText(R.id.widget_days_number, "${countdown.days} días")
                views.setTextViewText(
                    R.id.widget_countdown_subtitle,
                    "${countdown.hours}h ${countdown.minutes}m para Navidad ${countdown.targetYear}"
                )
            }

            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun refreshAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, ChristmasCountdownWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                for (id in ids) {
                    updateWidget(context, manager, id)
                }
            }
        }

        fun canRequestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val manager = context.getSystemService(AppWidgetManager::class.java) ?: return false
            return manager.isRequestPinAppWidgetSupported
        }

        fun requestPinWidget(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val manager = context.getSystemService(AppWidgetManager::class.java) ?: return false
            if (!manager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, ChristmasCountdownWidgetProvider::class.java)
            return manager.requestPinAppWidget(provider, null, null)
        }
    }
}
