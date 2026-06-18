package com.example.dailyspark.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dailyspark.model.QuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {

    @Query("SELECT * FROM quotes_table")
    fun getAllQuotes(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes_table WHERE isFavourite = 1")
    fun getFavouriteQuotes(): Flow<List<QuoteEntity>>

    @Query(
        """
        SELECT * FROM quotes_table
        WHERE quote    LIKE '%' || :query || '%'
           OR author   LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
    """
    )
    fun searchQuotes(query: String): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes_table WHERE LOWER(category) = LOWER(:category)")
    fun getQuotesByCategory(category: String): Flow<List<QuoteEntity>>

    @Query(
        """
        SELECT * FROM quotes_table
        WHERE (quote  LIKE '%' || :query || '%'
            OR author LIKE '%' || :query || '%')
          AND LOWER(category) = LOWER(:category)
    """
    )
    fun searchAndFilterQuotes(query: String, category: String): Flow<List<QuoteEntity>>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuotes(quotes: List<QuoteEntity>)

    @Query(
        """
        UPDATE quotes_table
        SET quote = :quote, author = :author, category = :category
        WHERE id = :id
    """
    )
    suspend fun updateQuoteContent(id: Int, quote: String, author: String, category: String)


    @Query("UPDATE quotes_table SET isFavourite = 1 - isFavourite WHERE id = :id")
    suspend fun toggleFavourite(id: Int)

    suspend fun upsertQuotes(quotes: List<QuoteEntity>) {
        insertQuotes(quotes)
        quotes.forEach { q ->
            updateQuoteContent(q.id, q.quote, q.author, q.category)
        }
    }

    @Query("DELETE FROM quotes_table")
    suspend fun deleteAllQuotes(): Int

    @Query("SELECT COUNT(*) FROM quotes_table")
    suspend fun getCount(): Int
}