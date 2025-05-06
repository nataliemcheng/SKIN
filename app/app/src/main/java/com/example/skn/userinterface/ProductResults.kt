package com.example.skn.userinterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.skn.model.Product
import com.example.skn.viewmodel.ProductViewModel
import androidx.navigation.NavHostController

@Composable
fun ProductResults(
    viewModel: ProductViewModel,
    products: List<Product>,
    favorites: List<Product>,
    skinTags: Map<Int, ProductViewModel.TagType>,
    onToggleFavorite: (Product) -> Unit,
    onToggleTag: (Product, ProductViewModel.TagType) -> Unit,
    navController: NavHostController,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(8.dp)
    ) {
        items(products) { product ->
            Card(colors = CardDefaults.cardColors(containerColor =  MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 8.dp) // Only vertical spacing between cards
                    .clickable {
                        viewModel.logSearchQueryToFirebase(product)
                        viewModel.loadRecentSearchesFromFirebase()
                        navController.navigate("product/${product.id}")
                    }
            )
            {
                Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = rememberAsyncImagePainter(product.image_link),
                        contentDescription = product.name,
                        modifier = Modifier.size(125.dp).clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        product.name?.let {
                            Text(text = it, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                        product.brand?.let {
                            Text(
                                text = "Brand: $it",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = { onToggleFavorite(product) }) {
                                val isFavorited = favorites.any { it.id == product.id }
                                Icon(
                                    imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorited) "Unfavorite" else "Favorite",
                                    tint = if (isFavorited) Color.Red else Color.Gray
                                )
                            }

                            IconButton(onClick = {
                                val currentTag = skinTags[product.id]
                                val newTag = if (currentTag == ProductViewModel.TagType.GOOD)
                                    ProductViewModel.TagType.NONE else ProductViewModel.TagType.GOOD
                                onToggleTag(product, newTag)
                            }) {
                                val isGood = skinTags[product.id] == ProductViewModel.TagType.GOOD
                                Icon(
                                    imageVector = if (isGood) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                                    contentDescription = "Mark Good",
                                    tint = if (isGood) Color.Green else Color.Gray
                                )
                            }

                            IconButton(onClick = {
                                val currentTag = skinTags[product.id]
                                val newTag = if (currentTag == ProductViewModel.TagType.BAD)
                                    ProductViewModel.TagType.NONE else ProductViewModel.TagType.BAD
                                onToggleTag(product, newTag)
                            }) {
                                val isBad = skinTags[product.id] == ProductViewModel.TagType.BAD
                                Icon(
                                    imageVector = if (isBad) Icons.Default.Warning else Icons.Outlined.Warning,
                                    contentDescription = "Mark Bad",
                                    tint = if (isBad) Color.Red else Color.Gray
                                )
                            }
                        }

                    }

                }


                }
            }
        }
    }
}
