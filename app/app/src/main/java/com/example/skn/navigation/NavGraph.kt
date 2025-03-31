package com.example.skn.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.skn.userinterface.MainScreen
import com.example.skn.userinterface.ScanOrSearchScreen
import com.example.skn.userinterface.BarcodeScannerScreen
import com.example.skn.viewmodel.ProductViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = modifier
    ) {
        composable("main") {
            MainScreen(
                viewModel = viewModel,
                onSearchClick = { navController.navigate("search") },
                onCreatePostClick = { /* TODO: navigate to post */ }
            )
        }
        composable("search") {
            ScanOrSearchScreen(
                viewModel = viewModel,
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