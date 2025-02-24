package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    modifier : Modifier = Modifier
) {
    Text(
        text = "Profile Screen",
        modifier = Modifier
            .padding(20.dp)
    )

}