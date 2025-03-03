package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.campusbites.domain.model.UserProfile

@Composable
fun UserInfoCard(user: UserProfile) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors( containerColor = MaterialTheme.colorScheme.surfaceContainerLow )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Caja para la imagen con borde circular usando primaryContainer (puede ser tu color de marca)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = user.profileImage),
                    contentDescription = "Foto de perfil de ${user.name}",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = user.role,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PreferencesCard(user: UserProfile) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors( containerColor = MaterialTheme.colorScheme.surfaceContainerHigh )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Preferencias",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Divider(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .height(1.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
            Text(
                text = "Preferencias dietéticas: " + user.dietaryPreferences.joinToString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tipos de comida: " + user.favoriteFoodTypes.joinToString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Rango de precios: " + user.preferredPriceRange,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ActivityCard(user: UserProfile) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors( containerColor = MaterialTheme.colorScheme.surfaceContainerLow )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Historial de Actividad",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Divider(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .height(1.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
            Text(
                text = "Restaurantes visitados recientemente:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
            ) {
                items(user.recentlyVisitedRestaurants.size) { index ->
                    ChipItem(label = user.recentlyVisitedRestaurants[index])
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Comentarios:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            user.comments.forEach { comment ->
                Text(
                    text = comment,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Fotos subidas: ${user.uploadedPhotos.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ChipItem(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun AccountSettingsCard(user: UserProfile) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors( containerColor = MaterialTheme.colorScheme.surfaceContainerHigh )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Configuración de Cuenta",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Divider(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .height(1.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
            Text(
                text = "Contacto: ${user.contactInfo}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Privacidad: ${user.privacySettings}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}