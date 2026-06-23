package com.dailyspark.mobile.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dailyspark.mobile.service.ShareCountReceiver

object ShareHelper {

    fun shareQuote(context: Context, text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        val receiverIntent = Intent(context, ShareCountReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            receiverIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val chooser =
            Intent.createChooser(sendIntent, "Share Quote via", pendingIntent.intentSender)
        context.startActivity(chooser)
    }

}