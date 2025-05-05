package com.example.skn.model

data class Product(
    val id: Int = -1,
    val brand: String? = null,
    val name: String? = null,
    val price: String? = null,
    val price_sign: String? = null,
    val currency: String? = null,
    val image_link: String? = null,
    val product_link: String? = null,
    val website_link: String? = null,
    val description: String? = null,
    val ingredients: String? = null,
    val rating: Float? = null,
    val category: String? = null,
    val product_type: String? = null,
    val tag_list: List<String>? = emptyList(),
    val created_at: String? = null,
    val updated_at: String? = null,
    val product_api_url: String? = null,
    val api_featured_image: String? = null
)
