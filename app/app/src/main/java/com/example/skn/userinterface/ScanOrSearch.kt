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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.skn.viewmodel.ProductViewModel
import com.example.skn.viewmodel.ChemicalsViewModel
import com.example.skn.navigation.AppBottomNavigation
import com.example.skn.navigation.NavigationTab

@Composable
fun ScanOrSearchScreen(
    viewModel: ProductViewModel,
    chemicalsViewModel: ChemicalsViewModel,
    onBackClick: () -> Unit,
    onCreatePostClick: () -> Unit = {},
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

    val products by viewModel.products.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    // ChemicalsAPI
    val chemicals by chemicalsViewModel.chemicals.collectAsState()
    val chemicalsLoading by chemicalsViewModel.isLoading.collectAsState()
    val chemicalsError by chemicalsViewModel.errorMessage.collectAsState()

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
                    onProfileClick() },
                onCreatePostClick = { selectedTab = NavigationTab.CREATE
                    onCreatePostClick() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isTabletLandscape) {
                Row(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left pane: search + scan UI (make it scrollable if content grows)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // — Header Buttons —
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 30.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(onClick = onBackClick) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                Text("Back to Feed")
                            }
                            Button(onClick = onCreatePostClick) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("Create Post")
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // — Search Inputs —
                        Text("Search a product", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Brand") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = productType,
                            onValueChange = { productType = it },
                            label = { Text("Product") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            viewModel.searchProducts(
                                brand = brand.ifBlank { null },
                                productType = productType.ifBlank { null }
                            )
                            val searchQuery = "$brand $productType" // search in ChemicalAPI
                            if (searchQuery.isNotBlank()) {
                                chemicalsViewModel.searchChemicals(searchQuery)
                            }
                        }) {
                            Text("Search")
                        }

                        Spacer(Modifier.height(24.dp))
                    }

                    // Right pane: results
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text("Results", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        when {
                            loading -> CircularProgressIndicator()
                            error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                            products.isEmpty() -> Text("No results found.")
                            else -> {
                                val harmfulIngredients = listOf(
                                    "parabens",
                                    "paraben",
                                    "phthalates",
                                    "sulfates",
                                    "formaldehyde",
                                    "triclosan",
                                    "fragrance",
                                    "alcohol",
                                    "silicones",
                                    "oxybenzone",
                                    "toluene",
                                    "PFAS",
                                    "dye"
                                )
                                val beneficialIngredients = listOf(
                                    "hyaluronic acid",
                                    "niacinamide",
                                    "vitamin C",
                                    "vitamin E",
                                    "vitamin A",
                                    "retinol",
                                    "glycolic acid",
                                    "salicylic acid",
                                    "ceramides",
                                    "peptides",
                                    "AHA",
                                    "BHA",
                                    "vegan",
                                    "free from"
                                )
                                val favoriteProducts by viewModel.favoriteProducts.collectAsState()

                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(products) { product ->
                                        // … your item UI (Image, name, tags, favorite icon) …
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Text("Ingredients Found", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        when {
                            chemicalsLoading -> CircularProgressIndicator()
                            chemicalsError != null -> Text("Error loading ingredients: $chemicalsError", color = MaterialTheme.colorScheme.error)
                            chemicals.isEmpty() -> Text("No ingredients found.")
                            else -> {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(chemicals) { chemical ->
                                        Column(modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                        ) {
                                            Text(chemical.chemicalName ?: "Unknown Chemical", style = MaterialTheme.typography.bodyLarge)
                                            chemical.casNumber?.let { cas ->
                                                Text("CAS No: $cas", style = MaterialTheme.typography.bodySmall)
                                            }
                                            chemical.brandName?.let {
                                                Text("Brand: $it", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Spacer(Modifier.height(16.dp))

                    // Search Inputs
                    Text("Search a product", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = productType,
                        onValueChange = { productType = it },
                        label = { Text("Product") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        viewModel.searchProducts(
                            brand = brand.ifBlank { null },
                            productType = productType.ifBlank { null }
                        )
                    }) {
                        Text("Search")
                    }

                    Spacer(Modifier.height(24.dp))
                    // Results Section
                    Text("Results", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    when {
                        loading -> {
                            CircularProgressIndicator()
                        }

                        error != null -> {
                            Text("Error: $error", color = MaterialTheme.colorScheme.error)
                        }

                        products.isEmpty() -> {
                            Text("No results found.")
                        }

                        else -> {
                            val harmfulIngredients = listOf(
                                "parabens",
                                "paraben",
                                "phthalates",
                                "sulfates",
                                "formaldehyde",
                                "triclosan",
                                "fragrance",
                                "alcohol",
                                "silicones",
                                "oxybenzone",
                                "toluene",
                                "PFAS",
                                "dye"
                            )
                            val beneficialIngredients = listOf(
                                "hyaluronic acid",
                                "niacinamide",
                                "vitamin C",
                                "vitamin E",
                                "vitamin A",
                                "retinol",
                                "glycolic acid",
                                "salicylic acid",
                                "ceramides",
                                "peptides",
                                "AHA",
                                "BHA",
                                "vegan",
                                "free from"
                            )


                            val favoriteProducts by viewModel.favoriteProducts.collectAsState()

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                items(products) { product ->
                                    val tags = product.tag_list?.map { it.lowercase() } ?: emptyList()
                                    val description = product.description?.lowercase() ?: ""

                                    val containsHarmful =
                                        tags.any { it in harmfulIngredients.map { h -> h.lowercase() } } ||
                                                harmfulIngredients.any { it in description }

                                    val containsBeneficial =
                                        tags.any { it in beneficialIngredients.map { b -> b.lowercase() } } ||
                                                beneficialIngredients.any { it in description }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(product.image_link),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(100.dp)
                                                .border(1.dp, Color.LightGray)
                                        )

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                product.name ?: "Unnamed Product",
                                                style = MaterialTheme.typography.titleMedium
                                            )

                                            Text(
                                                text = when {
                                                    containsHarmful -> "This product might be harmful ❌"
                                                    containsBeneficial -> "This product is beneficial 🌿"
                                                    else -> "This product is neutral"
                                                },
                                                color = when {
                                                    containsHarmful -> Color.Red
                                                    containsBeneficial -> Color.Green
                                                    else -> Color.Blue
                                                }
                                            )

                                            Text(product.description ?: "No description available")
                                        }

                                        IconButton(onClick = { viewModel.toggleFavorite(product) }) {
                                            val isFavorited = favoriteProducts.contains(product)
                                            Icon(
                                                imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = if (isFavorited) "Unfavorite" else "Favorite",
                                                tint = if (isFavorited) Color.Red else Color.Gray
                                            )
                                        }
                                    }

                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}