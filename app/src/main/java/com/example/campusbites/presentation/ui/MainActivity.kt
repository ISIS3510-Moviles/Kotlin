package com.example.campusbites.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.campusbites.presentation.ui.material.CampusBitesTheme
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.campusbites.presentation.navigation.NavGraph
import com.example.campusbites.presentation.ui.screens.SignInScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusBitesTheme {
                val user by authViewModel.user.collectAsStateWithLifecycle()

                if (user == null) {
                    // Pasar el authViewModel a SignInScreen
                    SignInScreen(authViewModel = authViewModel)
                } else {
                    NavGraph(authViewModel = authViewModel)
                }
            }
        }
    }
}