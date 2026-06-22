package com.example.dailyspark.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dailyspark.repository.QuoteRepository
import com.example.dailyspark.repository.StreakRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val quoteRepository: QuoteRepository,
    private val streakRepository: StreakRepository
) : ViewModel() {

    val totalSavedCount: StateFlow<Int> = quoteRepository.favouriteQuotes
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun getStreakCount(): Int {
        return streakRepository.getStreakCount()
    }

    class Factory(
        private val quoteRepo: QuoteRepository,
        private val streakRepo: StreakRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(quoteRepo, streakRepo) as T
        }
    }
}