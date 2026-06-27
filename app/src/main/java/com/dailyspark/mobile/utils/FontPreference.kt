package com.dailyspark.mobile.utils

import android.content.Context

object FontPreference {

    private const val PREF_NAME = "quote_fonts"

    fun saveFont(context: Context, quoteId: Int, fontId: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("font_$quoteId", fontId)
            .apply()
    }

    fun getFont(context: Context, quoteId: Int): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt("font_$quoteId", -1)
    }
}