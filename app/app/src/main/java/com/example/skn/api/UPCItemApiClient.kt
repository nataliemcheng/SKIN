package com.example.skn.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object UPCItemApiClient {
    private const val BASE_URL = "https://api.upcitemdb.com/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())  // ✅ this line is essential
        .build()

    val api: UPCItemApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))  // ✅ pass in moshi
            .build()
            .create(UPCItemApiService::class.java)
    }
}
