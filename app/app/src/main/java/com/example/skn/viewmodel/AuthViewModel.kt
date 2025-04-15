package com.example.skn.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.skn.api.UserProfile

class AuthViewModel(private val userProfileViewModel: UserProfileViewModel) : ViewModel() {

    private val auth = Firebase.auth

    // Provides the current user (for UI)
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        // Update the current user when the auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        Log.d("Auth", "✅ Registered: $uid")
                        onResult(true, uid) // success
                        // After success, initialize the user profile
                        userProfileViewModel.saveUserProfile(
                            UserProfile(
                                uid = uid,
                                email = email,
                                firstName = "",
                                lastName = "",
                                skinType = "",
                                skinConcerns = listOf()
                            )
                        )
                    } else {
                        Log.e("Auth", "❌ UID is null after registration")
                        onResult(false, "User ID is null")
                    }
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Unknown error occurred"
                    Log.e("Auth", "❌ Registration failed: $errorMsg", task.exception)
                    onResult(false, errorMsg)
                }
            }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    onResult(true, uid)
                } else {
                    // Just show the built-in Firebase error message
                    val errorMsg = task.exception?.localizedMessage ?: "Login failed"
                    onResult(false, errorMsg)
                }
            }
    }

    fun logout() {
        auth.signOut()
        Log.d("Auth", "👋 Logged out")
    }

    fun changePassword(currentPassword: String, newPassword: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) { // This shouldn't be an issue, this page will only show if user is signed in
            onResult(false, "No user is currently signed in")
            return
        }

        val email = user.email
        if (email == null) { // This shouldn't be an issue, this page will only show if user is signed in
            onResult(false, "User email is not available")
            return
        }

        // Re-authenticate user before changing password
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    // User re-authenticated successfully, now change password
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Log.d("Auth", "Password updated successfully")
                                onResult(true, null)
                            } else { // Send error message
                                val errorMsg = updateTask.exception?.localizedMessage ?: "Failed to update password"
                                Log.e("Auth", "Password update failed: $errorMsg", updateTask.exception)
                                onResult(false, errorMsg)
                            }
                        }
                } else { // Send error message
                    val errorMsg = reauthTask.exception?.localizedMessage ?: "Re-authentication failed"
                    Log.e("Auth", "Re-authentication failed: $errorMsg", reauthTask.exception)
                    onResult(false, errorMsg)
                }
            }
    }

    // Clean up when ViewModel is destroyed
    override fun onCleared() {
        super.onCleared()
    }
}