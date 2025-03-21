package com.example.campusbites.presentation.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.campusbites.presentation.navigation.AppNavigation
import com.example.campusbites.presentation.ui.material.CampusBitesTheme
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Si MainActivity se lanza desde el proceso de sign in, recoge los extras
        val loginSuccess = intent.getBooleanExtra("login_success", false)
        if (loginSuccess) {
            val userId = intent.getStringExtra("user_id") ?: ""
            val userName = intent.getStringExtra("user_name") ?: ""
            val userEmail = intent.getStringExtra("user_email") ?: ""
            // Actualiza el AuthViewModel con la información del usuario autenticado
            authViewModel.checkOrCreateUser(
                userId = userId,
                userName = userName,
                userEmail = userEmail,
                onSuccess = { Log.d("MainActivity", "✅ Usuario actualizado en AuthViewModel") },
                onFailure = { error -> Log.e("MainActivity", "❌ Error actualizando usuario: ${error.message}") }
            )
        }

        setContent {
            CampusBitesTheme {
                // Pasa el AuthViewModel compartido a la navegación
                AppNavigation(authViewModel = authViewModel)
            }
        }
    }
}
