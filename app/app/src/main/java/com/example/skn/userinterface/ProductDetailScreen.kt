package com.example.skn.userinterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.skn.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Int,
    viewModel: ProductViewModel,
    onBackClick: () -> Unit = {}
) {
    val productList by viewModel.products.collectAsState()
    val product = productList.firstOrNull { it.id == productId }
    product?.let { nonNullProduct ->
        val isFavorited =
            viewModel.favoriteProducts.collectAsState().value.any { it.id == nonNullProduct.id }
        val tagType = viewModel.skinTags.collectAsState().value[nonNullProduct.id]
            ?: ProductViewModel.TagType.NONE


        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(product.name ?: "Product Details") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    product.image_link?.let { imageUrl ->
                            Image(
                                painter = rememberAsyncImagePainter(imageUrl),
                                contentDescription = product.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                    }
                    // Icons Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Favorite button
                        IconButton(onClick = { viewModel.toggleFavorite(product) }) {
                            Icon(
                                imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorited) "Unfavorite" else "Favorite",
                                tint = if (isFavorited) Color.Red else Color.Gray
                            )
                        }

                        // Good tag button
                        IconButton(onClick = {
                            val newTag = if (tagType == ProductViewModel.TagType.GOOD)
                                ProductViewModel.TagType.NONE else ProductViewModel.TagType.GOOD
                            viewModel.toggleSkinTag(product, newTag)
                        }) {
                            Icon(
                                imageVector = if (tagType == ProductViewModel.TagType.GOOD) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Mark Good",
                                tint = if (tagType == ProductViewModel.TagType.GOOD) Color.Green else Color.Gray
                            )
                        }

                        // Bad tag button
                        IconButton(onClick = {
                            val newTag = if (tagType == ProductViewModel.TagType.BAD)
                                ProductViewModel.TagType.NONE else ProductViewModel.TagType.BAD
                            viewModel.toggleSkinTag(product, newTag)
                        }) {
                            Icon(
                                imageVector = if (tagType == ProductViewModel.TagType.BAD) Icons.Default.Warning else Icons.Outlined.Warning,
                                contentDescription = "Mark Bad",
                                tint = if (tagType == ProductViewModel.TagType.BAD) Color.Red else Color.Gray
                            )
                        }
                    }
                    Text(
                        "Brand: ${product.brand ?: "N/A"}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "Type: ${product.product_type ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("Description:", style = MaterialTheme.typography.titleMedium)
                    Text(product.description ?: "No description available")
                }
        }
    }
}
