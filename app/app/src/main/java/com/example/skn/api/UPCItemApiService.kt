package com.example.skn.api

import retrofit2.http.GET
import retrofit2.http.Query
import com.example.skn.model.UPCItemResponse

interface UPCItemApiService {
    @GET("prod/trial/lookup")
    suspend fun lookupUPC(
        @Query("upc") upc: String
    ): UPCItemResponse
}