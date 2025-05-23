package com.example.campusbites.presentation.ui.screens

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
    // navController: NavHostController, // Descomentar si se necesita navegación
    viewModel: ReservationsVendorViewModel = hiltViewModel()
) {
    val reservationsWithUsers by viewModel.reservationsWithUserDetails.collectAsState()
    val networkAvailable by viewModel.networkAvailable.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
                title = { Text("Restaurant Reservations", style = MaterialTheme.typography.titleLarge) },
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
                        text = "You are currently offline.", // Necesitarás añadir este string
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
                        text = "There are not reservations", // Necesitarás añadir este string
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                // Clasificar reservas
                val now = LocalDateTime.now()
                val (pending, pastOrCancelled) = reservationsWithUsers.partition {
                    val resDateTime = LocalDateTime.of(
                        Instant.parse(it.reservation.datetime).atZone(ZoneId.systemDefault()).toLocalDate(),
                        LocalTime.parse(it.reservation.time, DateTimeFormatter.ofPattern("HH:mm"))
                    )
                    !it.reservation.hasBeenCancelled && !it.reservation.isCompleted && resDateTime.isAfter(now)
                }
                val (completed, cancelled) = pastOrCancelled.partition { it.reservation.isCompleted && !it.reservation.hasBeenCancelled }

                // Ordenar cada grupo
                val sortedPending = pending.sortedBy { Instant.parse(it.reservation.datetime) }
                val sortedCompleted = completed.sortedByDescending { Instant.parse(it.reservation.datetime) }
                val sortedCancelled = pastOrCancelled
                    .filter { it.reservation.hasBeenCancelled }
                    .sortedByDescending { Instant.parse(it.reservation.datetime) }


                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    if (sortedPending.isNotEmpty()) {
                        item {
                            ListHeader(title = "Pending reservations") // Añadir string
                        }
                        items(sortedPending, key = { it.reservation.id }) { item ->
                            ReservationVendorCard(
                                userFullName = item.userName,
                                date = Instant.parse(item.reservation.datetime)
                                    .atZone(ZoneId.systemDefault()).toLocalDate()
                                    .format(viewModel.displayDateFormatter),
                                time = LocalTime.parse(item.reservation.time, DateTimeFormatter.ofPattern("HH:mm"))
                                    .format(viewModel.displayTimeFormatter),
                                guests = item.reservation.numberCommensals,
                                status = "Pending",
                                onMarkAsCompleted = { viewModel.markAsCompleted(item.reservation.id) },
                                onCancelReservation = { viewModel.cancelReservation(item.reservation.id) }
                            )
                        }
                    }

                    if (sortedCompleted.isNotEmpty()) {
                        item {
                            ListHeader(title = "Completed reservations") // Añadir string
                        }
                        items(sortedCompleted,  key = { it.reservation.id }) { item ->
                            ReservationVendorCard(
                                userFullName = item.userName,
                                date = Instant.parse(item.reservation.datetime)
                                    .atZone(ZoneId.systemDefault()).toLocalDate()
                                    .format(viewModel.displayDateFormatter),
                                time = LocalTime.parse(item.reservation.time, DateTimeFormatter.ofPattern("HH:mm"))
                                    .format(viewModel.displayTimeFormatter),
                                guests = item.reservation.numberCommensals,
                                status = "Completed",
                                onMarkAsCompleted = null,
                                onCancelReservation = null
                            )
                        }
                    }

                    if (sortedCancelled.isNotEmpty()) {
                        item {
                            ListHeader(title = "Cancelled reservations") // Añadir string
                        }
                        items(sortedCancelled,  key = { it.reservation.id }) { item ->
                            ReservationVendorCard(
                                userFullName = item.userName,
                                date = Instant.parse(item.reservation.datetime)
                                    .atZone(ZoneId.systemDefault()).toLocalDate()
                                    .format(viewModel.displayDateFormatter),
                                time = LocalTime.parse(item.reservation.time, DateTimeFormatter.ofPattern("HH:mm"))
                                    .format(viewModel.displayTimeFormatter),
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

