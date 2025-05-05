package com.example.skn.userinterface

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.skn.model.ChemicalRecord
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme

@Composable
fun IngredientResults(
    chemicals: List<ChemicalRecord>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(chemicals) { chemical ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
//                    Text(chemical.chemicalName ?: "Unknown Chemical", style = MaterialTheme.typography.bodyLarge)

                    chemical.productName?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    chemical.brandName?.let { Text("Brand: $it", style = MaterialTheme.typography.bodyMedium) }
                    chemical.chemicalName?.let { Text("Chemicals found: $it", style = MaterialTheme.typography.bodySmall)}
                }
            }
        }
    }
}


