package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.campusbites.domain.model.AlertDomain

@Composable
fun AlertCard(notification: AlertDomain, onAlertClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .padding(16.dp),

        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = { onAlertClick(notification.id) }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
        ) {
            AsyncImage(
                model = notification.icon,
                contentDescription = "Notification Icon",
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp)
            )
            Column {
                Text(
                    text = notification.restaurantDomain.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}