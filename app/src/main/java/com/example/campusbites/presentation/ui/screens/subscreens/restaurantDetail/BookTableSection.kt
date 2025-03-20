package com.example.campusbites.presentation.ui.screens.subscreens.restaurantDetail

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
import java.util.*

@Composable
fun BookTableSection() {
    var selectedDate by remember { mutableStateOf("") }
    var selectedHour by remember { mutableStateOf("") }
    var showHourDropdown by remember { mutableStateOf(false) }
    var comensals by remember { mutableStateOf(1) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            selectedDate = "${month + 1}/$dayOfMonth/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

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
            // Date Picker
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

            // Hour Picker
            Text("Hour", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 16.sp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFF1B5E20), RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .clickable { showHourDropdown = true }
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

            // Comensals Selector
            Text("Comensals", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 16.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFF1B5E20), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (comensals > 1) comensals-- } // Evita que sea menor que 1
                ) {
                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                }

                Text(text = comensals.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                IconButton(
                    onClick = { comensals++ }
                ) {
                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                }
            }

            // Book Button
            Button(
                onClick = { /* Acción de reserva */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Book", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
