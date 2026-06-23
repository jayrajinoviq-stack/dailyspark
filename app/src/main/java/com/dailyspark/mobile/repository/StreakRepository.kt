package com.dailyspark.mobile.repository

import android.content.Context

class StreakRepository(context: Context) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun getStreakCount(): Int = prefs.getInt("streak_count", 0)
    
    fun getLastDate(): String? = prefs.getString("last_date", null)

    fun saveStreak(count: Int, date: String) {
        prefs.edit().apply {
            putInt("streak_count", count)
            putString("last_date", date)
            apply()
        }
    }
}