package com.dailyspark.mobile.repository

import android.content.Context
import android.util.Log
import com.dailyspark.mobile.data.dao.QuoteDao
import com.dailyspark.mobile.model.FolderEntity
import com.dailyspark.mobile.model.FolderQuoteEntity
import com.dailyspark.mobile.model.FolderWithCount
import com.dailyspark.mobile.model.QuoteEntity
import com.dailyspark.mobile.service.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

val TAG = "Repository"

class QuoteRepository(
    private val api: ApiService,
    private val dao: QuoteDao,
    context: Context
) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    val allQuotes: Flow<List<QuoteEntity>> = dao.getAllQuotes()
    val favouriteQuotes: Flow<List<QuoteEntity>> = dao.getFavouriteQuotes()

    private var shuffledIds: List<Int> = emptyList()
    fun getFilteredQuotes(query: String, category: String): Flow<List<QuoteEntity>> {
        val q = query.trim()
        return when {
            category == "All" && q.isBlank() -> dao.getAllQuotes()
            category == "All" && q.isNotBlank() -> dao.searchQuotes(q)
            category != "All" && q.isBlank() -> dao.getQuotesByCategory(category)
            else -> dao.searchAndFilterQuotes(q, category)
        }
    }

    suspend fun getQuotesByIds(ids: List<Int>): List<QuoteEntity> {
        return dao.getQuotesByIds(ids)
    }

    fun getQuotesByIdsFlow(ids: List<Int>): Flow<List<QuoteEntity>> {
        return dao.getQuotesByIdsFlow(ids)
    }


    suspend fun toggleFavourite(id: Int) = withContext(Dispatchers.IO) {
        dao.toggleFavourite(id)
    }

    suspend fun syncDataIfNeeded(forceRefresh: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        val lastSync = prefs.getLong("last_sync_time", 0L)
        val now = System.currentTimeMillis()
        val twentyFourHrs = 24 * 60 * 60 * 1000L
        val dbCount = dao.getCount()

        if (dbCount == 0 || (now - lastSync > twentyFourHrs) || forceRefresh) {
            try {
                val response = api.fetchQuotes(
                    "sb_publishable_k46r574RPC5drOcnKYw0pA_vKvzJZW1",
                    "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhpZmRseWtvbWFyaXd6cGhuZm9wIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc4MTEwMzQxNiwiZXhwIjoyMDk2Njc5NDE2fQ.6v3v0gMsWyyL6UnATCMqSnbHNMUoHRRMPQWMU0UgkrI"
                )
                if (response.isNotEmpty()) {
                    dao.upsertQuotes(response)
                    prefs.edit().putLong("last_sync_time", now).apply()
                    Log.d("SYNC", "Synced ${response.size} quotes")
                } else if (dbCount == 0) {
                    return@withContext Result.failure(Exception("No data available"))
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
        return@withContext Result.success(Unit)

    }

    val folders: Flow<List<FolderWithCount>> = dao.getFoldersWithCount()

    suspend fun createFolder(name: String) = withContext(Dispatchers.IO) {
        dao.insertFolder(FolderEntity(name = name))
    }

    fun getQuotesInFolder(folderId: Int) = dao.getQuotesByFolder(folderId)

    suspend fun addQuoteToFolder(quote: FolderQuoteEntity): Result<String> =
        withContext(Dispatchers.IO) {
            val currentCount = dao.getCountInFolder(quote.folderId)
            if (currentCount >= 10) {
                Result.failure(Exception("Limit reached: Max 10 quotes per folder"))
            } else {
                dao.insertQuoteToFolder(quote)
                Result.success("Added successfully")
            }
        }

    suspend fun updateQuoteInFolder(quote: FolderQuoteEntity) = withContext(Dispatchers.IO) {
        dao.updateFolderQuote(quote)
    }

    suspend fun deleteQuoteFromFolder(quote: FolderQuoteEntity) = withContext(Dispatchers.IO) {
        dao.deleteFolderQuote(quote)
    }

    suspend fun deleteFolder(folderId: Int) = withContext(Dispatchers.IO) {
        dao.deleteFolder(folderId)
    }


}