package com.example.shoter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shoter.ui.screens.ARGameScreen
import com.example.shoter.ui.screens.MainMenuScreen
import com.example.shoter.ui.screens.PlayerRegistrationScreen
import com.example.shoter.ui.theme.ShoterTheme
import com.example.shoter.viewmodel.GameViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            ShoterTheme {
                val gameViewModel: GameViewModel = viewModel()
                var screenState by remember { mutableStateOf("menu") }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (screenState) {
                        "menu" -> MainMenuScreen(
                            onStartGame = { screenState = "game" },
                            onRegisterPlayer = { screenState = "register" }
                        )

                        "game" -> ARGameScreen(modifier = Modifier.padding(innerPadding))
                        "register" -> PlayerRegistrationScreen(
                            onDone = { screenState = "menu" },
                            gameViewModel = gameViewModel
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
