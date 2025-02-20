package presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun HomeScreen(
    onRestaurantClick: (String) -> Unit,
    modifier: Modifier = Modifier
){
    Column (
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
    ){
        Text(
            text = "Home Screen",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}