package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.campusbites.R
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.presentation.ui.viewmodels.ReservationsViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(
    navController: NavHostController,
    reservationsViewModel: ReservationsViewModel = hiltViewModel()
) {
    val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val displayTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Reservas en vivo
    val reservations by reservationsViewModel.reservations.collectAsState()

    // Estado de red y eventos UI
    val networkAvailable by reservationsViewModel.networkAvailable.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(reservationsViewModel.uiEvent) {
        reservationsViewModel.uiEvent.collect { event ->
            when (event) {
                is ReservationsViewModel.UIEvent.ShowMessage ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservations", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- BANNER OFFLINE ---
            if (!networkAvailable) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Offline: Cancellations will be queued and sent when you return online.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // --- LISTADO DE RESERVAS ---
            val now = LocalDateTime.now()
            val (cancelled, nonCancelled) = reservations.partition { it.hasBeenCancelled == true }
            val (upcoming, past) = nonCancelled.partition {
                val d = LocalDate.parse(it.datetime)
                val t = LocalTime.parse(it.time, DateTimeFormatter.ofPattern("HH:mm"))
                LocalDateTime.of(d, t).isAfter(now)
            }

            val sortedUpcoming = upcoming.sortedBy {
                val d = LocalDate.parse(it.datetime)
                val t = LocalTime.parse(it.time, DateTimeFormatter.ofPattern("HH:mm"))
                LocalDateTime.of(d, t)
            }
            val sortedPast = past.sortedByDescending {
                val d = LocalDate.parse(it.datetime)
                val t = LocalTime.parse(it.time, DateTimeFormatter.ofPattern("HH:mm"))
                LocalDateTime.of(d, t)
            }
            val sortedCancelled = cancelled.sortedByDescending {
                val d = LocalDate.parse(it.datetime)
                val t = LocalTime.parse(it.time, DateTimeFormatter.ofPattern("HH:mm"))
                LocalDateTime.of(d, t)
            }

            if (reservations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.there_are_not_reservations),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Canceladas
                    if (sortedCancelled.isNotEmpty()) {
                        item {
                            Text(
                                text = "Cancelled Reservations",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(sortedCancelled) { res ->
                            ReservationCardWithCancel(
                                date = LocalDate.parse(res.datetime).format(displayDateFormatter),
                                time = LocalTime.parse(res.time, DateTimeFormatter.ofPattern("HH:mm"))
                                    .format(displayTimeFormatter),
                                guests = res.numberCommensals,
                                status = "Canceled",
                                modifier = Modifier.fillMaxWidth(),
                                onCancelClick = null
                            )
                        }
                    }

                    // Próximas
                    if (sortedUpcoming.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.upcoming_reservations),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(sortedUpcoming) { res ->
                            ReservationCardWithCancel(
                                date = LocalDate.parse(res.datetime).format(displayDateFormatter),
                                time = LocalTime.parse(res.time, DateTimeFormatter.ofPattern("HH:mm"))
                                    .format(displayTimeFormatter),
                                guests = res.numberCommensals,
                                status = if (res.isCompleted) "Completed" else "Pending",
                                modifier = Modifier.fillMaxWidth(),
                                onCancelClick = {
                                    reservationsViewModel.cancelReservation(res.id)
                                }
                            )
                        }
                    }

                    // Pasadas
                    if (sortedPast.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.past_reservations),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(sortedPast) { res ->
                            ReservationCardWithCancel(
                                date = LocalDate.parse(res.datetime).format(displayDateFormatter),
                                time = LocalTime.parse(res.time, DateTimeFormatter.ofPattern("HH:mm"))
                                    .format(displayTimeFormatter),
                                guests = res.numberCommensals,
                                status = "Completed",
                                modifier = Modifier.fillMaxWidth(),
                                onCancelClick = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReservationCardWithCancel(
    date: String,
    time: String,
    guests: Int,
    status: String,
    modifier: Modifier = Modifier,
    onCancelClick: (() -> Unit)? = null
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row (verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, Modifier.padding(end = 8.dp))
                    Text("Date: $date", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row (verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, Modifier.padding(end = 8.dp))
                    Text("Hour: $time", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row (verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Face, contentDescription = null, Modifier.padding(end = 8.dp))
                    Text("Participants: $guests", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row (verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, Modifier.padding(end = 8.dp))
                    Text("Status: $status", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (onCancelClick != null) {
                IconButton(onClick = onCancelClick) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar reserva", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
