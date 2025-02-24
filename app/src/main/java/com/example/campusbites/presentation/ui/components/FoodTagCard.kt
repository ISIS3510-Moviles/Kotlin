package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.campusbites.data.TestData.foodTags
import com.example.campusbites.domain.model.FoodTag

@Composable
fun FoodTagCard(
    foodTag: FoodTag,
    onFoodTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier
            .wrapContentSize()
            .size(100.dp)
            .clickable { onFoodTagClick(foodTag.name) }
    ) {
        Column (
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
        ) {
            Image(
                painter = painterResource(id = foodTag.icon),
                contentDescription = "${foodTag.name} icon",
                modifier = Modifier
                    .fillMaxWidth()
                    .size(72.dp)
            )
            Text(
                text = foodTag.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FoodTagCardPreview() {
    FoodTagCard(
        foodTags.get(0),
        onFoodTagClick = {}
    )
}