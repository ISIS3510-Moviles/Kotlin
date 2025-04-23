package com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.campusbites.domain.model.ProductDomain
import com.example.campusbites.presentation.ui.components.ProductListRow

@Composable
fun FoodSection(
    popularProducts: List<ProductDomain>,
    affordableProducts: List<ProductDomain>,
    onProductClick: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        if (popularProducts.isNotEmpty()) {
            ProductListRow(
                name = "Popular",
                description = "",
                products = popularProducts,
                onProductClick = onProductClick,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            Text(
                text = "No Products Available",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )
        }

        if (affordableProducts.isNotEmpty()) {
            ProductListRow(
                name = "Under $20.000",
                description = "",
                products = affordableProducts,
                onProductClick = onProductClick
            )
        }
    }
}