package com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.widget.DatePicker
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
import java.util.*

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase


@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun BookTableSection(authViewModel: AuthViewModel, restaurant: RestaurantDomain, restaurantDetailViewModel: RestaurantDetailViewModel) {
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

    val primaryBlue = Color(0xFF1565C0) // Azul consistente con el otro componente

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val picked = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            if (picked.before(calendar)) {
                errorMessage = "You can't select a past date"
            } else {
                selectedDate = "${month + 1}/$dayOfMonth/$year"
                errorMessage = ""
            }
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.minDate = calendar.timeInMillis
    }

    val availableHours = listOf("7:00", "8:00", "9:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00")

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

            Text("Hour", fontWeight = FontWeight.Bold, color = primaryBlue, fontSize = 16.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, primaryBlue, RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .clickable(enabled = selectedDate.isNotEmpty()) {
                        showHourDropdown = true
                    }
            ) {
                Text(text = if (selectedHour.isNotEmpty()) selectedHour else "Select hour")
                DropdownMenu(
                    expanded = showHourDropdown,
                    onDismissRequest = { showHourDropdown = false }
                ) {
                    availableHours.forEach { hour ->
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

                Text(text = comensals.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)

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
                    // 1) Validación
                    val validationError = when {
                        !isUserLoggedIn     -> "You must be logged in to book"
                        selectedDate.isEmpty()  -> "Please select a date"
                        selectedHour.isEmpty()  -> "Please select an hour"
                        else                     -> null
                    }

                    if (validationError != null) {
                        // Si hay error, lo muestro
                        errorMessage = validationError
                        return@Button
                    }

                    // 2) No hay error → limpio mensaje y ejecuto la reserva
                    errorMessage = ""
                    analytics.logEvent("restaurant_reservation_used") {
                        param("date", selectedDate)
                        param("hour", selectedHour)
                    }

                    // Combinas fecha y hora si quieres un solo campo datetime,
                    // o los mantienes separados según tu modelo
                    restaurantDetailViewModel.createReservation(
                        ReservationDomain(
                            id = "",
                            restaurantId = restaurant.id,
                            userId = user!!.id,
                            datetime = "$selectedDate $selectedHour",
                            time = selectedHour,
                            numberCommensals = comensals,
                            isCompleted = false
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Book", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

