package com.example.dailyspark.model

import com.example.dailyspark.viewmodel.HomeViewModel

data class HomeUiState(
    val selectedCategory: String = "All",
    val currentQuote: QuoteEntity? = null,
    val categoryQuotes: List<QuoteEntity> = emptyList(),
    val streakState: HomeViewModel.StreakUiState = HomeViewModel.StreakUiState()
)