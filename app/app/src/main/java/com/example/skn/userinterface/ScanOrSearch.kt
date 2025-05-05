package com.example.skn.userinterface

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.skn.viewmodel.ProductViewModel
import com.example.skn.viewmodel.ChemicalsViewModel
import com.example.skn.navigation.AppBottomNavigation
import com.example.skn.navigation.NavigationTab

@Composable
fun ScanOrSearchScreen(
    viewModel: ProductViewModel,
    chemicalsViewModel: ChemicalsViewModel,
    navController: NavHostController,
    onBackClick: () -> Unit,
    onScanClick: () -> Unit = {},
    snackbarHostState: SnackbarHostState,
    scannedBarcode: String? = null,
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isTabletLandscape =
        isTablet && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var brand by remember { mutableStateOf("") }
    var productType by remember { mutableStateOf("") }
    var ingredient by remember { mutableStateOf("") }

    val products by viewModel.products.collectAsState()
    val productsLoading by viewModel.loading.collectAsState()
    val productsError by viewModel.error.collectAsState()

    // ChemicalsAPI
    val chemicals by chemicalsViewModel.chemicals.collectAsState()
    val chemicalsLoading by chemicalsViewModel.isLoading.collectAsState()
    val chemicalsError by chemicalsViewModel.errorMessage.collectAsState()

    var userSearch by remember { mutableStateOf("")}
    val resultsLoading = productsLoading || chemicalsLoading

    var selectedTab by remember { mutableStateOf(NavigationTab.SEARCH) }

    LaunchedEffect(scannedBarcode) {
        scannedBarcode?.let {
            snackbarHostState.showSnackbar("Scanned: $it")
        }
    }

    Scaffold(bottomBar = { AppBottomNavigation(selectedTab = selectedTab,
                onHomeClick = {
                    selectedTab = NavigationTab.HOME
                    onHomeClick() },
                onSearchClick = { selectedTab = NavigationTab.SEARCH },
                onScanClick = { selectedTab = NavigationTab.SCAN
                    onScanClick() },
                onProfileClick = { selectedTab = NavigationTab.PROFILE
                    onProfileClick() }

            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Landscape mode
            if (isTabletLandscape) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left pane: search + scan UI
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)

                    ) {
                        Spacer(Modifier.height(24.dp))

                        OutlinedTextField(value = userSearch,
                            onValueChange = { userSearch = it },
                            placeholder = { Text("Search a brand, product, or ingredient") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        )
                        Spacer(Modifier.height(16.dp))

                        Button(onClick = {
                            val searchQuery = userSearch.trim()
                            if (searchQuery.isNotBlank()) {
                                viewModel.searchProducts(searchQuery)
                                chemicalsViewModel.searchChemicals(searchQuery)
                            }
                        }) {
                            Text("Go")
                        }

                        Spacer(Modifier.height(24.dp))
                    }

                    // Right pane: results
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (resultsLoading) {
                            CircularProgressIndicator()
                        } else {
                            if (productsError != null) { Text("Product error: $productsError", color = MaterialTheme.colorScheme.error) }
                            if (chemicalsError != null) { Text("Ingredients error: $chemicalsError", color = MaterialTheme.colorScheme.error) }
                            if (products.isEmpty() && chemicals.isEmpty()) { Text("No results found.") }
                        }

                        val tabs = listOf("Products", "Ingredients")
                        var selectedTabIndex by remember { mutableStateOf(0) }

                        TabRow(selectedTabIndex = selectedTabIndex,
                            indicator = { tabPositions -> // Indicator for the selected tab
                                SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = Color.DarkGray
                                )
                            }) {
                            tabs.forEachIndexed { index, title ->
                                Tab(text = { Text(title) }, selected = selectedTabIndex == index, onClick = { selectedTabIndex = index })
                            }
                        }

                        val favoriteProducts by viewModel.favoriteProducts.collectAsState()
                        val skinTags by viewModel.skinTags.collectAsState()

                        when (selectedTabIndex) {
                            0 -> if (products.isNotEmpty()) ProductResults(
                                products = products,
                                favorites = favoriteProducts,
                                skinTags = skinTags,
                                onToggleFavorite = { product -> viewModel.toggleFavorite(product) },
                                onToggleTag = { product, tagType -> viewModel.toggleSkinTag(product, tagType) },
                                navController = navController)
                            1 -> if (chemicals.isNotEmpty()) IngredientResults(chemicals)
                        }


                    }
                }
            }
            // Portrait mode
            else {
                Column(modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp))
                {
                    Spacer(Modifier.height(16.dp))

                    Row(modifier = modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(value = userSearch,
                            onValueChange = { userSearch = it },
                            placeholder = { Text("Search a brand, product, or ingredient") },
                            modifier = Modifier.weight(1f)
                        )

                        Button(onClick = {
                            val searchQuery = userSearch.trim()
                            if (searchQuery.isNotBlank()) {
                                viewModel.searchProducts(searchQuery)
                                chemicalsViewModel.searchChemicals(searchQuery)
                            }
                        }) {
                            Text("Go")
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    if (resultsLoading) {
                        CircularProgressIndicator()
                    } else {
                        if (productsError != null) { Text("Product error: $productsError", color = MaterialTheme.colorScheme.error) }
                        if (chemicalsError != null) { Text("Ingredients error: $chemicalsError", color = MaterialTheme.colorScheme.error) }
                        if (products.isEmpty() && chemicals.isEmpty()) { Text("No results found.") }
                    }

                    val tabs = listOf("Products", "Ingredients")
                    var selectedTabIndex by remember { mutableStateOf(0) }

                    TabRow(selectedTabIndex = selectedTabIndex,
                        indicator = { tabPositions -> // Indicator for the selected tab
                            SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = Color.DarkGray
                            )
                        }) {
                        tabs.forEachIndexed { index, title ->
                            Tab(text = { Text(title) }, selected = selectedTabIndex == index, onClick = { selectedTabIndex = index })
                        }
                    }

                    val favoriteProducts by viewModel.favoriteProducts.collectAsState()
                    val skinTags by viewModel.skinTags.collectAsState()

                    when (selectedTabIndex) {
                        0 -> if (products.isNotEmpty()) ProductResults(
                                products = products,
                                favorites = favoriteProducts,
                                skinTags = skinTags,
                                onToggleFavorite = { product -> viewModel.toggleFavorite(product) },
                                onToggleTag = { product, tagType -> viewModel.toggleSkinTag(product, tagType) },
                                navController = navController)
                        1 -> if (chemicals.isNotEmpty()) IngredientResults(chemicals)
                    }


                }
            }
        }
    }
}
