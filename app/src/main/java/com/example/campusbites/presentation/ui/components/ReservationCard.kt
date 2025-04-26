package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ReservationCard(
    date: String,
    time: String,
    guests: Int,
    status: String,
    modifier: Modifier = Modifier
) {
    // Determinar si la reserva es pasada
    val resolvedStatus = try {
        val datePart = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
        val timePart = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
        val reservationDateTime = LocalDateTime.of(datePart, timePart)
        if (reservationDateTime.isBefore(LocalDateTime.now())) "Completed" else status
    } catch (e: Exception) {
        status
    }

    val statusColor = when (resolvedStatus) {
        "Pending" -> Color.Red
        "Completed" -> Color.Black
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Fecha
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.DateRange,
                    contentDescription = "Fecha de reservación",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = date,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hora
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Info,
                    contentDescription = "Hora de reservación",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Número de personas
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Face,
                    contentDescription = "Número de personas",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$guests Personas",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Estado de la reservación
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (resolvedStatus == "Completed") androidx.compose.material.icons.Icons.Filled.CheckCircle else androidx.compose.material.icons.Icons.Filled.Warning,
                    contentDescription = "Estado de la reservación",
                    tint = statusColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = resolvedStatus,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = statusColor,
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}
