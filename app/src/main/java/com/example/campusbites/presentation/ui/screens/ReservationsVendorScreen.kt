package com.example.campusbites.presentation.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.campusbites.R
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.presentation.ui.components.ReservationVendorCard
//import com.example.campusbites.presentation.ui.components.vendor.ReservationVendorCard
import com.example.campusbites.presentation.ui.viewmodels.ReservationsVendorViewModel
//import com.example.campusbites.presentation.ui.viewmodels.vendor.ReservationsVendorViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsVendorScreen(
    viewModel: ReservationsVendorViewModel = hiltViewModel()
) {
    val reservationsWithUsers by viewModel.reservationsWithUserDetails.collectAsState()
    val networkAvailable by viewModel.networkAvailable.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Log para ver el estado inicial de las reservaciones
    LaunchedEffect(reservationsWithUsers) {
        reservationsWithUsers.forEach {
            Log.d("ReservationsVendorScreen", "Res: ${it.reservation.id}, Completed: ${it.reservation.isCompleted}, Cancelled: ${it.reservation.hasBeenCancelled}, DateTime: ${it.reservation.datetime} ${it.reservation.time}")
        }
    }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ReservationsVendorViewModel.UIEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Restaurant reservations") }, // Usar string resource
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
            if (!networkAvailable) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your are currently offline. Please check your internet connection.", // Usar string resource
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (reservationsWithUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "There are no reservations now.", // Usar string resource
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                val now = LocalDateTime.now()

                // 1. Filtrar canceladas primero
                val (cancelled, activeReservations) = reservationsWithUsers.partition {
                    it.reservation.hasBeenCancelled
                }

                // 2. De las activas, separar las que aún son futuras y están pendientes de acción (no completadas)
                val (pendingFuture, potentiallyPastOrCompleted) = activeReservations.partition { item ->
                    val resDateTime = LocalDateTime.of(
                        Instant.parse(item.reservation.datetime).atZone(ZoneId.systemDefault()).toLocalDate(),
                        LocalTime.parse(item.reservation.time, DateTimeFormatter.ofPattern("HH:mm"))
                    )
                    // Condición para PENDING: No completada AÚN Y es FUTURA
                    !item.reservation.isCompleted && resDateTime.isAfter(now)
                }

                // 3. De las restantes (potentiallyPastOrCompleted), separar las que YA fueron completadas por el vendor
                val (completed, pastAndNotCompleted) = potentiallyPastOrCompleted.partition {
                    it.reservation.isCompleted
                }

                // 4. De las que son pasadas y no completadas, estas son las "Missed"
                // No es necesario un 'partition' aquí, 'pastAndNotCompleted' ya son las 'missed'
                // porque ya filtramos las futuras pendientes y las completadas.
                // Solo nos aseguramos que realmente sean pasadas.
                val missed = pastAndNotCompleted.filter { item ->
                    val resDateTime = LocalDateTime.of(
                        Instant.parse(item.reservation.datetime).atZone(ZoneId.systemDefault()).toLocalDate(),
                        LocalTime.parse(item.reservation.time, DateTimeFormatter.ofPattern("HH:mm"))
                    )
                    resDateTime.isBefore(now) // Y no está completada (ya filtrado) y no cancelada (ya filtrado)
                }


                // Ordenar cada grupo
                val sortedPending = pendingFuture.sortedBy { Instant.parse(it.reservation.datetime) }
                val sortedCompleted = completed.sortedByDescending { Instant.parse(it.reservation.datetime) }
                val sortedMissed = missed.sortedByDescending { Instant.parse(it.reservation.datetime) }
                val sortedCancelled = cancelled.sortedByDescending { Instant.parse(it.reservation.datetime) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    // Pendientes (Futuras, no completadas, no canceladas)
                    if (sortedPending.isNotEmpty()) {
                        item { ListHeader("Upcoming reservations") }
                        items(sortedPending, key = { it.reservation.id }) { item ->
                            ReservationVendorCard(
                                userFullName = item.userName,
                                date = Instant.parse(item.reservation.datetime).atZone(ZoneId.systemDefault()).toLocalDate().format(viewModel.displayDateFormatter),
                                time = LocalTime.parse(item.reservation.time, DateTimeFormatter.ofPattern("HH:mm")).format(viewModel.displayTimeFormatter),
                                guests = item.reservation.numberCommensals,
                                status = "Pending",
                                onMarkAsCompleted = { viewModel.markAsCompleted(item.reservation.id) },
                                onCancelReservation = { viewModel.cancelReservation(item.reservation.id) }
                            )
                        }
                    }

                    // Completadas (Confirmadas por el vendor)
                    if (sortedCompleted.isNotEmpty()) {
                        item { ListHeader(title = "Confirmed reservations") }
                        items(sortedCompleted, key = { it.reservation.id }) { item ->
                            ReservationVendorCard(
                                userFullName = item.userName,
                                date = Instant.parse(item.reservation.datetime).atZone(ZoneId.systemDefault()).toLocalDate().format(viewModel.displayDateFormatter),
                                time = LocalTime.parse(item.reservation.time, DateTimeFormatter.ofPattern("HH:mm")).format(viewModel.displayTimeFormatter),
                                guests = item.reservation.numberCommensals,
                                status = "Confirmed", // O "Completed"
                                onMarkAsCompleted = null, // Ya no se puede marcar
                                onCancelReservation = null  // Ya no se puede cancelar
                            )
                        }
                    }

                    // Missed (Pasadas, no completadas por vendor, no canceladas)
                    if (sortedMissed.isNotEmpty()) {
                        item { ListHeader(title = "Missed reservations") } // Nuevo string
                        items(sortedMissed, key = { it.reservation.id }) { item ->
                            ReservationVendorCard(
                                userFullName = item.userName,
                                date = Instant.parse(item.reservation.datetime).atZone(ZoneId.systemDefault()).toLocalDate().format(viewModel.displayDateFormatter),
                                time = LocalTime.parse(item.reservation.time, DateTimeFormatter.ofPattern("HH:mm")).format(viewModel.displayTimeFormatter),
                                guests = item.reservation.numberCommensals,
                                status = "Missed",
                                onMarkAsCompleted = null, // No se puede marcar
                                onCancelReservation = null  // No se puede cancelar
                            )
                        }
                    }

                    // Canceladas
                    if (sortedCancelled.isNotEmpty()) {
                        item { ListHeader(title = "Cancelled") }
                        items(sortedCancelled, key = { it.reservation.id }) { item ->
                            ReservationVendorCard(
                                userFullName = item.userName,
                                date = Instant.parse(item.reservation.datetime).atZone(ZoneId.systemDefault()).toLocalDate().format(viewModel.displayDateFormatter),
                                time = LocalTime.parse(item.reservation.time, DateTimeFormatter.ofPattern("HH:mm")).format(viewModel.displayTimeFormatter),
                                guests = item.reservation.numberCommensals,
                                status = "Cancelled",
                                onMarkAsCompleted = null,
                                onCancelReservation = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    )
}

