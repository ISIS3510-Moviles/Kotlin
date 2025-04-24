package com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
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
import java.time.format.DateTimeFormatter
import java.util.*

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun BookTableSection(
    authViewModel: AuthViewModel,
    restaurant: RestaurantDomain,
    restaurantDetailViewModel: RestaurantDetailViewModel
) {
    val user by authViewModel.user.collectAsState()
    val analytics = Firebase.analytics
    val isUserLoggedIn = authViewModel.user.value != null

    var selectedDate by remember { mutableStateOf("") }
    var selectedHour by remember { mutableStateOf("") }
    var showHourDropdown by remember { mutableStateOf(false) }
    var comensals by remember { mutableStateOf(1) }
    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val primaryBlue = Color(0xFF1565C0)

    // Formateadores:
    val displayDateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    val jsonDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE             // yyyy-MM-dd
    val displayTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
    val jsonTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")       // 24h with leading zero

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val picked = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            if (picked.before(calendar)) {
                errorMessage = "You can't select a past date"
            } else {
                selectedDate = displayDateFormatter.format(
                    LocalDate.of(year, month + 1, dayOfMonth)
                )
                errorMessage = ""
                // reset hour when date changes
                selectedHour = ""
            }
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply { datePicker.minDate = calendar.timeInMillis }

    // Horas disponibles, filtradas si la fecha es hoy
    val availableHours = (7..23).map { it.toString() + ":00" }
    val filteredHours by remember(selectedDate) {
        derivedStateOf {
            if (selectedDate.isNotEmpty()) {
                val pickedDate = LocalDate.parse(selectedDate, displayDateFormatter)
                if (pickedDate.isEqual(LocalDate.now())) {
                    availableHours.filter { hour ->
                        val time = LocalTime.parse(hour, displayTimeFormatter)
                        time.isAfter(LocalTime.now())
                    }
                } else availableHours
            } else emptyList()
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date picker
            Text("Date:", fontWeight = FontWeight.Bold, color = primaryBlue, fontSize = 16.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, primaryBlue, RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .clickable { datePickerDialog.show() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(text = if (selectedDate.isNotEmpty()) selectedDate else "MM/DD/YYYY")
            }

            // Hour dropdown
            Text("Hour", fontWeight = FontWeight.Bold, color = primaryBlue, fontSize = 16.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, primaryBlue, RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .clickable(enabled = selectedDate.isNotEmpty()) { showHourDropdown = true }
            ) {
                Text(text = if (selectedHour.isNotEmpty()) selectedHour else "Select hour")
                DropdownMenu(
                    expanded = showHourDropdown,
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

            // Comensales
            Text("Comensals", fontWeight = FontWeight.Bold, color = primaryBlue, fontSize = 16.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, primaryBlue, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (comensals > 1) comensals-- }) {
                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primaryBlue)
                }
                Text(comensals.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { comensals++ }) {
                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primaryBlue)
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    // Validación
                    val validationError = when {
                        !isUserLoggedIn       -> "You must be logged in to book"
                        selectedDate.isEmpty()  -> "Please select a date"
                        selectedHour.isEmpty()  -> "Please select an hour"
                        else                     -> null
                    }
                    if (validationError != null) {
                        errorMessage = validationError
                        return@Button
                    }

                    // Preparar formatos para JSON
                    val jsonDate = LocalDate.parse(selectedDate, displayDateFormatter)
                        .format(jsonDateFormatter)
                    val jsonTime = LocalTime.parse(selectedHour, displayTimeFormatter)
                        .format(jsonTimeFormatter)

                    // Ejecutar reserva
                    errorMessage = ""
                    analytics.logEvent("restaurant_reservation_used") {
                        param("date", jsonDate)
                        param("time", jsonTime)
                    }
                    restaurantDetailViewModel.createReservation(
                        ReservationDomain(
                            id = "",
                            restaurantId = restaurant.id,
                            userId = user!!.id,
                            datetime = jsonDate,
                            time = jsonTime,
                            numberCommensals = comensals,
                            isCompleted = false,
                            hasBeenCancelled = false
                        ),
                        authViewModel
                    )

                    // Mostrar confirmación y resetear campos
                    Toast.makeText(context, "Reservation confirmed!", Toast.LENGTH_SHORT).show()
                    selectedDate = ""
                    selectedHour = ""
                    comensals = 1
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Book", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
