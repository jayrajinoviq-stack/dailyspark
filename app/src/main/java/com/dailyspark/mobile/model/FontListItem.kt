package com.dailyspark.mobile.model

sealed class FontListItem {
    data class Header(val title: String, val isPremium: Boolean = false) : FontListItem()
    data class Item(val font: FontOption) : FontListItem()
}