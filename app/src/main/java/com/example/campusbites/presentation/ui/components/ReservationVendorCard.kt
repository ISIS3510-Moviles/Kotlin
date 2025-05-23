package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusbites.presentation.ui.material.CampusBitesTheme

@Composable
fun ReservationVendorCard(
    modifier: Modifier = Modifier,
    userFullName: String, // Asumiendo que tendrás el nombre del usuario
    date: String,
    time: String,
    guests: Int,
    status: String, // "Pending", "Confirmed", "Cancelled"
    onMarkAsCompleted: (() -> Unit)?,
    onCancelReservation: (() -> Unit)?
) {
    val statusColor = when (status) {
        "Pending" -> MaterialTheme.colorScheme.secondary
        "Confirmed" -> MaterialTheme.colorScheme.primary
        "Cancelled" -> MaterialTheme.colorScheme.error
        else -> Color.Gray
    }

    val statusIcon = when (status) {
        "Pending" -> Icons.Filled.Info
        "Confirmed" -> Icons.Filled.Check
        "Cancelled" -> Icons.Filled.Close
        else -> Icons.Filled.Info
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = userFullName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            InfoRow(icon = Icons.Filled.DateRange, label = "Date", value = date)
            InfoRow(icon = Icons.Filled.Notifications, label = "Time", value = time)
            InfoRow(icon = Icons.Filled.Face, label = "Guests", value = guests.toString())
            InfoRow(icon = statusIcon, label = "Status", value = status, valueColor = statusColor)

            // Acciones solo si la reserva no está cancelada ni completada por el vendor
            if (status == "Pending") {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    onCancelReservation?.let {
                        Button(
                            onClick = it,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    onMarkAsCompleted?.let {
                        Button(
                            onClick = it,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Confirmed", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, valueColor: Color? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewReservationVendorCard_Pending() {
    CampusBitesTheme {
        ReservationVendorCard(
            userFullName = "John Doe",
            date = "25 Dec 2025",
            time = "14:30",
            guests = 4,
            status = "Pending",
            onMarkAsCompleted = {},
            onCancelReservation = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewReservationVendorCard_Completed() {
    CampusBitesTheme {
        ReservationVendorCard(
            userFullName = "Jane Smith",
            date = "26 Dec 2025",
            time = "19:00",
            guests = 2,
            status = "Completed",
            onMarkAsCompleted = null,
            onCancelReservation = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewReservationVendorCard_Cancelled() {
    CampusBitesTheme {
        ReservationVendorCard(
            userFullName = "Peter Jones",
            date = "27 Dec 2025",
            time = "12:00",
            guests = 5,
            status = "Cancelled",
            onMarkAsCompleted = null,
            onCancelReservation = null
        )
    }
}