package com.dailyspark.mobile.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folder_quotes",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["folderId"])]
)

data class FolderQuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val folderId: Int,
    val quote: String,
    val category: String,
    val author: String
)