package com.example.skn.userinterface

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import com.example.skn.viewmodel.ProductViewModel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.rememberAsyncImagePainter
import com.example.skn.model.Product
import com.example.skn.viewmodel.AuthViewModel
import com.example.skn.navigation.AppBottomNavigation
import com.example.skn.navigation.NavigationTab
import androidx.compose.foundation.shape.RoundedCornerShape


@Composable
fun MainScreen(
    navController: NavHostController,
    viewModel: ProductViewModel,
    authViewModel: AuthViewModel,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onScanClick: () -> Unit,
    submitted: Boolean = false

) {
    LaunchedEffect(Unit) {
        viewModel.loadProductsFromFirestore()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(submitted) {
        if (submitted) {
            snackbarHostState.showSnackbar("Product submitted successfully")
        }
    }

    val products by viewModel.products.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val recentlySearched by viewModel.recentlySearched.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteProducts.collectAsStateWithLifecycle()
    val skinTags by viewModel.skinTags.collectAsStateWithLifecycle()
    val popular = products.filter { it.rating != null && it.rating >= 4.5 }.take(5)
    val goodForSkin = products.filter  { skinTags[it.id] == ProductViewModel.TagType.GOOD }
    val badForSkin = products.filter  { skinTags[it.id] == ProductViewModel.TagType.BAD }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isTabletLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && isTablet

    var selectedTab by remember { mutableStateOf(NavigationTab.HOME) }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { AppBottomNavigation(selectedTab = selectedTab,
                onHomeClick = { selectedTab = NavigationTab.HOME },
                onSearchClick = { selectedTab = NavigationTab.SEARCH
                    onSearchClick()
                },
                onScanClick = { selectedTab = NavigationTab.SCAN
                    onScanClick()
                },
                onProfileClick = { selectedTab = NavigationTab.PROFILE
                    onProfileClick()
                }

            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                isTabletLandscape -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(40.dp)

                        ) {
                            Section("Favorites", favorites, navController = navController,)
                            Section("Recently Searched", recentlySearched, navController = navController,)
                        }
                        VerticalDivider()
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(40.dp)
                        ) {
                            Section("Popular", popular, navController = navController,)
                            Section("Good for My SKIN", goodForSkin, navController = navController,)
                            Section("Bad for My SKIN", badForSkin, navController = navController,)

                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(40.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Section("Favorites", favorites, navController = navController,)
                        }
                        item {
                            Section("Recently Searched", recentlySearched, navController = navController,)
                        }
                        item {
                            Section("Popular", popular, navController = navController,)
                        }
                        item {
                            Section("Good for My SKIN", goodForSkin, navController = navController,)
                        }
                        item {
                            Section("Bad for My SKIN", badForSkin, navController = navController,)
                        }
                        }
                    }
                }
            }
        }
    }




@Composable
fun Section(
    title: String,
    items: List<Product>,
    navController: NavHostController
) {
    var expandedProduct by remember { mutableStateOf<Product?>(null) }

    Column {
        Text(title, fontSize = 20.sp, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { product ->
                Card(
                    modifier = Modifier
                        .width(160.dp)
                        .size(width = 160.dp, height = 200.dp)
                        .clickable {
                            navController.navigate("product/${product.id}")
                        },
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ){
                Column(
                    modifier = Modifier
                        .width(160.dp)
                        .padding(4.dp)
                        .clickable {
                            expandedProduct = if (expandedProduct == product) null else product
                            navController.navigate("product/${product.id}")
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    product.image_link?.let { imageUrl ->
                        Image(
                            painter = rememberAsyncImagePainter(imageUrl),
                            contentDescription = product.name,
                            modifier = Modifier
                                .height(100.dp)
                                .fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        product.name ?: "Unnamed",
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun DropdownMenuButton(onLogout: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Logout") },
                onClick = {
                    expanded = false
                    onLogout()
                }
            )
        }
    }
}

@Composable
fun VerticalBorder (
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    thickness: Dp = 1.dp,
    modifier: Modifier = Modifier
    ){
    Box(
        modifier
            .fillMaxHeight()
            .width(thickness)
            .background(color)
    )
}

