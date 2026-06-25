package com.dailyspark.mobile.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dailyspark.mobile.receiver.ReminderReceiver
import java.util.Calendar

object ReminderHelper {

    private const val PREFS = "daily_spark_reminder"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HOUR = "hour"
    private const val KEY_MIN = "min"

    fun saveTime(context: Context, hour: Int, min: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MIN, min)
            .apply()

        scheduleAlarm(context)
    }

    fun getHour(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_HOUR, 8)
    }

    fun getMin(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_MIN, 0)
    }

    fun setReminderEnabled(context: Context, enabled: Boolean) {

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()

        if (enabled) {
            scheduleAlarm(context)
        } else {
            cancelAlarm(context)
        }
    }

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }

    fun scheduleAlarm(context: Context) {

        if (!isEnabled(context)) return

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {

            set(Calendar.HOUR_OF_DAY, getHour(context))
            set(Calendar.MINUTE, getMin(context))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.cancel(pendingIntent)

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

    }

    fun cancelAlarm(context: Context) {

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}