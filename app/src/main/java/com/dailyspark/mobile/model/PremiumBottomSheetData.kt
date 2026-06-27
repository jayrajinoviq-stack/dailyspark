package com.dailyspark.mobile.model

data class PremiumBottomSheetData(
    val icon: Int,
    val title: String,
    val description: String,
    val features: List<String>,
    val primaryButton: String,
    val secondaryButton: String
)