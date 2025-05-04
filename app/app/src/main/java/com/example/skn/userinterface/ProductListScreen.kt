package com.example.skn.userinterface

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.skn.viewmodel.ProductViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProductListScreen(viewModel: ProductViewModel) {
    var brand by remember { mutableStateOf("") }
    var productType by remember { mutableStateOf("") }

    val products by viewModel.products.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Makeup Product Search", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = brand,
            onValueChange = { brand = it },
            label = { Text("Brand") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = productType,
            onValueChange = { productType = it },
            label = { Text("Product Type (e.g., lipstick, eyeliner)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

//        Button(
//            onClick = {
//                viewModel.searchProducts(
//                    brand = brand.ifBlank { null },
//                    productType = productType.ifBlank { null }
//                )
//            },
//            modifier = Modifier.align(Alignment.End)
//        ) {
//            Text("Search")
//        }

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        } else if (products.isEmpty()) {
            Text("No results found")
        } else {
            LazyColumn {
                items(products) { product ->
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(product.name ?: "Unnamed Product", style = MaterialTheme.typography.bodyLarge)
                        Text("Brand: ${product.brand ?: "Unknown"}")
                        Text("Type: ${product.product_type ?: "N/A"}")
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}