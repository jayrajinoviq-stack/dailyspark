package com.example.dailyspark.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dailyspark.data.dao.QuoteDao
import com.example.dailyspark.model.FolderEntity
import com.example.dailyspark.model.FolderQuoteEntity
import com.example.dailyspark.model.QuoteEntity

object AppConstants {
    const val DATABASE_NAME = "quotes_database"
}

@Database(
    entities = [
        QuoteEntity::class,
        FolderEntity::class,
        FolderQuoteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    AppConstants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
