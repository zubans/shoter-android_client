package com.example.shoter.ui.screens

import android.util.Base64
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shoter.ui.CrosshairOverlay
import com.example.shoter.ui.components.CameraPreview
import com.example.shoter.viewmodel.GameViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ARGameScreen(modifier: Modifier = Modifier) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val gameViewModel: GameViewModel = viewModel()
    val uiState by gameViewModel.uiState.collectAsState()
    val imageCaptureState = remember { mutableStateOf<ImageCapture?>(null) }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }
    val context = LocalContext.current

    if (cameraPermissionState.status.isGranted) {
        Box(modifier = modifier.fillMaxSize()) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                gameViewModel = gameViewModel,
                onImageCaptureReady = { imageCaptureState.value = it }
            )
            CrosshairOverlay(
                modifier = Modifier.fillMaxSize(),
                isPlayerDetected = uiState.isPlayerDetected
            )

            // Show elimination message
            if (uiState.eliminationMessage.isNotEmpty()) {
                Text(
                    text = uiState.eliminationMessage,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            Button(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(24.dp),
                enabled = !uiState.isShotInProgress,
                onClick = {
                    val imageCapture = imageCaptureState.value
                    if (imageCapture == null) {
                        gameViewModel.onShotFailed("Камера не готова")
                        return@Button
                    }
                    gameViewModel.startShot()
                    val photoFile = File.createTempFile("shot_", ".jpg", context.cacheDir)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        captureExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                try {
                                    val bytes = photoFile.readBytes()
                                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                    gameViewModel.submitShot(base64)
                                } catch (e: Exception) {
                                    gameViewModel.onShotFailed("Ошибка обработки кадра: ${e.message}")
                                } finally {
                                    photoFile.delete()
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                photoFile.delete()
                                gameViewModel.onShotFailed("Ошибка съемки: ${exception.message}")
                            }
                        }
                    )
                }
            ) {
                Text(if (uiState.isShotInProgress) "Отправка..." else "Выстрел")
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            Text("Camera permission required for AR game")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Grant Camera Permission")
            }
        }
    }
}