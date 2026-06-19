package com.example.dailyspark.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.dailyspark.model.FolderQuoteEntity
import com.example.dailyspark.model.FolderWithCount
import com.example.dailyspark.model.QuoteEntity
import com.example.dailyspark.repository.QuoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.logging.Logger

sealed class QuoteUiState {
    object Loading : QuoteUiState()
    data class Success(val quotes: List<QuoteEntity>) : QuoteUiState()
    object Empty : QuoteUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
class QuoteViewModel(private val repository: QuoteRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _category = MutableStateFlow("All")

    val currentCategory: StateFlow<String> = _category.asStateFlow()

    val uiState: Flow<QuoteUiState> =
        combine(_searchQuery, _category) { q, c -> q to c }
            .flatMapLatest { (q, c) -> repository.getFilteredQuotes(q, c) }
            .map { if (it.isEmpty()) QuoteUiState.Empty else QuoteUiState.Success(it) }
            .onStart { emit(QuoteUiState.Loading) }

    val favouriteUiState: Flow<QuoteUiState> =
        repository.favouriteQuotes
            .map { if (it.isEmpty()) QuoteUiState.Empty else QuoteUiState.Success(it) }
            .onStart { emit(QuoteUiState.Loading) }

    val folders: Flow<List<FolderWithCount>> = repository.folders

    fun onSearchQueryChanged(query: String) { _searchQuery.value = query }
    fun onCategoryChanged(category: String) { _category.value = category }

    fun toggleFavourite(id: Int) {
        viewModelScope.launch { repository.toggleFavourite(id) }
    }

    fun addNewFolder(name: String) {
        viewModelScope.launch { repository.createFolder(name) }
    }


    fun deleteFolder(folderId: Int) {
        viewModelScope.launch { repository.deleteFolder(folderId) }
    }

    fun addQuoteToFolder(folderId: Int, text: String, author: String, category: String) {
        viewModelScope.launch {
            val newQuote = FolderQuoteEntity(
                folderId = folderId,
                quote = text,
                author = author,
                category = category
            )
            repository.addQuoteToFolder(newQuote)
                .onFailure { Log.e("QuoteViewModel", "addQuoteToFolder failed: $it") }
        }
    }

    class Factory(private val repository: QuoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuoteViewModel(repository) as T
    }
}