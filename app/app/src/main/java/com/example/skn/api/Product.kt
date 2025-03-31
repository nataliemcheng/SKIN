package com.example.skn.api

data class Product(
    val id: Int,
    val brand: String?,
    val name: String?,
    val price: String?,
    val price_sign: String?,
    val currency: String?,
    val image_link: String?,
    val product_link: String?,
    val website_link: String?,
    val description: String?,
    val rating: Float?,
    val category: String?,
    val product_type: String?,
    val tag_list: List<String>?,
    val created_at: String?,
    val updated_at: String?,
    val product_api_url: String?,
    val api_featured_image: String?
)
