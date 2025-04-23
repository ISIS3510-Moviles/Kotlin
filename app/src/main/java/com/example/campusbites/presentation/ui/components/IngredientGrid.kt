package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.campusbites.domain.model.FoodTagDomain
import com.example.campusbites.domain.model.IngredientDomain

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IngredientGrid(
    ingredients: List<IngredientDomain>,
    onIngredientClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    rowCount: Int = 2
) {
    val scrollState = rememberScrollState()

    val columns = ingredients.chunked(rowCount)

    Column(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        for (rowIndex in 0 until rowCount) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = if (rowIndex < rowCount - 1) 8.dp else 0.dp)
            ) {
                columns.forEach { columnItems ->
                    if (rowIndex < columnItems.size) {
                        IngredientCard(
                            ingredient = columnItems[rowIndex],
                            onIngredientClick = onIngredientClick,
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }
}