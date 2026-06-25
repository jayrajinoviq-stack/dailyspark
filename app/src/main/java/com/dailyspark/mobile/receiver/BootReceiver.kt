package com.dailyspark.mobile.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailyspark.mobile.utils.ReminderHelper

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            if (ReminderHelper.isEnabled(context)) {
                ReminderHelper.scheduleAlarm(context)
            }
        }
    }
}