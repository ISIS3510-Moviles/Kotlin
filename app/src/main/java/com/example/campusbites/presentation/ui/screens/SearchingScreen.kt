package com.example.campusbites.presentation.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SearchingScreen(
    query: String
) {
    Text(
        text = "Searching for: $query",
        style = MaterialTheme.typography.bodyLarge
    )
}