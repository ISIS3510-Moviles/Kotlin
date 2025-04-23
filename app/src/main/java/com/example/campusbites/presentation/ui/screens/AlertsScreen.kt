package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.campusbites.presentation.ui.components.AlertList
import com.example.campusbites.presentation.ui.components.AlertTopBar
import com.example.campusbites.presentation.ui.viewmodels.AlertsViewModel
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    navController: NavController,
    onBackClick: () -> Unit,
    viewModel: AlertsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val notifications = uiState.alerts
    val isLoading = uiState.isLoading

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("alert_create") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Agregar alerta"
                )
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.refreshAlertsManually() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                AlertTopBar(onBackClick = onBackClick)

                Text(
                    text = "Alerts",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )

                AlertList(
                    notifications = notifications,
                    onAlertClick = { alertId ->
                        println("Alert clicked: $alertId")
                        // Aqui se debe navegar al detalle del restaurante
                        // navController.navigate("restaurant/$restaurant_id")
                    },
                    onUpvoteClick = { alert ->
                        viewModel.upvote(alert)
                    },
                    onDownvoteClick = { alert ->
                        viewModel.downvote(alert)
                    }
                )
            }
        }
    }
}