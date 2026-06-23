package com.dailyspark.mobile.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailyspark.mobile.repository.StatsRepository

class ShareCountReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        val statsRepo = StatsRepository.getInstance(context)
        statsRepo.incrementShareCount()
    }
}