package com.dailyspark.mobile.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dailyspark.mobile.NetworkMonitor
import com.dailyspark.mobile.model.FolderQuoteEntity
import com.dailyspark.mobile.model.FolderWithCount
import com.dailyspark.mobile.model.QuoteEntity
import com.dailyspark.mobile.repository.QuoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class QuoteUiState {
    object Loading : QuoteUiState()
    data class Success(val quotes: List<QuoteEntity>) : QuoteUiState()
    object Empty : QuoteUiState()
}


sealed class SyncStatus {
    object Idle : SyncStatus()
    object Loading : SyncStatus()
    object Success : SyncStatus()
    object NoInternet : SyncStatus()
    data class Error(val msg: String) : SyncStatus()
}
//
//@OptIn(ExperimentalCoroutinesApi::class)
//class QuoteViewModel(
//    private val repository: QuoteRepository,
//    private val networkMonitor: NetworkMonitor
//) : ViewModel() {
//
//
//    init {
//        viewModelScope.launch { repository.syncDataIfNeeded() }
//        startSync()
//    }
//
//    private val _searchQuery = MutableStateFlow("")
//    private val _category = MutableStateFlow("All")
//
//    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
//    val syncStatus = _syncStatus.asStateFlow()
//
//    private var shuffledIdOrder: List<Int>? = null
//
//    val currentCategory: StateFlow<String> = _category.asStateFlow()
//
//    private val _toastEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
//    val toastEvent = _toastEvent.asSharedFlow()
//
//    private var cachedAllQuotes: List<QuoteEntity> = emptyList()
//
//    val uiState: StateFlow<QuoteUiState> =
//        combine(_searchQuery, _category) { q, c -> q to c }
//            .flatMapLatest { (q, c) -> repository.getFilteredQuotes(q, c) }
//            .map { quotes ->
//                if (quotes.isEmpty()) return@map QuoteUiState.Empty
//
//                val finalQuotes = if (_category.value == "All" && _searchQuery.value.isBlank()) {
//                    if (shuffledIdOrder == null || shuffledIdOrder?.size != quotes.size) {
//                        shuffledIdOrder = quotes.map { it.id }.shuffled()
//                    }
//                    val quoteMap = quotes.associateBy { it.id }
//                    shuffledIdOrder!!.mapNotNull { quoteMap[it] }
//                } else {
//                    shuffledIdOrder = null
//                    quotes
//                }
//
//                QuoteUiState.Success(finalQuotes)
//            }
//            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuoteUiState.Loading)
//
//
//    val favouriteUiState: StateFlow<QuoteUiState> =
//        repository.favouriteQuotes
//            .map { if (it.isEmpty()) QuoteUiState.Empty else QuoteUiState.Success(it) }
//            .stateIn(
//                scope = viewModelScope,
//                started = SharingStarted.WhileSubscribed(5000),
//                initialValue = QuoteUiState.Loading
//            )
//
//    val folders: Flow<List<FolderWithCount>> = repository.folders
//
//    fun onSearchQueryChanged(query: String) {
//        _searchQuery.value = query
//    }
//
//
//    fun onCategoryChanged(category: String) {
//
//        if (category == "All" &&
//            _category.value != "All"
//        ) {
//            cachedAllQuotes = emptyList()
//        }
//
//        _category.value = category
//    }
//
//    fun toggleFavourite(id: Int) {
//        viewModelScope.launch { repository.toggleFavourite(id) }
//    }
//
//    fun addNewFolder(name: String) {
//        viewModelScope.launch { repository.createFolder(name) }
//    }
//
//
//    fun startSync(isRetry: Boolean = false) {
//        viewModelScope.launch {
//            _syncStatus.value = SyncStatus.Loading
//
//            if (!networkMonitor.isConnected()) {
//                _syncStatus.value = SyncStatus.NoInternet
//                _toastEvent.emit("No Internet Connection")
//                return@launch
//            }
//
//            val result = repository.syncDataIfNeeded(forceRefresh = isRetry)
//
//            if (result.isSuccess) {
//                _syncStatus.value = SyncStatus.Success
//            } else {
//                _syncStatus.value = SyncStatus.Error("Failed to fetch data")
//            }
//        }
//    }
//
//
//    private val _quoteListState = MutableStateFlow<List<QuoteEntity>>(emptyList())
//
//    fun loadQuotesByIds(ids: List<Int>) {
//        viewModelScope.launch {
//            val quotes = repository.getQuotesByIds(ids)
//            val ordered = ids.mapNotNull { id -> quotes.find { it.id == id } }
//            _quoteListState.value = ordered
//        }
//    }
//
//    private val _quoteIds = MutableStateFlow<List<Int>>(emptyList())
//
//    val quoteListState: StateFlow<List<QuoteEntity>> =
//        _quoteIds
//            .flatMapLatest { ids ->
//                if (ids.isEmpty()) flowOf(emptyList())
//                else repository.getQuotesByIdsFlow(ids)
//            }
//            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
//
//    fun setQuoteIds(ids: List<Int>) {
//        _quoteIds.value = ids
//    }
//
//
//    fun deleteFolder(folderId: Int) {
//        viewModelScope.launch { repository.deleteFolder(folderId) }
//    }
//
//    fun addQuoteToFolder(folderId: Int, text: String, author: String, category: String) {
//        viewModelScope.launch {
//            val newQuote = FolderQuoteEntity(
//                folderId = folderId,
//                quote = text,
//                author = author,
//                category = category
//            )
//            repository.addQuoteToFolder(newQuote)
//                .onFailure { Log.e("QuoteViewModel", "addQuoteToFolder failed: $it") }
//        }
//    }
//
//    class Factory(
//        private val repository: QuoteRepository,
//        private val networkMonitor: NetworkMonitor
//    ) : ViewModelProvider.Factory {
//
//        @Suppress("UNCHECKED_CAST")
//        override fun <T : ViewModel> create(modelClass: Class<T>): T {
//            return QuoteViewModel(repository, networkMonitor) as T
//        }
//    }
//}



