package com.dailyspark.mobile.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StatsRepository private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("app_stats", Context.MODE_PRIVATE)

    private val _shareCount = MutableStateFlow(prefs.getInt("total_shares", 0))
    val shareCount: StateFlow<Int> = _shareCount.asStateFlow()

    fun getShareCount(): Int = _shareCount.value

    fun incrementShareCount() {
        val nextValue = _shareCount.value + 1
        prefs.edit().putInt("total_shares", nextValue).apply()
        _shareCount.value = nextValue
    }

    companion object {
        @Volatile
        private var INSTANCE: StatsRepository? = null

        fun getInstance(context: Context): StatsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StatsRepository(context).also { INSTANCE = it }
            }
        }
    }
}