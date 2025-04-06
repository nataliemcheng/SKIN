package com.example.skn.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.skn.userinterface.MainScreen
import com.example.skn.userinterface.ScanOrSearchScreen
import com.example.skn.userinterface.BarcodeScannerScreen
import com.example.skn.userinterface.LoginScreen
import com.example.skn.userinterface.SignUpScreen
import com.example.skn.viewmodel.AuthViewModel
import com.example.skn.viewmodel.ProductViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavGraph(
    navController: NavHostController,
    productViewModel: ProductViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    val startDestination = if (isLoggedIn) "main" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                navController = navController,
                onLoginSuccess = { navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                    launchSingleTop = true
                }}
            )
        }
        composable("signup") {
            SignUpScreen(
                authViewModel = authViewModel,
                onSignUpSuccess = {
                    navController.navigate("main") {
                        popUpTo("signup") { inclusive = true }
                        launchSingleTop = true

                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("main") {
            MainScreen(
                viewModel = productViewModel,
                authViewModel = authViewModel,
                onSearchClick = { navController.navigate("search") },
                onCreatePostClick = { /* your logic */ },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }  // so user can't go back
                        launchSingleTop = true

                    }
                }
            )
        }
        composable("search") {
            ScanOrSearchScreen(
                viewModel = productViewModel,
                onBackClick = { navController.popBackStack() },
                onCreatePostClick = { /* TODO: navigate to post */ },
                onScanClick = { navController.navigate("barcodeScanner") }
            )
        }
        composable("barcodeScanner") {
            BarcodeScannerScreen()
        }

    }
}