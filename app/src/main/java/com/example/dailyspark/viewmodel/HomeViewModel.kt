package com.example.dailyspark.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.example.dailyspark.repository.StreakRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import androidx.lifecycle.viewModelScope
import com.example.dailyspark.model.QuoteEntity
import com.example.dailyspark.repository.QuoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class HomeViewModel(
    private val streakRepository: StreakRepository,
    private val quoteRepository: QuoteRepository
) : ViewModel() {

    sealed class DayStatus {
        object Completed : DayStatus()
        object Active : DayStatus()
        object Normal : DayStatus()
    }

    data class StreakUiState(
        val count: Int = 0,
        val dayStatuses: List<DayStatus> = List(7) { DayStatus.Normal },
        val greeting: String = ""
    )

    private val _streakState = MutableStateFlow(StreakUiState())
    val streakState: StateFlow<StreakUiState> = _streakState.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val categoryQuotes = _selectedCategory
        .flatMapLatest { category -> quoteRepository.getFilteredQuotes("", category) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentQuote = MutableStateFlow<QuoteEntity?>(null)
    val currentQuote: StateFlow<QuoteEntity?> = _currentQuote.asStateFlow()

    init {
        viewModelScope.launch {
            categoryQuotes.collect { quotes ->
                if (quotes.isNotEmpty() && _currentQuote.value == null) {
                    showRandomQuote()
                }
            }
        }
    }

    fun onCategoryChanged(category: String) {
        _selectedCategory.value = category
        viewModelScope.launch {
            val quotes = categoryQuotes.filter { it.isNotEmpty() }.first()
            _currentQuote.value = quotes.random()
        }
    }

    fun showRandomQuote() {
        val quotes = categoryQuotes.value
        if (quotes.isNotEmpty()) {
            _currentQuote.value = quotes.random()
        }
    }

    fun toggleFavourite(id: Int) {
        viewModelScope.launch {
            quoteRepository.toggleFavourite(id)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateStreak() {
        val today = LocalDate.now()
        val lastDateStr = streakRepository.getLastDate()
        var currentCount = streakRepository.getStreakCount()

        if (lastDateStr != null) {
            val lastDate = LocalDate.parse(lastDateStr)
            when {
                lastDate.isEqual(today) -> {}
                lastDate.isEqual(today.minusDays(1)) -> {
                    currentCount++
                    streakRepository.saveStreak(currentCount, today.toString())
                }
                else -> {
                    currentCount = 1
                    streakRepository.saveStreak(currentCount, today.toString())
                }
            }
        } else {
            currentCount = 1
            streakRepository.saveStreak(currentCount, today.toString())
        }

        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val statuses = (0..6).map { i ->
            val dateToCheck = monday.plusDays(i.toLong())
            when {
                dateToCheck.isEqual(today) -> DayStatus.Active
                dateToCheck.isBefore(today) && (today.toEpochDay() - dateToCheck.toEpochDay() < currentCount) -> DayStatus.Completed
                else -> DayStatus.Normal
            }
        }

        _streakState.value = StreakUiState(
            count = currentCount,
            dayStatuses = statuses,
            greeting = getGreetingMessage(LocalTime.now().hour)
        )
    }

    private fun getGreetingMessage(hour: Int): String = when (hour) {
        in 5..11 -> "Good morning ☕"
        in 12..16 -> "Good afternoon ☀️"
        in 17..20 -> "Good evening ⛅"
        else -> "Good night 🌙"
    }
}