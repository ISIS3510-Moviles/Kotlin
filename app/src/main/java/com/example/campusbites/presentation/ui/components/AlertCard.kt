package com.example.campusbites.presentation.ui.components

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.R

@Composable
fun AlertCard(
    notification: AlertDomain,
    onAlertClick: (String) -> Unit,
    onUpvoteClick: (AlertDomain) -> Unit,
    onDownvoteClick: (AlertDomain) -> Unit
) {
    Log.d("AlertCard", "Icono de alerta: ${notification.restaurantDomain.profilePhoto}")
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
            modifier = Modifier.padding(16.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(notification.icon)
                    .crossfade(true)
                    .placeholder(R.drawable.burguer_icon)
                    .error(R.drawable.restaurant_logo)
                    .build(),
                contentDescription = "Icono de alerta",
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = notification.restaurantDomain.name,
                    style = MaterialTheme.typography.titleMedium
                )

                // Añadimos el nombre del creador aquí
                Text(
                    text = "By: ${notification.publisher.name}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}