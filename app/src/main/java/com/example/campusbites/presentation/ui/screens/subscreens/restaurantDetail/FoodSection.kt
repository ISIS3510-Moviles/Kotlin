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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (popularProducts.isNotEmpty()) {
            ProductListRow(
                name = "Popular",
                description = "The people's favorite ones",
                products = popularProducts,
                onProductClick = onProductClick,
            )
        } else {
            Text(
                text = "No restaurants available",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )
        }

        if (affordableProducts.isNotEmpty()) {
            ProductListRow(
                name = "Under $20.000",
                description = "Because saving money is important",
                products = affordableProducts,
                onProductClick = onProductClick,
            )
        }
    }
}
