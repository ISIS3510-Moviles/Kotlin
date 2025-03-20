package com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
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
    ) {
        // Sección de "Popular"
        ProductListRow(
            name = "Popular",
            description = "",
            products = popularProducts,
            onProductClick = onProductClick,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Sección de "Under $20.000"
        ProductListRow(
            name = "Under $20.000",
            description = "",
            products = affordableProducts,
            onProductClick = onProductClick
        )
    }
}
