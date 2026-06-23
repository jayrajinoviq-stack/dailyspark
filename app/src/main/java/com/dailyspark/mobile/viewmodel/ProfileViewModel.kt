package com.dailyspark.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dailyspark.mobile.repository.QuoteRepository
import com.dailyspark.mobile.repository.StatsRepository
import com.dailyspark.mobile.repository.StreakRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(
    private val quoteRepository: QuoteRepository,
    private val streakRepository: StreakRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    val totalSharedCount: StateFlow<Int> = statsRepository.shareCount

    val totalSavedCount: StateFlow<Int> = quoteRepository.favouriteQuotes
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun getStreakCount(): Int = streakRepository.getStreakCount()

    class Factory(
        private val quoteRepo: QuoteRepository,
        private val streakRepo: StreakRepository,
        private val statsRepo: StatsRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(quoteRepo, streakRepo, statsRepo) as T
        }
    }
}