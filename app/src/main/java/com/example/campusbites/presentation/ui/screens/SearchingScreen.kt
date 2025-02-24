package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SearchingScreen(
    query: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Searching for: $query",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .padding(20.dp)
    )
}