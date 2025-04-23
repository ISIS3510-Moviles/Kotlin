package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.campusbites.domain.model.RestaurantDomain
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RestaurantCard(
    restaurant: RestaurantDomain,
    onRestaurantClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Tags logic
    val allTags = restaurant.foodTags.map { it.name } + restaurant.dietaryTags.map { it.name }
    val initialTags = allTags.take(2)
    val extraTags = allTags.drop(2)

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .width(379.dp)  // full screen width
            .height(320.dp)
            .padding(8.dp)
            .clickable { onRestaurantClick(restaurant.id) }
    ) {
        Column {
            // Image with placeholder and error fallback
            AsyncImage(
                model = restaurant.overviewPhoto,
                contentDescription = "${restaurant.name} photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
                    .padding(12.dp)
            ) {
                Profile(
                    profilePhoto = restaurant.profilePhoto,
                    name = restaurant.name,
                    rating = restaurant.rating,
                    comments = restaurant.commentsIds
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Show up to two tags
                Column {
                    initialTags.forEach { tag ->
                        TagChip(tag = tag)
                    }
                }

                // Show "more" icon only if extraTags exist
                if (extraTags.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "More tags",
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { expanded = true }
                                .padding(4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .width(200.dp)
                                .shadow(8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Text(
                                    text = "All tags",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                extraTags.forEach { tag ->
                                    TagChip(tag = tag)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun Profile(
    profilePhoto: String,
    name: String,
    rating: Double,
    comments: List<String>
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = profilePhoto, // URL de la imagen
            contentDescription = "$name profile Photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape) // O puedes usar RoundedCornerShape(50.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Estrella de calificación",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 4.dp)
                )
                Text(
                    text = String.format(Locale.US, "%.1f", rating),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "(${if (comments.size >= 100) "+99" else comments.size} reviews)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun TagList(
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .wrapContentSize(),
        verticalArrangement = Arrangement.Center
    ) {
        tags.take(1).forEach { tag ->
            TagChip(tag = tag)
        }
    }
}

@Composable
fun TagChip(tag: String, modifier: Modifier = Modifier) {
    Card(
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
            .padding(2.dp)
    ) {
        Text(
            text = "#$tag",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}