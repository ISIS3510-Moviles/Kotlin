package com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.util.Log
import android.widget.DatePicker
// import android.widget.Toast // Ya no se usa directamente para feedback de reserva
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusbites.domain.model.ReservationDomain
import com.example.campusbites.domain.model.RestaurantDomain
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import com.example.campusbites.presentation.ui.viewmodels.RestaurantDetailViewModel
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlinx.coroutines.flow.collectLatest
import java.time.ZonedDateTime

@SuppressLint("StateFlowValueCalledInComposition") // Revisar si hay accesos directos a .value en Composición
@Composable
fun BookTableSection(
    authViewModel: AuthViewModel,
    restaurant: RestaurantDomain, // Asumimos que restaurant no es null cuando esta sección se muestra
    restaurantDetailViewModel: RestaurantDetailViewModel
) {
    val user by authViewModel.user.collectAsState()
    val analytics = Firebase.analytics
    // isUserLoggedIn se usa directamente en la validación

    var selectedDate by remember { mutableStateOf("") }
    var selectedHour by remember { mutableStateOf("") } // Formato "HH:mm"
    var showHourDropdown by remember { mutableStateOf(false) }
    var comensals by remember { mutableStateOf(1) }
    var errorMessageFromValidation by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current // Usado por DatePickerDialog
    val primaryBlue = Color(0xFF1565C0) // Definición de color

    // Formateadores de fecha y hora
    val displayDateFormatter = remember { DateTimeFormatter.ofPattern("MM/dd/yyyy") }
    val timeParser = remember { DateTimeFormatter.ofPattern("HH:mm") } // Para parsear horas como "07:00"
    val isoInstantFormatter = remember { DateTimeFormatter.ISO_INSTANT }

    // --- Lógica del DatePickerDialog ---
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = remember(context, currentYear, currentMonth, currentDay) { // Recrear solo si el contexto o fecha actual cambia
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val pickedLocalDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (pickedLocalDate.isBefore(LocalDate.now())) {
                    errorMessageFromValidation = "You can't select a past date."
                } else {
                    selectedDate = pickedLocalDate.format(displayDateFormatter)
                    errorMessageFromValidation = null // Limpiar error
                    selectedHour = "" // Resetear hora al cambiar fecha
                }
            },
            currentYear,
            currentMonth,
            currentDay
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000 // Permitir seleccionar el día de hoy
        }
    }

    // --- Lógica de Horas Disponibles ---
    val availableHours = remember { (7..23).map { String.format("%02d:00", it) } } // Formato HH:mm

    val filteredHours by remember(selectedDate, availableHours) {
        derivedStateOf {
            if (selectedDate.isNotEmpty()) {
                try {
                    val pickedDate = LocalDate.parse(selectedDate, displayDateFormatter)
                    if (pickedDate.isEqual(LocalDate.now())) {
                        val now = LocalTime.now()
                        availableHours.filter { hourString ->
                            LocalTime.parse(hourString, timeParser).isAfter(now)
                        }
                    } else {
                        availableHours
                    }
                } catch (e: Exception) {
                    // En caso de error de parseo de fecha (improbable con DatePickerDialog)
                    Log.e("BookTableSection", "Error parsing selectedDate: $selectedDate", e)
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    // --- Estados y Eventos del ViewModel ---
    val uiState by restaurantDetailViewModel.uiState.collectAsState()
    // val isOnline = uiState.isOnline // Se puede usar para feedback visual si es necesario, pero el botón es siempre enabled

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { // Usar una clave constante si no depende de nada más para relanzarse
        restaurantDetailViewModel.uiEventFlow.collectLatest { event ->
            when (event) {
                is RestaurantDetailViewModel.UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
                // Añadir más casos si tienes otros UiEvents, ej. UiEvent.ReservationSuccess
            }
        }
    }

    // --- UI de la Sección ---
    Box { // Contenedor para el Card y el SnackbarHost
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selección de Fecha
                Text("Date:", fontWeight = FontWeight.Bold, color = primaryBlue, fontSize = 16.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp) // Altura fija para consistencia
                        .border(1.dp, primaryBlue, RoundedCornerShape(8.dp))
                        .clickable { datePickerDialog.show() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (selectedDate.isNotEmpty()) selectedDate else "MM/DD/YYYY",
                        color = if (selectedDate.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Selección de Hora
                Text("Hour:", fontWeight = FontWeight.Bold, color = primaryBlue, fontSize = 16.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(1.dp, primaryBlue, RoundedCornerShape(8.dp))
                        .clickable(enabled = selectedDate.isNotEmpty()) {
                            if (selectedDate.isNotEmpty()) {
                                if (filteredHours.isEmpty()) {
                                    errorMessageFromValidation = "No available hours for the selected date."
                                    showHourDropdown = false
                                } else {
                                    errorMessageFromValidation = null // Limpiar error si hay horas
                                    showHourDropdown = true
                                }
                            }
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (selectedHour.isNotEmpty()) selectedHour else "Select hour",
                        color = if (selectedHour.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    DropdownMenu(
                        expanded = showHourDropdown && filteredHours.isNotEmpty(),
                        onDismissRequest = { showHourDropdown = false }
                    ) {
                        filteredHours.forEach { hour ->
                            DropdownMenuItem(
                                text = { Text(hour) },
                                onClick = {
                                    selectedHour = hour
                                    showHourDropdown = false
                                }
                            )
                        }
                    }
                }

                // Selección de Comensales
                Text("Comensals:", fontWeight = FontWeight.Bold, color = primaryBlue, fontSize = 16.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(1.dp, primaryBlue, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp), // Padding ajustado para IconButtons
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (comensals > 1) comensals-- }) {
                        Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = primaryBlue)
                    }
                    Text(comensals.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { if (comensals < 20) comensals++ }) { // Límite superior razonable
                        Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = primaryBlue)
                    }
                }

                // Mensaje de Error de Validación
                errorMessageFromValidation?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Botón de Reservar
                Button(
                    onClick = {
                        val currentUser = user // Capturar valor actual para la validación
                        val validationError = when {
                            currentUser == null -> "You must be logged in to book."
                            selectedDate.isEmpty() -> "Please select a date."
                            selectedHour.isEmpty() -> "Please select an hour."
                            else -> null
                        }

                        if (validationError != null) {
                            errorMessageFromValidation = validationError
                            return@Button
                        }
                        errorMessageFromValidation = null // Limpiar errores de validación

                        try {
                            val localDate = LocalDate.parse(selectedDate, displayDateFormatter)
                            val localTime = LocalTime.parse(selectedHour, timeParser)

                            // Combinar en OffsetDateTime en UTC, considerando la zona horaria del sistema para la entrada.
                            val systemDefaultZone = ZoneOffset.systemDefault()
                            val utcDateTime: ZonedDateTime? = localDate.atTime(localTime)
                                .atZone(systemDefaultZone)      // Interpretar como fecha/hora en la zona del sistema
                                .withZoneSameInstant(ZoneOffset.UTC) // Convertir a UTC

                            val jsonDateTime = utcDateTime!!.format(isoInstantFormatter) // Formato ISO_INSTANT para UTC
                            val jsonTime = localTime.format(timeParser) // Formato HH:mm

                            analytics.logEvent("restaurant_reservation_attempt") {
                                param("datetime_utc", jsonDateTime)
                                param("time_local", jsonTime)
                                param("restaurant_id", restaurant.id)
                                param("user_id", currentUser!!.id) // currentUser ya no es null aquí
                                param("number_comensals", comensals.toLong())
                                param("is_online", uiState.isOnline.toString())
                            }

                            restaurantDetailViewModel.createReservation(
                                ReservationDomain(
                                    id = "", // Será asignado por el backend o la lógica de encolamiento si es necesario
                                    restaurantId = restaurant.id,
                                    userId = currentUser!!.id,
                                    datetime = jsonDateTime,
                                    time = jsonTime,
                                    numberCommensals = comensals,
                                    isCompleted = false,
                                    hasBeenCancelled = false
                                )
                            )

                            // Resetear campos después de iniciar la acción de reserva
                            selectedDate = ""
                            selectedHour = ""
                            comensals = 1

                        } catch (e: Exception) {
                            Log.e("BookTableSection", "Error preparing reservation data: ${e.message}", e)
                            errorMessageFromValidation = "Error with selected date/time. Please try again."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = true // El botón siempre está habilitado; el ViewModel maneja online/offline.
                    // El feedback visual de offline general vendría de un banner en la app.
                ) {
                    Text("Book Table", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // SnackbarHost para mostrar mensajes (ej. "Reserva encolada", "Reserva confirmada")
        // Se alinea en la parte inferior del Box contenedor.
        // Si esta sección es parte de una pantalla más grande con su propio Scaffold,
        // considera mover el SnackbarHost a ese Scaffold principal.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}