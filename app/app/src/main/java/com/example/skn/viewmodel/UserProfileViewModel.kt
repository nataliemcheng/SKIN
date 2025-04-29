package com.example.skn.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.skn.model.UserProfile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.update

class UserProfileViewModel : ViewModel()  {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage.reference
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
        auth.currentUser?.uid?.let{ fetchUser(it) }
    }

    // Get current user ID or null if not logged in
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Fetch user profile from Firestore
    fun fetchUser(userId: String) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null

                val docRef = firestore.collection(userCollection).document(userId)
                val document = docRef.get().await()

                if (document.exists()) {
                    // Create profile document
                    val profile = document.toObject(UserProfile::class.java) ?: UserProfile(uid = userId)
                    _userProfile.value = profile
                } else {
                    // Create a new profile with default fields if not found
                    val newProfile = UserProfile(uid = userId)
                    _userProfile.value = newProfile
                    // Save the new profile but don't mark as update success
                    // This prevents auto-navigation during initial profile creation
                    saveUserProfileWithoutSuccess(newProfile)
                }
            } catch (e: Exception) {
                Log.e("UserProfile:", "Error fetching user profile", e)
                _error.value = "Failed to load profile: ${e.localizedMessage}"
            } finally {
                _loading.value = false
            }
        }
    }

    // Set user and load their profile
    fun setUser(user: FirebaseUser) {
        fetchUser(user.uid) // Load profile from Firestore
        _userProfile.update { current ->
            current?.copy(email = user.email ?: "") ?: UserProfile(
                uid = user.uid,
                email = user.email ?: "",
                firstName = "",
                lastName = ""
            )
        }
    }

    // Save user profile to Firestore and signal update success (for completing flows)
    fun saveUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                _updateSuccess.value = false // Reset success status before starting

                val uid = getCurrentUserId()
                if (uid == null) {
                    _error.value = "No user is logged in"
                    return@launch
                }

                // Save with correct uID
                val updatedProfile = profile.copy(uid=uid)

                // Save to DB
                firestore.collection(userCollection).document(uid).set(updatedProfile).await()

                _userProfile.value = updatedProfile
                _updateSuccess.value = true  // Set updateSuccess to true after successful save
                Log.d("UserProfile:", "User Profile saved successfully")
            } catch (e: Exception) {
                Log.e("UserProfile:", "Error saving user profile", e)
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

                val uid = getCurrentUserId()
                if (uid == null) {
                    _error.value = "No user is logged in"
                    return@launch
                }

                // Save with correct uID
                val updatedProfile = profile.copy(uid=uid)

                // Save to DB
                firestore.collection(userCollection).document(uid).set(updatedProfile).await()

                _userProfile.value = updatedProfile
                // Don't set _updateSuccess to true - this is the key difference
                Log.d("UserProfile:", "Initial user profile created (no navigation)")
            } catch (e: Exception) {
                Log.e("UserProfile:", "Error saving initial user profile", e)
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

    // Delete user profile
    fun deleteUserProfile(onComplete: (Boolean, String?) -> Unit) {
        val uid = getCurrentUserId()
        if (uid == null) {
            onComplete(false, "No user is logged in")
            return
        }

        firestore.collection(userCollection).document(uid)
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

    // Reset update success status - should be called after navigation completes
    fun resetUpdateStatus() {
        _updateSuccess.value = false
    }
}