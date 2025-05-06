package com.example.skn.viewmodel

import androidx.lifecycle.ViewModel
import com.example.skn.model.UserProfile
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow

class AuthViewModel(private val userProfileViewModel: UserProfileViewModel) : ViewModel() {

    private val auth = Firebase.auth

    private val _currentUser = MutableStateFlow(auth.currentUser)

    init {
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
                        onResult(true, uid)
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
                        onResult(false, "User ID is null")
                    }
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Unknown error occurred"
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
                    val errorMsg = task.exception?.localizedMessage ?: "Login failed"
                    onResult(false, errorMsg)
                }
            }
    }

    fun logout() {
        auth.signOut()
    }

    fun changePassword(currentPassword: String, newPassword: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, "No user is currently signed in")
            return
        }

        val email = user.email
        if (email == null) {
            onResult(false, "User email is not available")
            return
        }

        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                onResult(true, null)
                            } else {
                                val errorMsg = updateTask.exception?.localizedMessage ?: "Failed to update password"
                                onResult(false, errorMsg)
                            }
                        }
                } else {
                    val errorMsg = reauthTask.exception?.localizedMessage ?: "Re-authentication failed"
                    onResult(false, errorMsg)
                }
            }
    }

}
