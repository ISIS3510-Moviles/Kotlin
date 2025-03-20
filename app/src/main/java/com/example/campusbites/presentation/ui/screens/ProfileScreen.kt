package com.example.campusbites.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.auth.FirebaseAuth


@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onSignInClick: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user == null) {
            Button(onClick = onSignInClick) {
                Text(text = "Iniciar sesión con Google")
            }
        } else {
            Text(text = "Bienvenido, ${user.displayName}")
            Text(text = "Email: ${user.email}")

            Button(onClick = {
                FirebaseAuth.getInstance().signOut()
            }) {
                Text("Cerrar sesión")
            }
        }
    }
}
