package com.example.skn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.skn.navigation.AppNavGraph
import com.example.skn.viewmodel.AuthViewModel
import com.example.skn.viewmodel.ProductViewModel
import com.example.skn.viewmodel.UserProfileViewModel
import com.google.firebase.FirebaseApp


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        setContent {
            val productViewModel: ProductViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            val navController = rememberNavController()
            val userProfileViewModel: UserProfileViewModel = viewModel()

            AppNavGraph(
                navController = navController,
                productViewModel = productViewModel,
                authViewModel = authViewModel,
                userProfileViewModel = userProfileViewModel
            )
        }
    }
}