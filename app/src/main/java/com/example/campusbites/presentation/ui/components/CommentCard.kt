package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusbites.domain.model.CommentDomain
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun CommentCard(comment: CommentDomain) {
    val borderColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Nombre + Fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = comment.author.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                comment.datetime?.let {
                    val parsedDate = try {
                        ZonedDateTime.parse(it)
                    } catch (e: Exception) {
                        null
                    }
                    val formattedDate = parsedDate?.format(
                        DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a")
                    ) ?: it

                    Text(
                        text = formattedDate,
                        fontSize = 13.sp,
                        color = secondaryTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            StarRating(rating = comment.rating)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = comment.message,
                fontSize = 15.sp,
                color = textColor
            )
        }
    }
}
