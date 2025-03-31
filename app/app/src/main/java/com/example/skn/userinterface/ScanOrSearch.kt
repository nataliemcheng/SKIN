package com.example.skn.userinterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.skn.viewmodel.ProductViewModel

@Composable
fun ScanOrSearchScreen(
    viewModel: ProductViewModel,
    onBackClick: () -> Unit,
    onCreatePostClick: () -> Unit,
    onScanClick: () -> Unit

) {
    var brand by remember { mutableStateOf("") }
    var productType by remember { mutableStateOf("") }

    val products by viewModel.products.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        // Header Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
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

        // Camera Placeholder
        Text("Scan a product", style = MaterialTheme.typography.titleMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .border(2.dp, Color.Gray)
                .clickable { onScanClick() },
        contentAlignment = Alignment.Center
        ) {
            Text("Point your camera at a barcode")
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    items(products) { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(product.image_link),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(100.dp)
                                    .border(1.dp, Color.LightGray)
                            )
                            Column {
                                Text(product.name ?: "Unnamed Product", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = if ((product.tag_list ?: emptyList()).contains("Vegan"))
                                        "This product is beneficial 🌿"
                                    else
                                        "This product might be harmful ❌",
                                    color = if ((product.tag_list ?: emptyList()).contains("Vegan")) Color.Green else Color.Red
                                )
                                Text(product.description ?: "No description available")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}