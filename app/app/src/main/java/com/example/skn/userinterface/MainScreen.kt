package com.example.skn.userinterface

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onProfileClick: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val recentlySearched by viewModel.recentlySearched.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteProducts.collectAsStateWithLifecycle()
    val popular = products.filter { it.rating != null && it.rating >= 4.5 }.take(5)

    // checks if its a tablet in landscape
    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isTabletLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && isTablet

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
//        Navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Scan or Search
            Button(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan or Search")
            }
            // Create Post
            Button(onClick = onCreatePostClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create Post")
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // User Profile
            Button(onClick = onProfileClick) {
                Icon(Icons.Default.AccountCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Your Profile")
            }
            DropdownMenuButton(onLogout = {
                authViewModel.logout()
                onLogout() // optional: for navigation
            })
        }

        Spacer(Modifier.height(16.dp))

        when {
            loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
            }
            else -> {
                if (isTabletLandscape) {
                    // Two-column layout: Left Column holds Favorites and Recently Searched,
                    // Right Column holds Popular and Tutorials.
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(40.dp)
                        ) {
                            Section(
                                title = "Favorites",
                                items = favorites,
                                isFavoriteSection = true,
                                onToggleFavorite = { viewModel.toggleFavorite(it) }
                            )
                            Section(
                                title = "Recently Searched",
                                items = recentlySearched,
                                onToggleFavorite = { viewModel.toggleFavorite(it) }
                            )
                        }
                        // Right Column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(40.dp)
                        ) {
                            Section(
                                title = "Popular",
                                items = popular,
                                onToggleFavorite = { viewModel.toggleFavorite(it) }
                            )
                            Column {
                                Text("Discover Tutorials", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(listOf("Dry Skin @user1", "Acne @user2", "Oily Skin @user3")) { item ->
                                        TutorialCard(text = item)
                                    }
                                }
                            }
                        }
                    }
                }
                else
                    LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(40.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Section(
                            title = "Favorites",
                            items = favorites,
                            isFavoriteSection = true,
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
                    }
                    item {
                        Section(
                            title = "Recently Searched",
                            items = recentlySearched,
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
                    }
                    item {
                        Section(
                            title = "Popular",
                            items = popular,
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
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