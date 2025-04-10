package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.domain.model.UserDomain
import com.example.campusbites.presentation.ui.components.ReservationCard
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(
    reservations: List<ReservationDomain> = emptyList()
) {
    // Formateadores para fecha y hora
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Reservaciones", style = MaterialTheme.typography.titleLarge)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(reservations) { reservation ->
                // Se formatean la fecha y la hora
                val formattedDate = reservation.datetime.format(dateFormatter)
                val formattedTime = reservation.datetime.format(timeFormatter)
                val status = if (reservation.isCompleted) "Completed" else "Pending"
                ReservationCard(
                    date = formattedDate,
                    time = formattedTime,
                    guests = reservation.numberCommensals,
                    status = status,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReservationsScreenPreview() {
    // Datos de ejemplo para la vista previa
    val sampleReservations = listOf(
        ReservationDomain(
            id = "1",
            datetime = LocalDateTime.now().plusDays(1),
            numberCommensals = 4,
            isCompleted = false
        ),
        ReservationDomain(
            id = "2",
            datetime = LocalDateTime.now().plusDays(2),
            numberCommensals = 2,
            isCompleted = true,
        ),
        ReservationDomain(
            id = "3",
            datetime = LocalDateTime.now().plusDays(3),
            numberCommensals = 3,
            isCompleted = false,
        )
    )

    MaterialTheme {
        ReservationsScreen(reservations = sampleReservations)
    }
}