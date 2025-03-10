package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.campusbites.data.TestData
import com.example.campusbites.domain.model.Restaurant

@Composable
fun RestaurantListRow (
    name: String,
    description: String,
    restaurants: List<Restaurant>,
    onRestaurantClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column (
        horizontalAlignment = Alignment.End,
        modifier = modifier
            .wrapContentSize()
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(4.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
        )
        LazyRow(
            modifier = Modifier
                .wrapContentSize()
        ) {
            items(restaurants) { restaurant ->
                RestaurantCard(
                    restaurant = restaurant,
                    onRestaurantClick = onRestaurantClick
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RestaurantListRowPreview() {
    Column (
        horizontalAlignment = Alignment.End,
        modifier = Modifier
            .fillMaxSize()
    ) {
        RestaurantListRow(
            name = "Near to you",
            description = "Restaurants near you",
            restaurants = TestData.restaurants,
            onRestaurantClick = {},
            modifier = Modifier
        )
    }
}