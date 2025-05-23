package com.example.skn.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val skinType: String? = "",
    val skinConcerns: List<String>? = emptyList()
)
