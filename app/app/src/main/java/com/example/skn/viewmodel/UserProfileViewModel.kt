package com.example.skn.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skn.api.UserProfile
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserProfileViewModel : ViewModel()  {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage.reference

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // Load user profile when ViewModel is created if a user is logged in
        auth.currentUser?.let { user ->
            fetchUser(user.uid)
        }
    }

    // CRUD USER
    fun fetchUser(userId: String? = null) {
        val uid = userId?: auth.currentUser?.uid
        if (uid == null) {
            _error.value = "No user is currently logged in"
            return
        }

        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val docRef = firestore.collection("users").document(uid)
                val document = docRef.get().await()

                if (document.exists()) {
                    // Convert document to UserProfile
                    val profile = document.toObject(UserProfile::class.java)?: UserProfile(uid = uid)
                    _userProfile.value = profile

                } else {
                    // Create a new user profile if DNE
                    val newProfile = UserProfile(uid = uid)
                    _userProfile.value = newProfile
                    saveUserProfile(newProfile)
                }
            } catch (e: Exception) {
                Log.e("UserProfile:", "Error fetching user profile", e)
                _error.value = "${e.localizedMessage}"
            }finally {
                _loading.value = false
            }
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val uid = auth.currentUser?.uid
                if (uid == null) {
                    _error.value = "No user is logged in"
                    return@launch
                }

                // Save with correct uID
                val updatedProfile = profile.copy(uid=uid)

                // Save to DB
                firestore.collection("users").document(uid).set(updatedProfile).await()

                _userProfile.value = updatedProfile
                Log.d("UserProfile:","User Profile saved successfully")
            } catch (e: Exception) {
                Log.e("UserProfile:","Error saving user profile", e)
                _error.value = "Failed to save profile: ${e.localizedMessage}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateProfile(
        email: String? = null,
        firstName: String? = null,
        lastName: String? = null,
        skinType: String? = null,
        skinConcerns: List<String>? = null
    ) {
        val currentProfile = _userProfile.value ?: return

        val updatedProfile = currentProfile.copy(
            email = email ?: currentProfile.email,
            firstName = firstName ?: currentProfile.firstName,
            lastName = lastName ?: currentProfile.lastName,
            skinType = skinType ?: currentProfile.skinType,
            skinConcerns = skinConcerns ?: currentProfile.skinConcerns
        )

        saveUserProfile(updatedProfile)
    }

    // Delete user profile
    fun deleteUserProfile(onComplete: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onComplete(false, "No user is logged in")
            return
        }

        firestore.collection("users").document(uid)
            .delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _userProfile.value = UserProfile(uid = uid)
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.localizedMessage)
                }
            }
    }
}