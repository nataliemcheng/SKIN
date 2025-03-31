package com.example.skn.api

import retrofit2.Response
import retrofit2.http.*


interface MakeupApiService {
    @GET("products.json")
    suspend fun getAllProducts(): Response<List<Product>>

    @GET("products.json")
    suspend fun getProductsByBrand(@Query("brand") brand: String): Response<List<Product>>

    @GET("products.json")
    suspend fun getProductsByType(@Query("product_type") productType: String): Response<List<Product>>

    @GET("products.json")
    suspend fun searchProducts(
        @Query("brand") brand: String?,
        @Query("product_type") productType: String?,
        @Query("product_category") productCategory: String?,
        @Query("product_tags") productTags: String?,
        @Query("price_greater_than") priceGreaterThan: Float?,
        @Query("price_less_than") priceLessThan: Float?,
        @Query("rating_greater_than") ratingGreaterThan: Float?,
        @Query("rating_less_than") ratingLessThan: Float?
    ): Response<List<Product>>
}