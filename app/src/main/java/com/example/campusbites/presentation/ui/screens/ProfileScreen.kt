package com.example.campusbites.presentation.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.campusbites.domain.model.UserProfile
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.tooling.preview.Preview
import com.example.campusbites.data.TestData
import com.example.campusbites.data.TestData.sampleUser
import com.example.campusbites.presentation.ui.components.AccountSettingsCard
import com.example.campusbites.presentation.ui.components.ActivityCard
import com.example.campusbites.presentation.ui.components.AlertTopBar
import com.example.campusbites.presentation.ui.components.PreferencesCard
import com.example.campusbites.presentation.ui.components.UserInfoCard


@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    user: UserProfile
) {

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {
        AlertTopBar(onBackClick = { onBackClick()})
        UserInfoCard(user = user)
        PreferencesCard(user = user)
        ActivityCard(user = user)
        AccountSettingsCard(user = user)
    }
}

@Preview(showBackground = true)
@Composable
fun UserProfilePreview() {
    ProfileScreen(user = TestData.sampleUser, onBackClick = { })
}