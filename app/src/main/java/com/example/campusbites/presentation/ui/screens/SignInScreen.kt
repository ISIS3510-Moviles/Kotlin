package com.example.campusbites.presentation.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusbites.R
import com.example.campusbites.presentation.GoogleSignInActivity
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel

@Composable
fun SignInScreen(authViewModel: AuthViewModel) { // Añadido AuthViewModel como parámetro
    val context = LocalContext.current
    val isNetworkAvailable by authViewModel.isNetworkAvailable.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // Fondo blanco para la pantalla
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Barra superior naranja
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color(0xFFFFA000)) // Naranja similar al botón
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Logo
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Nombre de la App
        Text(
            text = "CampusBites",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Descripción
        Text(
            text = stringResource(id = R.string.sign_in_description),
            fontSize = 14.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Mensaje de "No hay conexión"
        AnimatedVisibility(
            visible = !isNetworkAvailable,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "No internet connection. Please check your connection and try again later.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Botón de Google Sign-In
        Button(
            onClick = {
                val intent = Intent(context, GoogleSignInActivity::class.java)
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(50.dp),
            enabled = isNetworkAvailable // Deshabilitar si no hay red
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle, // Icono de Google o similar
                contentDescription = "Google Icon",
                modifier = Modifier.size(20.dp),
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Log in with Google", fontSize = 16.sp, color = Color.Black)
        }
    }
}