package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.campusbites.data.local.entity.DraftAlertEntity
import com.example.campusbites.presentation.ui.components.AlertTopBar
import com.example.campusbites.presentation.ui.viewmodels.AlertsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DraftAlertsScreen(
    onBackClick: () -> Unit,
    viewModel: AlertsViewModel
) {
    val draftAlerts by viewModel.draftAlerts.collectAsState()
    val connectivityState by viewModel.connectivityState.collectAsState()
    val isNetworkAvailable = connectivityState.isConnected

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AlertTopBar(onBackClick = onBackClick)

            Text(
                text = "Saved Draft Alerts",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (draftAlerts.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "No draft alerts saved",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                if (!isNetworkAvailable) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = "You're offline. Connect to the internet to send your draft alerts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                LazyColumn {
                    items(draftAlerts) { draft ->
                        DraftAlertItem(
                            draft = draft,
                            isNetworkAvailable = isNetworkAvailable,
                            onSendClick = {
                                viewModel.sendDraftAlert(draft.id, draft.message, draft.restaurantId)
                            },
                            onDeleteClick = {
                                viewModel.deleteDraftAlert(draft.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DraftAlertItem(
    draft: DraftAlertEntity,
    isNetworkAvailable: Boolean,
    onSendClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(draft.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = draft.restaurantName,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = draft.message,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Saved on: $formattedDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSendClick,
                    enabled = isNetworkAvailable,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send alert"
                    )
                    Text(
                        text = "Send",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete draft"
                    )
                }
            }
        }
    }
}