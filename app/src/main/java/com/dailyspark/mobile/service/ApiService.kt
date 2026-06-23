package com.dailyspark.mobile.service

import com.dailyspark.mobile.model.QuoteEntity
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiService {
    @GET("rest/v1/quotes?select=*")
    suspend fun fetchQuotes(
        @Header("apikey") apiKey: String,
        @Header("Authorization") bearerToken: String
    ): List<QuoteEntity>
}