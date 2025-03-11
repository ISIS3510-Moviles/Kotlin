package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.campusbites.presentation.ui.components.AlertList
import com.example.campusbites.presentation.ui.components.AlertTopBar
import com.example.campusbites.presentation.ui.material.CampusBitesTheme

@Composable
fun AlertsScreen(
    notifications: List<com.example.campusbites.domain.model.AlertDomain> = emptyList(),
    onBackClick: () -> Unit,
    onAlertClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AlertTopBar(onBackClick = onBackClick)
        AlertList(
            notifications = notifications,
            onAlertClick = onAlertClick
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AlertsScreenPreview() {
    CampusBitesTheme {
        AlertsScreen(
            notifications = emptyList(),
            onBackClick = { /* Accion de volver */ },
            onAlertClick = { alertId -> /* Manejar click en alerta, ejemplo: println(alertId) */ }
        )
    }
}
