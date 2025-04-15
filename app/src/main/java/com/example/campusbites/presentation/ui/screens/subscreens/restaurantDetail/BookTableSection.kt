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
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import java.util.*

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun BookTableSection(authViewModel: AuthViewModel) {
    val isUserLoggedIn = authViewModel.user.value != null

    var selectedDate by remember { mutableStateOf("") }
    var selectedHour by remember { mutableStateOf("") }
    var showHourDropdown by remember { mutableStateOf(false) }
    var comensals by remember { mutableStateOf(1) }

    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

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
            Text("Date:", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 16.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFF1B5E20), RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .clickable { datePickerDialog.show() },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(text = if (selectedDate.isNotEmpty()) selectedDate else "MM/DD/YYYY")
            }

            Text("Hour", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 16.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFF1B5E20), RoundedCornerShape(8.dp))
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

            Text("Comensals", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 16.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFF1B5E20), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (comensals > 1) comensals-- }) {
                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                }

                Text(text = comensals.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                IconButton(onClick = { comensals++ }) {
                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
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
                    errorMessage = when {
                        !isUserLoggedIn -> "You must be logged in to book"
                        selectedDate.isEmpty() -> "Please select a date"
                        selectedHour.isEmpty() -> "Please select an hour"
                        else -> {
                            // Ejecutar acción de reserva aquí
                            ""
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Book", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
