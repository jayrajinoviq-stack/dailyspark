package com.example.dailyspark.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable

    @Entity(tableName = "quotes_table")
    data class QuoteEntity(
        @PrimaryKey
        @SerializedName("id")       val id: Int,
        @SerializedName("quote")    val quote: String,
        @SerializedName("category") val category: String,
        @SerializedName("author")   val author: String,

        var isFavourite: Boolean = false
    ): Serializable