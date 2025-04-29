package com.example.skn.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.skn.userinterface.MainScreen
import com.example.skn.userinterface.ScanOrSearchScreen
import com.example.skn.userinterface.BarcodeScannerScreen
import com.example.skn.userinterface.LoginScreen
import com.example.skn.userinterface.OnBoardingScreen
import com.example.skn.userinterface.SignUpScreen
import com.example.skn.userinterface.UserProfileScreen
import com.example.skn.viewmodel.AuthViewModel
import com.example.skn.viewmodel.ProductViewModel
import com.example.skn.viewmodel.UserProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavGraph(
    navController: NavHostController,
    productViewModel: ProductViewModel,
    authViewModel: AuthViewModel,
    userProfileViewModel: UserProfileViewModel,
    modifier: Modifier = Modifier
) {
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    val startDestination = if (isLoggedIn) "main" else "login"
    val snackbarHostState = remember { SnackbarHostState() }

    // Common navigation handlers to be reused
    val navigateToHome = {
        navController.navigate("main") {
            popUpTo("main") { inclusive = true }
            launchSingleTop = true
        }
    }

    val navigateToSearch = {
        navController.navigate("scanOrSearch?barcode=")
    }

    val navigateToScan = {
        navController.navigate("barcodeScanner")
    }

    val navigateToProfile = {
        navController.navigate("profile")
    }

    val navigateToCreatePost = {
        // TODO: Implement your create post screen navigation
    }

    val performLogout = {
        navController.navigate("login") {
            popUpTo("main") { inclusive = true }  // so user can't go back
            launchSingleTop = true
        }
    }

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
                    navController.navigate("onboarding") {
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

        composable("onboarding") {
            OnBoardingScreen(
                authViewModel = authViewModel,
                profileViewModel = userProfileViewModel,
                onFinish = {
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("main") {
            MainScreen(
                viewModel = productViewModel,
                authViewModel = authViewModel,
                onSearchClick = navigateToSearch,
                onScanClick = navigateToScan,
                onCreatePostClick = navigateToCreatePost,
                onLogout = performLogout,
                onProfileClick = navigateToProfile
            )
        }

        composable(
            "scanOrSearch?barcode={barcode}",
            arguments = listOf(navArgument("barcode") { defaultValue = "" })
        ) { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode")

            ScanOrSearchScreen(
                viewModel = productViewModel,
                onBackClick = { navController.popBackStack() },
                onCreatePostClick = navigateToCreatePost,
                onScanClick = navigateToScan,
                onHomeClick = navigateToHome,
                onProfileClick = navigateToProfile,
                snackbarHostState = snackbarHostState,
                scannedBarcode = barcode.takeIf { !it.isNullOrBlank() }
            )
        }

        composable("barcodeScanner") {
            BarcodeScannerScreen(
                navController = navController,
                onScanComplete = { result ->
                    navController.navigate("scanOrSearch?barcode=${Uri.encode(result)}") {
                        popUpTo("barcodeScanner") { inclusive = true }
                    }
                },
                snackbarHostState = snackbarHostState,
                onCreatePostClick = navigateToCreatePost,
                onSearchClick = navigateToSearch,
                onHomeClick = navigateToHome,
                onProfileClick = navigateToProfile,
            )
        }

        composable("profile") {
            UserProfileScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onLogout = performLogout,
                profileViewModel = userProfileViewModel,
                onCreatePostClick = navigateToCreatePost,
                onScanClick = navigateToScan,
                onHomeClick = navigateToHome,
                onSearchClick = navigateToSearch,
            )
        }
    }
}