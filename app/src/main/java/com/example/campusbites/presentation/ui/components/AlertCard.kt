package com.example.campusbites.presentation.ui.components

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.R

@Composable
fun AlertCard(
    notification: AlertDomain,
    onAlertClick: (String) -> Unit,
    onUpvoteClick: (AlertDomain) -> Unit,
    onDownvoteClick: (AlertDomain) -> Unit
) {
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
                model = notification.restaurantDomain.profilePhoto,
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
                // Fila para mostrar los votos a la derecha.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { onUpvoteClick(notification) }) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_upvote),
                            contentDescription = "Upvote",
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                        )
                    }
                    Text(
                        text = notification.votes.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = { onDownvoteClick(notification) }) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_downvote),
                            contentDescription = "Downvote",
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }
            }
        }
    }
}