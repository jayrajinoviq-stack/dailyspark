package com.dailyspark.mobile.utils

import com.dailyspark.mobile.R
import com.dailyspark.mobile.model.FontListItem
import com.dailyspark.mobile.model.FontOption

object FontUtils {

    fun getAppFonts() = listOf(
        FontOption(7, "Manrope", R.font.manrope),
        FontOption(8, "Merriweather", R.font.merriweather),
        FontOption(9, "Montserrat", R.font.montserrat),
        FontOption(10, "Nunito", R.font.nunito),
        FontOption(11, "Pacifico", R.font.pacifico),
        FontOption(12, "Poppins", R.font.poppins),
        FontOption(13, "Raleway", R.font.raleway),
        FontOption(14, "Satisfy", R.font.satisfy),

        FontOption(1, "Great Vibes", R.font.great_vibes, true),
        FontOption(2, "Dancing Script", R.font.dancing_script, true),
        FontOption(3, "Playfair", R.font.playfair_display, true),
        FontOption(4, "Cormorant", R.font.cormorant_garamond, true),
        FontOption(5, "DM Serif", R.font.dm_serif_display, true),
        FontOption(6, "Kaushan", R.font.kaushan_script, true)
    )

    fun getGroupedFontListItems(): List<FontListItem> {
        val fonts = getAppFonts()
        val free = fonts.filter { !it.isPremium }
        val premium = fonts.filter { it.isPremium }

        val result = mutableListOf<FontListItem>()
        if (free.isNotEmpty()) {
            result.add(FontListItem.Header("Free Fonts", isPremium = false))
            result.addAll(free.map { FontListItem.Item(it) })
        }
        if (premium.isNotEmpty()) {
            result.add(FontListItem.Header("Premium Fonts", isPremium = true))
            result.addAll(premium.map { FontListItem.Item(it) })
        }
        return result
    }

}