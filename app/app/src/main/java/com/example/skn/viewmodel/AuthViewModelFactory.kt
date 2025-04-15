package com.example.skn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AuthViewModelFactory(
    private val userProfileViewModel: UserProfileViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(userProfileViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}