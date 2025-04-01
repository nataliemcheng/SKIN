package com.example.skn.userinterface

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.skn.viewmodel.ProductViewModel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MainScreen(
    viewModel: ProductViewModel,
    onSearchClick: () -> Unit,
    onCreatePostClick: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val favorites = products.filter { it.brand == "fenty" }.take(5)
    val recent = products.takeLast(5)
    val popular = products.filter { it.rating != null && it.rating!! >= 4.5 }.take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan or Search")
            }

            Button(onClick = onCreatePostClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create Post")
            }
        }

        if (loading) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        } else {
            Section(title = "Favorites", items = favorites.map { it.name ?: "Unnamed" })
            Section(title = "Recently Searched", items = recent.map { it.name ?: "Unnamed" })
            Section(title = "Popular", items = popular.map { it.name ?: "Unnamed" })
        }

        // Placeholder tutorial list
        val tutorials = listOf("Dry Skin @user1", "Acne @user2", "Oily Skin @user3")
        Column {
            Text("Discover Tutorials", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxHeight()) {
                items(tutorials) { item -> TutorialCard(item) }
        }
        }
    }
}

@Composable
fun Section(title: String, items: List<String>) {
    Column {
        Text(title, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items) { item ->
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
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