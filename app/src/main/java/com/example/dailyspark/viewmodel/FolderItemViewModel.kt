package com.example.dailyspark.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.dailyspark.model.FolderQuoteEntity
import com.example.dailyspark.repository.QuoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class FolderItemEvent {
    data class Error(val message: String) : FolderItemEvent()
    object Added : FolderItemEvent()
    object Updated : FolderItemEvent()
    object Deleted : FolderItemEvent()
    object FolderDeleted : FolderItemEvent()
}

class FolderItemViewModel(
    private val repository: QuoteRepository,
    private val folderId: Int
) : ViewModel() {

    companion object {
        const val MAX_ITEMS_PER_FOLDER = 10
    }

    val quotes: StateFlow<List<FolderQuoteEntity>> =
        repository.getQuotesInFolder(folderId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _events = MutableLiveData<FolderItemEvent>()
    val events: LiveData<FolderItemEvent> get() = _events

    fun canAddMore(): Boolean = (quotes.value?.size ?: 0) < MAX_ITEMS_PER_FOLDER

    fun addQuote(quote: String, author: String, category: String) {
        if (quote.isBlank()) {
            _events.value = FolderItemEvent.Error("Quote text can't be empty")
            return
        }
        viewModelScope.launch {
            val entity = FolderQuoteEntity(
                folderId = folderId,
                quote = quote.trim(),
                author = author.trim().ifBlank { "Unknown" },
                category = category.trim().ifBlank { "General" }
            )
            repository.addQuoteToFolder(entity)
                .onSuccess { _events.value = FolderItemEvent.Added }
                .onFailure { _events.value = FolderItemEvent.Error(it.message ?: "Failed to add") }
        }
    }

    fun updateQuote(entity: FolderQuoteEntity, quote: String, author: String, category: String) {
        if (quote.isBlank()) {
            _events.value = FolderItemEvent.Error("Quote text can't be empty")
            return
        }
        viewModelScope.launch {
            repository.updateQuoteInFolder(
                entity.copy(
                    quote = quote.trim(),
                    author = author.trim().ifBlank { "Unknown" },
                    category = category.trim().ifBlank { "General" }
                )
            )
            _events.value = FolderItemEvent.Updated
        }
    }

    fun deleteQuote(entity: FolderQuoteEntity) {
        viewModelScope.launch {
            repository.deleteQuoteFromFolder(entity)
            _events.value = FolderItemEvent.Deleted
        }
    }

    fun deleteFolder() {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            _events.value = FolderItemEvent.FolderDeleted
        }
    }

    class Factory(
        private val repository: QuoteRepository,
        private val folderId: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FolderItemViewModel::class.java)) {
                return FolderItemViewModel(repository, folderId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}