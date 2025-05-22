package com.example.campusbites.presentation.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.campusbites.domain.model.AlertDomain
import com.example.campusbites.R
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun AlertCard(
    notification: AlertDomain,
    onAlertClick: (String) -> Unit,
    onUpvoteClick: (AlertDomain) -> Unit,
    onDownvoteClick: (AlertDomain) -> Unit
) {
    Log.d("AlertCard", "Icono de alerta: ${notification.icon}, Restaurante: ${notification.restaurantDomain.name}") // Modificado para usar notification.icon

    // Formateador para la fecha y hora
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    }
    val formattedDateTime = remember(notification.datetime) {
        try {
            notification.datetime.format(dateTimeFormatter)
        } catch (e: Exception) {
            Log.e("AlertCard", "Error al formatear fecha: ${notification.datetime}", e)
            "Fecha inválida" // Fallback
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
        ,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = { onAlertClick(notification.id) }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp) // Padding interno para el contenido del Card
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top // Alinear elementos al tope
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(notification.icon) // Usar el icono de la alerta que viene de CreateAlertUseCase
                    .crossfade(true)
                    .placeholder(R.drawable.burguer_icon)
                    .error(R.drawable.restaurant_logo)
                    .build(),
                contentDescription = "Icono de alerta",
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .align(Alignment.CenterVertically)
            )

            Spacer(modifier = Modifier.width(12.dp)) // Espacio entre icono y texto

            Column(
                modifier = Modifier.weight(1f) // Para que la columna de texto ocupe el espacio restante
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = notification.restaurantDomain.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false) // Evita que el nombre empuje la fecha demasiado
                    )
                    Text(
                        text = formattedDateTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp)) // Pequeño espacio

                Text(
                    text = "By: ${notification.publisher.name}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}