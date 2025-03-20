package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusbites.domain.model.CommentDomain

@Composable
fun CommentCard(comment: CommentDomain) {
    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF0A2540)), // Color del borde
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Nombre y fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = comment.author.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0A2540)
                )
                comment.datetime?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = Color(0xFF0A2540)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Rating con estrellas
            StarRating(rating = comment.rating)

            Spacer(modifier = Modifier.height(8.dp))

            // Mensaje del comentario
            Text(
                text = comment.message,
                fontSize = 14.sp,
                color = Color(0xFF0A2540)
            )
        }
    }
}