@OptIn(ExperimentalCoroutinesApi::class)
class QuoteViewModel(
    private val repository: QuoteRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _category = MutableStateFlow("All")

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Loading)
    val syncStatus = _syncStatus.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastEvent = _toastEvent.asSharedFlow()

    private var shuffledIdOrder: List<Int>? = null

    val currentCategory: StateFlow<String> = _category.asStateFlow()

    private var cachedAllQuotes: List<QuoteEntity> = emptyList()

    val uiState: StateFlow<QuoteUiState> =
        combine(_searchQuery, _category) { q, c -> q to c }
            .flatMapLatest { (q, c) -> repository.getFilteredQuotes(q, c) }
            .map { quotes ->
                if (quotes.isEmpty()) return@map QuoteUiState.Empty

                val finalQuotes = if (_category.value == "All" && _searchQuery.value.isBlank()) {
                    if (shuffledIdOrder == null || shuffledIdOrder?.size != quotes.size) {
                        shuffledIdOrder = quotes.map { it.id }.shuffled()
                    }
                    val quoteMap = quotes.associateBy { it.id }
                    shuffledIdOrder!!.mapNotNull { quoteMap[it] }
                } else {
                    shuffledIdOrder = null
                    quotes
                }

                QuoteUiState.Success(finalQuotes)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), QuoteUiState.Loading)

    val favouriteUiState: StateFlow<QuoteUiState> =
        repository.favouriteQuotes
            .map { if (it.isEmpty()) QuoteUiState.Empty else QuoteUiState.Success(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = QuoteUiState.Loading
            )

    val folders: Flow<List<FolderWithCount>> = repository.folders

    private val _quoteListState = MutableStateFlow<List<QuoteEntity>>(emptyList())

    private val _quoteIds = MutableStateFlow<List<Int>>(emptyList())

    val quoteListState: StateFlow<List<QuoteEntity>> =
        _quoteIds
            .flatMapLatest { ids ->
                if (ids.isEmpty()) flowOf(emptyList())
                else repository.getQuotesByIdsFlow(ids)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        startSync()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryChanged(category: String) {
        if (category == "All" && _category.value != "All") {
            cachedAllQuotes = emptyList()
        }
        _category.value = category
    }

    fun toggleFavourite(id: Int) {
        viewModelScope.launch { repository.toggleFavourite(id) }
    }

    fun addNewFolder(name: String) {
        viewModelScope.launch { repository.createFolder(name) }
    }

    fun startSync(isRetry: Boolean = false) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Loading

            if (!networkMonitor.isConnected()) {
                _syncStatus.value = SyncStatus.NoInternet
                _toastEvent.emit("No Internet Connection")
                return@launch
            }

            val result = repository.syncDataIfNeeded(forceRefresh = isRetry)

            if (result.isSuccess) {
                _syncStatus.value = SyncStatus.Success
            } else {
                _syncStatus.value = SyncStatus.Error("Failed to fetch data")
            }
        }
    }

    fun loadQuotesByIds(ids: List<Int>) {
        viewModelScope.launch {
            val quotes = repository.getQuotesByIds(ids)
            val ordered = ids.mapNotNull { id -> quotes.find { it.id == id } }
            _quoteListState.value = ordered
        }
    }

    fun setQuoteIds(ids: List<Int>) {
        _quoteIds.value = ids
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

    class Factory(
        private val repository: QuoteRepository,
        private val networkMonitor: NetworkMonitor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QuoteViewModel(repository, networkMonitor) as T
        }
    }
}