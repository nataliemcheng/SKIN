package com.example.skn.userinterface

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.*
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
import com.example.skn.viewmodel.ProductViewModel
import com.example.skn.viewmodel.ChemicalsViewModel
import com.example.skn.navigation.AppBottomNavigation
import com.example.skn.navigation.NavigationTab
import com.example.skn.viewmodel.UserProfileViewModel

@Composable
fun ScanOrSearchScreen(
    viewModel: ProductViewModel,
    chemicalsViewModel: ChemicalsViewModel,
    profileViewModel: UserProfileViewModel,
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

    val userProfile by profileViewModel.userProfile.collectAsState()
    val skinType    = userProfile?.skinType ?: "Unknown"
    var showInfo by remember { mutableStateOf(false) }

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

                        OutlinedTextField(
                            value = userSearch,
                            onValueChange = { userSearch = it },
                            placeholder = { Text("Search a brand, product, or ingredient") },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp, max = 120.dp),
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

                        // Button for skin tip
                        FloatingActionButton(containerColor = MaterialTheme.colorScheme.primary,
                            onClick = { showInfo = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                Icon(imageVector = Icons.Default.Checklist, contentDescription = "Skin Tips", tint = MaterialTheme.colorScheme.background)
                                Text("  Skin Tips")
                            }
                        }

                    }

                    Spacer(Modifier.height(24.dp))

                    // Right pane: results
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        if (resultsLoading) {
                            CircularProgressIndicator()
                        } else {
                            if (productsError != null) {
                                Text(
                                    "Product error: $productsError",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            if (chemicalsError != null) {
                                Text(
                                    "Ingredients error: $chemicalsError",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            if (products.isEmpty() && chemicals.isEmpty()) {
                                Text("No results found.")
                            }
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
                                Tab(
                                    text = { Text(title) },
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index })
                            }
                        }

                        val favoriteProducts by viewModel.favoriteProducts.collectAsState()
                        val skinTags by viewModel.skinTags.collectAsState()

                        when (selectedTabIndex) {
                            0 -> if (products.isNotEmpty()) ProductResults(
                                viewModel = viewModel,
                                products = products,
                                favorites = favoriteProducts,
                                skinTags = skinTags,
                                onToggleFavorite = { product -> viewModel.toggleFavorite(product) },
                                onToggleTag = { product, tagType ->
                                    viewModel.toggleSkinTag(
                                        product,
                                        tagType
                                    )
                                },
                                navController = navController
                            )

                            1 -> if (chemicals.isNotEmpty()) IngredientResults(chemicals)
                        }
                    }
                    if (showInfo) {
                        // backdrop
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { showInfo = false } // dismiss when tapping outside
                        ){
                            // skin info card

                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.9f).wrapContentHeight(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                            val goodRecs = when (skinType) {
                                "Oily"        -> listOf("Salicylic Acid", "Niacinamide", "Clay")
                                "Dry"         -> listOf("Hyaluronic Acid", "Ceramides", "Glycerin")
                                "Combination" -> listOf("Niacinamide", "Hyaluronic Acid")
                                "Sensitive"   -> listOf("Aloe Vera", "Centella Asiatica")
                                else          -> listOf("Vitamin C", "Ceramides")
                            }
                            val avoidRecs = when (skinType) {
                                "Oily"        -> listOf("Heavy Oils", "Silicones", "Alcohol")
                                "Dry"         -> listOf("Sulfates", "Fragrance", "Retinoids (initially)")
                                "Combination" -> listOf("Alcohol", "Harsh Exfoliants")
                                "Sensitive"   -> listOf("Fragrance", "Essential Oils", "AHA/BHA (high %)")
                                else          -> listOf("Parabens", "Phthalates", "Formaldehyde")
                            }

                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Good for $skinType skin",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                goodRecs.forEach {
                                    Text(
                                        "• $it",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }

                                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                                Text(
                                    "Avoid for $skinType skin",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                avoidRecs.forEach {
                                    Text(
                                        "• $it",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                Button(
                                    onClick = { showInfo = false },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Close")
                                }
                            }
                            }
                        }
                    } //
                }
        }
            // Portrait mode
            else {
                Column(modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp))
                {
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = userSearch,
                        onValueChange = { userSearch = it },
                        placeholder = { Text("Search for products or ingredients") },
                        trailingIcon = {
                            IconButton(onClick = {
                                val searchQuery = userSearch.trim()
                                if (searchQuery.isNotBlank()) {
                                    viewModel.searchProducts(searchQuery)
                                    chemicalsViewModel.searchChemicals(searchQuery)
                                }
                            }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp, max = 120.dp)
                    )


                    Spacer(Modifier.height(16.dp))

                    // Button for skin tip
                    FloatingActionButton(containerColor = MaterialTheme.colorScheme.primary,
                        onClick = { showInfo = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Icon(imageVector = Icons.Default.Checklist, contentDescription = "Skin Tips", tint = MaterialTheme.colorScheme.background)
                            Text("  Skin Tips")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

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
                                viewModel = viewModel,
                                products = products,
                                favorites = favoriteProducts,
                                skinTags = skinTags,
                                onToggleFavorite = { product -> viewModel.toggleFavorite(product) },
                                onToggleTag = { product, tagType -> viewModel.toggleSkinTag(product, tagType) },
                                navController = navController)
                        1 -> if (chemicals.isNotEmpty()) IngredientResults(chemicals)
                    }


                }
                if (showInfo) {
                    // backdrop
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { showInfo = false } // dismiss when tapping outside
                    ){
                        // skin info card
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.9f).wrapContentHeight(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            val goodRecs = when (skinType) {
                                "Oily"        -> listOf("Salicylic Acid", "Niacinamide", "Clay")
                                "Dry"         -> listOf("Hyaluronic Acid", "Ceramides", "Glycerin")
                                "Combination" -> listOf("Niacinamide", "Hyaluronic Acid")
                                "Sensitive"   -> listOf("Aloe Vera", "Centella Asiatica")
                                else          -> listOf("Vitamin C", "Ceramides")
                            }
                            val avoidRecs = when (skinType) {
                                "Oily"        -> listOf("Heavy Oils", "Silicones", "Alcohol")
                                "Dry"         -> listOf("Sulfates", "Fragrance", "Retinoids (initially)")
                                "Combination" -> listOf("Alcohol", "Harsh Exfoliants")
                                "Sensitive"   -> listOf("Fragrance", "Essential Oils", "AHA/BHA (high %)")
                                else          -> listOf("Parabens", "Phthalates", "Formaldehyde")
                            }

                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Good for $skinType skin",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                goodRecs.forEach {
                                    Text(
                                        "• $it",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }

                                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                                Text(
                                    "Avoid for $skinType skin",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                avoidRecs.forEach {
                                    Text(
                                        "• $it",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                Button(
                                    onClick = { showInfo = false },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Close")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
