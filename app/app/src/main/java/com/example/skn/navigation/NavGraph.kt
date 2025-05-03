package com.example.skn.navigation

import android.util.Log
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.skn.userinterface.MainScreen
import com.example.skn.userinterface.ScanOrSearchScreen
import com.example.skn.userinterface.BarcodeScannerScreen
import com.example.skn.userinterface.LoginScreen
import com.example.skn.userinterface.OnBoardingScreen
import com.example.skn.userinterface.ProductDetailScreen
import com.example.skn.userinterface.SignUpScreen
import com.example.skn.userinterface.SubmitProductFormScreen
import com.example.skn.userinterface.UserProfileScreen
import com.example.skn.viewmodel.AuthViewModel
import com.example.skn.viewmodel.ProductViewModel
import com.example.skn.viewmodel.UserProfileViewModel
import com.example.skn.viewmodel.ChemicalsViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppNavGraph(
    navController: NavHostController,
    productViewModel: ProductViewModel,
    authViewModel: AuthViewModel,
    userProfileViewModel: UserProfileViewModel,
    chemicalsViewModel: ChemicalsViewModel,
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

        composable("main?submitted={submitted}", arguments = listOf(
            navArgument("submitted") { defaultValue = "false" }
        )) { backStackEntry ->
            val submitted = backStackEntry.arguments?.getString("submitted") == "true"

            MainScreen(
                viewModel = productViewModel,
                authViewModel = authViewModel,
                onSearchClick = navigateToSearch,
                onScanClick = navigateToScan,
                onProfileClick = navigateToProfile,
                navController = navController,
                submitted = submitted // ← pass this to MainScreen

            )
        }

        composable("product/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")?.toIntOrNull()
            if (productId != null) {
                ProductDetailScreen(
                    productId = productId,
                    viewModel = productViewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }



        composable(
            "scanOrSearch?barcode={barcode}",
            arguments = listOf(navArgument("barcode") { defaultValue = "" })
        ) { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode")

            ScanOrSearchScreen(
                viewModel = productViewModel,
                chemicalsViewModel = chemicalsViewModel,
                navController = navController,
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
                viewModel = productViewModel,
            )
        }

        composable(
            route = "submitProduct/{barcode}",
            arguments = listOf(navArgument("barcode") { defaultValue = "" })
        ) { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode") ?: ""
            val context = LocalContext.current
            val snackbarHostState = remember { SnackbarHostState() }

            SubmitProductFormScreen(
                navController = navController,
                barcode = barcode,
                onSubmit = { name, brand, description, ingredients, frontUri, backUri, upc ->
                    // Use a coroutine for Firebase calls
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            productViewModel.submitProductToFirestore(
                                name = name,
                                brand = brand,
                                description = description,
                                ingredients = ingredients,
                                frontUri = frontUri,
                                backUri = backUri,
                                barcode = upc
                            )
                            withContext(Dispatchers.Main) {
                                productViewModel.resetState()
                                navController.navigate("main?submitted=true") {
                                    popUpTo("main") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                            Log.d("Firestore", "✅ Product submitted successfully.")
                        } catch (e: Exception) {
                            Log.e("Firestore", "❌ Failed to submit: ${e.localizedMessage}")
                            withContext(Dispatchers.Main) {
                                snackbarHostState.showSnackbar("❌ Failed to submit product")
                            }
                        }
                    }
                }
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