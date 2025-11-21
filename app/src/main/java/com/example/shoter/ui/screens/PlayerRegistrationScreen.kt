package com.example.shoter.ui.screens

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.shoter.network.PhotoPayload
import com.example.shoter.util.processCapturedPhoto
import com.example.shoter.viewmodel.GameViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PlayerRegistrationScreen(
    onDone: () -> Unit,
    gameViewModel: GameViewModel
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val lifecycleOwner = LocalContext.current as LifecycleOwner
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetResolution(Size(1280, 720))
            .build()
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val angles = listOf("front", "left", "right", "back")
    var currentAngleIndex by remember { mutableStateOf(0) }
    var capturedImages by remember { mutableStateOf(listOf<String>()) }
    var capturedAngles by remember { mutableStateOf(listOf<String>()) }
    val capturedPayloads = remember { mutableStateListOf<PhotoPayload>() }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Регистрация игрока")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Сделайте фото: ${angles[currentAngleIndex]}")
        Spacer(modifier = Modifier.height(16.dp))

        if (!cameraPermissionState.status.isGranted) {
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Разрешить доступ к камере")
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    val view = PreviewView(ctx)
                    previewView = view

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(view.surfaceProvider)
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (exc: Exception) {
                            Log.e("PlayerReg", "Camera binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    view
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                enabled = !isProcessing,
                onClick = {
                    isProcessing = true
                    val outputFile =
                        File(context.cacheDir, "player_photo_${angles[currentAngleIndex]}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                try {
                                    val processingResult = processCapturedPhoto(
                                        file = outputFile,
                                        angle = angles[currentAngleIndex]
                                    )
                                    outputFile.delete()

                                    mainHandler.post {
                                        capturedImages = capturedImages + processingResult.legacyImage
                                        capturedAngles = capturedAngles + angles[currentAngleIndex]
                                        capturedPayloads.add(processingResult.payload)

                                        if (currentAngleIndex < angles.size - 1) {
                                            currentAngleIndex++
                                        } else {
                                            val playerId = UUID.randomUUID().toString()
                                            gameViewModel.createPlayerProfile(
                                                playerId = playerId,
                                                images = capturedImages,
                                                angles = capturedAngles,
                                                photos = capturedPayloads.toList()
                                            )

                                            Toast.makeText(
                                                context,
                                                "Профиль отправлен",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onDone()
                                        }
                                    }
                                } catch (e: Exception) {
                                    outputFile.delete()
                                    Log.e("PlayerReg", "Ошибка обработки фото", e)
                                    mainHandler.post {
                                        Toast.makeText(
                                            context,
                                            "Ошибка обработки фото: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } finally {
                                    mainHandler.post { isProcessing = false }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("PlayerReg", "Ошибка сохранения фото", exception)
                                outputFile.delete()
                                mainHandler.post {
                                    isProcessing = false
                                    Toast.makeText(
                                        context,
                                        "Не удалось сделать фото: ${exception.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }
            ,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val buttonText = if (currentAngleIndex < angles.size - 1) {
                    if (isProcessing) "Обработка..." else "Сделать фото (${angles[currentAngleIndex]})"
                } else {
                    if (isProcessing) "Обработка..." else "Сделать последнее фото и зарегистрировать"
                }
                Text(buttonText)
            }
        }
    }
}
