package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Un componente que muestra una calificación con estrellas.
 *
 * @param rating Valor actual de la calificación (entre 0 y maxRating)
 * @param maxRating Número máximo de estrellas a mostrar (predeterminado: 5)
 * @param starSize Tamaño de cada estrella (predeterminado: 24.dp)
 * @param activeColor Color de las estrellas activas (predeterminado: Dorado)
 * @param inactiveColor Color de las estrellas inactivas (predeterminado: Gris)
 * @param modifier Modificador opcional para aplicar al componente
 */
@Composable
fun StarRating(
    rating: Int,
    maxRating: Int = 5,
    starSize: Int = 24,
    activeColor: Color = Color(0xFFFFD700), // Gold color
    inactiveColor: Color = Color.Gray,
    modifier: Modifier = Modifier
) {
    val ratingDescription = "$rating de $maxRating estrellas"

    Row(
        modifier = modifier
            .height(starSize.dp)
            .semantics { contentDescription = ratingDescription }
    ) {
        repeat(maxRating) { index ->
            val isFilled = index < rating
            val starColor = if (isFilled) activeColor else inactiveColor
            val icon = if (isFilled) Icons.Filled.Star else Icons.Outlined.Star

            Icon(
                imageVector = icon,
                contentDescription = null, // Null because the parent row has the description
                tint = starColor,
                modifier = Modifier.size(starSize.dp)
            )
        }
    }
}