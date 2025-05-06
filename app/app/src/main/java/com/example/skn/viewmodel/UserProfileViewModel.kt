package com.example.skn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skn.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserProfileViewModel : ViewModel()  {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val userCollection = "user_profile"

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess = _updateSuccess.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Load user profile when ViewModel is created if a user is logged in
        // We don't want to trigger update success during initial load
        _updateSuccess.value = false
        auth.currentUser?.email?.let { fetchUser(it) }
    }

    // Get current user ID or null if not logged in
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.email
    }

    // Fetch user profile from Firestore
    private fun fetchUser(email: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val docRef = firestore.collection(userCollection).document(email)
                val document = docRef.get().await()

                if (document.exists()) {
                    val profile = document.toObject(UserProfile::class.java) ?: UserProfile(email = email)
                    _userProfile.value = profile
                } else {
                    val newProfile = UserProfile(uid = email)
                    _userProfile.value = newProfile
                    saveUserProfileWithoutSuccess(newProfile)
                }
            } catch (e: Exception) {
                _error.value = "Failed to load profile: ${e.localizedMessage}"
            } finally {
                _loading.value = false
            }
        }
    }


    // Save user profile to Firestore and signal update success (for completing flows)
    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                _updateSuccess.value = false // Reset success status before starting

                val email = getCurrentUserId()  // <- now returns email
                if (email == null) {
                    _error.value = "No user is logged in"
                    return@launch
                }

                // Save with correct uID
                val updatedProfile = profile.copy(email=email)

                // Save to DB
                firestore.collection(userCollection).document(email).set(updatedProfile).await()

                _userProfile.value = updatedProfile
                _updateSuccess.value = true  // Set updateSuccess to true after successful save
            } catch (e: Exception) {
                _error.value = "Failed to save profile: ${e.localizedMessage}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Save user profile without triggering update success (for initial profile creation)
    private fun saveUserProfileWithoutSuccess(profile: UserProfile) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val email = getCurrentUserId()
                if (email == null) {
                    _error.value = "No user is logged in"
                    return@launch
                }

                // Save with correct uID
                val updatedProfile = profile.copy(email=email)

                // Save to DB
                firestore.collection(userCollection).document(email).set(updatedProfile).await()

                _userProfile.value = updatedProfile
                // Don't set _updateSuccess to true - this is the key difference
            } catch (e: Exception) {
                _error.value = "Failed to save profile: ${e.localizedMessage}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Update specific profile fields
    fun updateProfile(
        firstName: String? = null,
        lastName: String? = null,
        skinType: String? = null,
        skinConcerns: List<String>? = null
    ) {
        val currentProfile = _userProfile.value
        if (currentProfile == null) {
            _error.value = "No profile loaded to update"
            return
        }

        val updatedProfile = currentProfile.copy(
            firstName = firstName ?: currentProfile.firstName,
            lastName = lastName ?: currentProfile.lastName,
            skinType = skinType ?: currentProfile.skinType,
            skinConcerns = skinConcerns ?: currentProfile.skinConcerns
        )

        saveUserProfile(updatedProfile)
    }

    fun setUser(user: FirebaseUser) {
        val email = user.email ?: return

        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val docRef = firestore.collection(userCollection).document(email)
                val document = docRef.get().await()

                if (document.exists()) {
                    val profile = document.toObject(UserProfile::class.java)
                    _userProfile.value = profile
                } else {
                    val newProfile = UserProfile(email = email, uid = user.uid)
                    firestore.collection(userCollection).document(email).set(newProfile).await()
                    _userProfile.value = newProfile
                }
            } catch (e: Exception) {
                _error.value = "Failed to load user: ${e.localizedMessage}"
            } finally {
                _loading.value = false
            }
        }
    }



    // Reset update success status - should be called after navigation completes
    fun resetUpdateStatus() {
        _updateSuccess.value = false
    }
}