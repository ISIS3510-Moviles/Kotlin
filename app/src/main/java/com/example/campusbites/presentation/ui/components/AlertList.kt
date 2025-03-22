package com.example.campusbites.presentation.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.campusbites.domain.model.AlertDomain

@Composable
fun AlertList(
    notifications: List<AlertDomain>,
    onAlertClick: (String) -> Unit,
    onUpvoteClick: (AlertDomain) -> Unit,
    onDownvoteClick: (AlertDomain) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(notifications) { notification ->
            AlertCard(
                notification = notification,
                onAlertClick = onAlertClick,
                onUpvoteClick = { onUpvoteClick(notification) },
                onDownvoteClick = { onDownvoteClick(notification) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}