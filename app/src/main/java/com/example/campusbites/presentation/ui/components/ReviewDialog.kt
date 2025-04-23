package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun ReviewDialog(
    onDismiss: () -> Unit,
    onSend: (rating: Int, message: String) -> Unit
) {
    var rating by remember { mutableStateOf(3f) }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = message.isNotBlank(),
                onClick = { onSend(rating.roundToInt(), message) }
            ) { Text("Post") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Write your review") },
        text = {
            Column {
                // Slider para la puntuación
                Text("Rating: ${rating.roundToInt()}")
                Slider(
                    value = rating,
                    onValueChange = { rating = it },
                    steps = 3,
                    valueRange = 1f..5f
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Your experience…") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}