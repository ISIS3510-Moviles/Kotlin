package com.example.campusbites.presentation.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import androidx.navigation.NavController
import com.example.campusbites.presentation.GoogleSignInActivity
import com.example.campusbites.presentation.ui.viewmodels.AuthViewModel

@Composable
fun SignInScreen(navController: NavController, authViewModel: AuthViewModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            val intent = Intent(context, GoogleSignInActivity::class.java)
            context.startActivity(intent)
        }) {
            Text("Sign in with Google")
        }
    }
}
