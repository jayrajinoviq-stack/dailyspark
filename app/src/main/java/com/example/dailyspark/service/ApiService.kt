package com.example.dailyspark.service

import com.example.dailyspark.model.QuoteEntity
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiService {
    @GET("rest/v1/quotes?select=*")
    suspend fun fetchQuotes(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String
    ): List<QuoteEntity>
}