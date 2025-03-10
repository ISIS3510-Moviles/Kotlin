package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.campusbites.domain.model.FoodTag
import com.example.campusbites.presentation.ui.material.CampusBitesTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodTagGrid(
    foodTags: List<FoodTag>,
    onFoodTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column (
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .width(500.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.Center,
            maxLines = 2,
            modifier = modifier
                .padding(8.dp)
                .wrapContentSize()
        ) {
            foodTags.forEach { foodTag ->
                FoodTagCard(
                    foodTag = foodTag,
                    onFoodTagClick = onFoodTagClick,
                    modifier = Modifier
                        .padding(8.dp)
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun FoodTagFlowRowPreview(){
    CampusBitesTheme {
        FoodTagGrid(
            emptyList(),
            onFoodTagClick = {}
        )
    }
}