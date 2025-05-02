package com.example.skn.api

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object UPCItemApiClient {
    private const val BASE_URL = "https://api.upcitemdb.com/"

    val api: UPCItemApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(UPCItemApiService::class.java)
    }
}