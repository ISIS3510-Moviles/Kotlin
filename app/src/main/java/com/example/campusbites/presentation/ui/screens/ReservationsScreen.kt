package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.campusbites.R
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import com.example.campusbites.presentation.ui.viewmodels.RestaurantDetailViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(
    authViewModel: AuthViewModel,
    navController: NavHostController,
) {
    val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val displayTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val viewModel: RestaurantDetailViewModel = hiltViewModel()
    val user by authViewModel.user.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservaciones", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val reservations = user?.reservationsDomain.orEmpty().map { res ->
            val date = LocalDate.parse(res.datetime, DateTimeFormatter.ISO_LOCAL_DATE)
            val time = LocalTime.parse(res.time, DateTimeFormatter.ofPattern("HH:mm"))
            val dateTime = LocalDateTime.of(date, time)
            res to dateTime
        }
        val now = LocalDateTime.now()
        val (past, upcoming) = reservations.partition { it.second.isBefore(now) }
        val sortedUpcoming = upcoming.sortedBy { it.second }

        if (reservations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
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
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (sortedUpcoming.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.upcoming_reservations),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(sortedUpcoming) { (res, dt) ->
                        ReservationCardWithCancel(
                            date = dt.format(displayDateFormatter),
                            time = dt.format(displayTimeFormatter),
                            guests = res.numberCommensals,
                            status = if (res.isCompleted) "Completed" else "Pending",
                            modifier = Modifier.fillMaxWidth(),
                            onCancelClick = {
                                coroutineScope.launch {
                                    viewModel.cancelReservation(res.id, authViewModel)
                                }
                            }
                        )
                    }
                }
                if (past.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.past_reservations),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(past) { (res, dt) ->
                        ReservationCardWithCancel(
                            date = dt.format(displayDateFormatter),
                            time = dt.format(displayTimeFormatter),
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
                Text(text = "Fecha: $date", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Hora: $time", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Comensales: $guests", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Estado: $status", style = MaterialTheme.typography.bodyMedium)
            }

            if (onCancelClick != null) {
                IconButton(onClick = onCancelClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelar reserva",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}