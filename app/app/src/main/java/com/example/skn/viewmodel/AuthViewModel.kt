package com.example.skn.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AuthViewModel : ViewModel() {

    private val auth = Firebase.auth

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        Log.d("Auth", "✅ Registered: $uid")
                        onResult(true, uid) // success
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

}