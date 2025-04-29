package com.example.skn.api

import retrofit2.Response
import retrofit2.http.*
import com.example.skn.model.ChemicalSearchResponse

interface ChemicalsApiService {
    @GET("api/3/action/datastore_search")
    suspend fun searchChemicals(
        @Query("resource_id") resourceId: String,
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int? = null
    ): Response<ChemicalSearchResponse>
}