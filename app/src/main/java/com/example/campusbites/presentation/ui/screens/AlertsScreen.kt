package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusbites.presentation.ui.components.AlertList
import com.example.campusbites.presentation.ui.components.AlertTopBar
import com.example.campusbites.presentation.ui.material.CampusBitesTheme
import com.example.campusbites.presentation.ui.viewmodels.AlertsViewModel

@Composable
fun AlertsScreen(
    onBackClick: () -> Unit,
    onAlertClick: (String) -> Unit,
    viewModel: AlertsViewModel = hiltViewModel()
) {

    val uiState = viewModel.uiState.collectAsState()
    val notifications = uiState.value.alerts

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
            onBackClick = { /* Accion de volver */ },
            onAlertClick = { alertId -> /* Manejar click en alerta, ejemplo: println(alertId) */ }
        )
    }
}
