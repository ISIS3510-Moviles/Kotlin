package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campusbites.domain.model.IngredientDomain

@Composable
fun PopularIngredientsSection(
    ingredients: List<IngredientDomain>,
    onIngredientClick: (IngredientDomain) -> Unit,
    modifier: Modifier = Modifier
) {
    if (ingredients.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = "Popular Ingredients",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp)
        )

        Text(
            text = "Most frequently used ingredients in our dishes",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        IngredientGrid(
            ingredients = ingredients,
            onIngredientClick = onIngredientClick,
            modifier = Modifier.padding(4.dp),
            rowCount = 1
        )
    }
}