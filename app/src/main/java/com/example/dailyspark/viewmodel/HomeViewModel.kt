package com.example.dailyspark.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyspark.model.QuoteEntity
import com.example.dailyspark.repository.QuoteRepository
import com.example.dailyspark.repository.StreakRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters


class HomeViewModel(
    private val streakRepository: StreakRepository,
    private val quoteRepository: QuoteRepository
) : ViewModel() {

    data class UiState(
        val selectedCategory: String = "All",
        val currentQuote: QuoteEntity? = null,
        val categoryQuotes: List<QuoteEntity> = emptyList(),
        val streakState: StreakUiState = StreakUiState(),
        val isLoading: Boolean = true
    )

    data class StreakUiState(
        val count: Int = 0,
        val dayStatuses: List<DayStatus> = List(7) { DayStatus.Normal },
        val greeting: String = ""
    )

    sealed class DayStatus {
        object Completed : DayStatus()
        object Active    : DayStatus()
        object Normal    : DayStatus()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val quoteCache = mutableMapOf<String, List<QuoteEntity>>()

    init {
        loadQuotesForCategory("All")
    }

    fun setCategory(category: String) {
        if (_uiState.value.selectedCategory == category &&
            _uiState.value.currentQuote != null) return
        _uiState.update { it.copy(selectedCategory = category, isLoading = true) }

        val cached = quoteCache[category]
        if (cached != null) {
            _uiState.update {
                it.copy(currentQuote = cached.random(), categoryQuotes = cached, isLoading = false)
            }
        } else {
            loadQuotesForCategory(category)
        }
    }

    fun showNextQuote() {
        val category = _uiState.value.selectedCategory
        quoteCache[category]?.let { quotes ->
            if (quotes.isEmpty()) return

            val current = _uiState.value.currentQuote
            val next = if (quotes.size > 1) quotes.filter { it.id != current?.id }.random()
            else quotes.first()
            _uiState.update { it.copy(currentQuote = next) }
        }
    }

//    fun toggleFavourite(id: Int) {
//        viewModelScope.launch(Dispatchers.IO) {
//            quoteRepository.toggleFavourite(id)
//        }
//    }

    fun toggleFavourite(id: Int) {
        val current = _uiState.value.currentQuote ?: return

        if (current.id == id) {
            val updatedQuote = current.copy(isFavourite = !current.isFavourite)
            _uiState.update { it.copy(currentQuote = updatedQuote) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            quoteRepository.toggleFavourite(id)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateStreak() {
        viewModelScope.launch(Dispatchers.IO) {
            val today        = LocalDate.now()
            val lastDateStr  = streakRepository.getLastDate()
            var count        = streakRepository.getStreakCount()

            if (lastDateStr != null) {
                val lastDate = LocalDate.parse(lastDateStr)
                when {
                    lastDate.isEqual(today)                  -> { /* already updated today */ }
                    lastDate.isEqual(today.minusDays(1))     -> { count++; streakRepository.saveStreak(count, today.toString()) }
                    else                                     -> { count = 1; streakRepository.saveStreak(count, today.toString()) }
                }
            } else {
                count = 1
                streakRepository.saveStreak(count, today.toString())
            }

            val monday   = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val statuses = (0..6).map { i ->
                val d = monday.plusDays(i.toLong())
                when {
                    d.isEqual(today)    -> DayStatus.Active
                    d.isBefore(today) && (today.toEpochDay() - d.toEpochDay() < count) -> DayStatus.Completed
                    else                -> DayStatus.Normal
                }
            }

            _uiState.update {
                it.copy(streakState = StreakUiState(
                    count      = count,
                    dayStatuses = statuses,
                    greeting   = getGreeting(LocalTime.now().hour)
                ))
            }
        }
    }


    private fun loadQuotesForCategory(category: String) {
        viewModelScope.launch {
            quoteRepository.getFilteredQuotes("", category)
                .collect { quotes ->
                    if (quotes.isEmpty()) return@collect

                    quoteCache[category] = quotes

                    if (_uiState.value.selectedCategory == category) {
                        _uiState.update { state ->
                            val updatedCurrentQuote = quotes.find { it.id == state.currentQuote?.id }
                                ?: quotes.random()

                            state.copy(
                                currentQuote = updatedCurrentQuote,
                                categoryQuotes = quotes,
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }

    private fun getGreeting(hour: Int) = when (hour) {
        in 5..11  -> "Good morning ☕"
        in 12..16 -> "Good afternoon ☀️"
        in 17..20 -> "Good evening ⛅"
        else      -> "Good night 🌙"
    }
}