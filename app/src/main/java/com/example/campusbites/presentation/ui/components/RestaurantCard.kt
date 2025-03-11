package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.campusbites.domain.model.RestaurantDomain
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RestaurantCard(
    restaurant: RestaurantDomain,
    onRestaurantClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier
            .width(388.dp)
            .wrapContentHeight()
            .padding(4.dp)
            .clickable { onRestaurantClick(restaurant.id.toString()) }
    ) {
        Column {
            AsyncImage(
                model = restaurant.overviewPhoto,
                contentDescription = "${restaurant.name} photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Profile(
                    profilePhoto = restaurant.profilePhoto,
                    name = restaurant.name,
                    distance = restaurant.latitude,
                    rating = restaurant.rating,
                    comments = restaurant.commentsIds
                )

                Spacer(modifier = Modifier.width(8.dp))

                TagChip(
                    tag = restaurant.foodTags.first().name,
                )

                TagChip(
                    tag = restaurant.dietaryTags.first().name,
                )

                // Icono con menú desplegable
                var expanded by remember { mutableStateOf(false) }
                Box {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "More tags",
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { expanded = true }
                            .padding(4.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .width(220.dp)
                            .shadow(elevation = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp)
                    ) {
                        Column(
                            Modifier.padding(6.dp)
                        ) {
                            Text(
                                text = "All tags",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.height(8.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                restaurant.dietaryTags.forEach { tag ->
                                    TagChip(tag = tag.name)
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
    distance: Double,
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
            Text(
                text = "Distance: ${String.format(Locale.US, "%.1f", distance)} mts",
                style = MaterialTheme.typography.bodySmall
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
                    text = String.format(Locale.US, "%.1f", rating) +
                            " (+${if (comments.size >= 100) "+99" else comments.size} opinions)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
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