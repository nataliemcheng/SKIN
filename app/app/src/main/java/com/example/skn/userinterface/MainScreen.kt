package com.example.skn.userinterface

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import com.example.skn.viewmodel.ProductViewModel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.skn.api.Product
import com.example.skn.viewmodel.AuthViewModel

@Composable
fun MainScreen(
    viewModel: ProductViewModel,
    authViewModel: AuthViewModel,
    onSearchClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onLogout: () -> Unit,
    onHomeClick: () -> Unit,
    onProfileClick: () -> Unit,
    onScanClick: () -> Unit

) {
    LaunchedEffect(Unit) {
        viewModel.loadProductsFromFirestore()
    }

    val products by viewModel.products.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val recentlySearched by viewModel.recentlySearched.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteProducts.collectAsStateWithLifecycle()
    val popular = products.filter { it.rating != null && it.rating >= 4.5 }.take(5)

    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isTabletLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && isTablet

    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Search") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        onHomeClick()
                    },
                        colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        unselectedIconColor = Color.Black,
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Create Post") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onSearchClick()
                    }
                )
                // Camera/Scan (center icon)
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "Scan Product",
                            modifier = Modifier.size(30.dp)
                        )
                    },
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 1
                        onScanClick()
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile") },
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 2
                        onProfileClick()
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Logout") },
                    selected = selectedTab == 4,
                    onClick = {
                        authViewModel.logout()
                        onCreatePostClick()
                    }
                )
            }
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
                            Section("Favorites", favorites, true) { viewModel.toggleFavorite(it) }
                            Section("Recently Searched", recentlySearched) { viewModel.toggleFavorite(it) }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(40.dp)
                        ) {
                            Section("Popular", popular) { viewModel.toggleFavorite(it) }
                            Column {
                                Text("Discover Tutorials", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(listOf("Dry Skin @user1", "Acne @user2", "Oily Skin @user3")) { item ->
                                        TutorialCard(item)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(40.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Section("Favorites", favorites, true) { viewModel.toggleFavorite(it) }
                        }
                        item {
                            Section("Recently Searched", recentlySearched) { viewModel.toggleFavorite(it) }
                        }
                        item {
                            Section("Popular", popular) { viewModel.toggleFavorite(it) }
                        }
                        item {
                            Column {
                                Text("Discover Tutorials", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(listOf("Dry Skin @user1", "Acne @user2", "Oily Skin @user3")) { item ->
                                        TutorialCard(item)
                                    }
                                }
                            }
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
    isFavoriteSection: Boolean = false,
    onToggleFavorite: (Product) -> Unit // ← no question mark
) {
    var expandedProduct by remember { mutableStateOf<Product?>(null) }

    Column {
        Text(title, fontSize = 20.sp, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { product ->
                Column(
                    modifier = Modifier
                        .width(160.dp)
                        .padding(4.dp)
                        .clickable {
                            expandedProduct = if (expandedProduct == product) null else product
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        product.name ?: "Unnamed",
                        maxLines = 2,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (expandedProduct == product && isFavoriteSection) {
                        Spacer(Modifier.height(6.dp))

                        Button(
                            onClick = { onToggleFavorite(product) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text("Unfavorite")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}



@Composable
fun TutorialCard(text: String) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = text, style = MaterialTheme.typography.bodySmall)
        }
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

