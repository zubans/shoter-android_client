package com.example.shoter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.shoter.BuildConfig

@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    onRegisterPlayer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(onClick = onStartGame, modifier = Modifier.fillMaxWidth()) {
                Text("В игру")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRegisterPlayer, modifier = Modifier.fillMaxWidth()) {
                Text("Зарегистрировать игрока")
            }
        }

        Text(
            text = "Сборка ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
