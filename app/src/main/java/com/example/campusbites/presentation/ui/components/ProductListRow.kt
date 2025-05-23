package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campusbites.domain.model.ProductDomain

@Composable
fun ProductListRow(
    name: String,
    description: String,
    products: List<ProductDomain>,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier.wrapContentSize()
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
        )

        if (products.isEmpty()) {
            Text(
                text = "No products available at the moment.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyRow(
                modifier = Modifier.wrapContentSize()
            ) {
                items(products) { product ->
                    ProductCard(
                        product = product,
                        onProductClick = onProductClick
                    )
                }
            }
        }
    }
}
