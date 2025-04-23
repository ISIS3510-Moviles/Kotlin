package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            // Header: Solo el nombre
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = comment.author.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            StarRating(rating = comment.rating)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = comment.message,
                fontSize = 15.sp,
                color = textColor
            )

            // Fecha en la parte inferior derecha
            comment.datetime?.let {
                Spacer(modifier = Modifier.height(12.dp))
                val parsedDate = try {
                    ZonedDateTime.parse(it)
                } catch (e: Exception) {
                    null
                }
                val formattedDate = parsedDate?.format(
                    DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a")
                ) ?: it

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = formattedDate,
                        fontSize = 13.sp,
                        color = secondaryTextColor
                    )
                }
            }
        }
    }
}
