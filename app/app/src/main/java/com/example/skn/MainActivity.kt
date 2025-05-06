package com.example.skn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.skn.navigation.AppNavGraph
import com.example.skn.viewmodel.AuthViewModel
import com.example.skn.viewmodel.AuthViewModelFactory
import com.example.skn.viewmodel.ProductViewModel
import com.example.skn.viewmodel.UserProfileViewModel
import com.example.skn.viewmodel.ChemicalsViewModel
import com.google.firebase.FirebaseApp
import com.example.skn.ui.theme.SKNTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            SKNTheme{
                val navController = rememberNavController()

                val userProfileViewModel: UserProfileViewModel = viewModel()
                val productViewModel: ProductViewModel = viewModel()
                val chemicalsViewModel: ChemicalsViewModel = viewModel()

                // Use the factory to create AuthViewModel
                val authViewModelFactory = AuthViewModelFactory(userProfileViewModel)
                val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)

                AppNavGraph(
                    navController = navController,
                    productViewModel = productViewModel,
                    authViewModel = authViewModel,
                    userProfileViewModel = userProfileViewModel,
                    chemicalsViewModel = chemicalsViewModel
                )
            }
        }
    }
}