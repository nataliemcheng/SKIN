package com.example.skn.model


data class UPCItemResponse(
    val code: String?,
    val items: List<UPCItem> = emptyList()
)

data class UPCItem(
    val title: String?,
    val brand: String?,
    val description: String?,
    val images: List<String>?
)