package com.example.skn.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UPCItemResponse(
    val code: String?,
    val items: List<UPCItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UPCItem(
    val title: String?,
    val brand: String?,
    val description: String?,
    val images: List<String>?
)