package com.example.shoter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shoter.ui.CrosshairOverlay
import com.example.shoter.ui.theme.ShoterTheme
import com.example.shoter.viewmodel.GameViewModel
import com.google.accompanist.permissions.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

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

    @Composable
    fun MainMenuScreen(
        onStartGame: () -> Unit,
        onRegisterPlayer: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
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
    }

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun ARGameScreen(modifier: Modifier = Modifier) {
        val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
        val gameViewModel: GameViewModel = viewModel()
        val uiState by gameViewModel.uiState.collectAsState()

        if (cameraPermissionState.status.isGranted) {
            Box(modifier = modifier.fillMaxSize()) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    gameViewModel = gameViewModel
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

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun PlayerRegistrationScreen(
        onDone: () -> Unit,
        gameViewModel: GameViewModel
    ) {
        val context = LocalContext.current
        val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
        val lifecycleOwner = LocalContext.current as LifecycleOwner
        val imageCapture = remember { ImageCapture.Builder().build() }
        val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

        var previewView by remember { mutableStateOf<PreviewView?>(null) }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Регистрация игрока")
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

                            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

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

                Button(onClick = {
                    val outputFile = File(context.cacheDir, "player_photo.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                    imageCapture.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
                                val base64Image = encodeBitmapToBase64(bitmap)

                                val playerId = UUID.randomUUID().toString()
                                gameViewModel.createPlayerProfile(
                                    playerId = playerId,
                                    images = listOf(base64Image),
                                    angles = listOf("front")
                                )

                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context, "Профиль отправлен", Toast.LENGTH_SHORT).show()
                                    onDone()
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("PlayerReg", "Ошибка сохранения фото", exception)
                            }
                        }
                    )
                }) {
                    Text("Сделать фото и зарегистрировать")
                }
            }
        }
    }


    fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    @Composable
    fun CameraPreview(
        modifier: Modifier = Modifier,
        gameViewModel: GameViewModel
    ) {
        val context = LocalContext.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val options = AccuratePoseDetectorOptions.Builder()
                    .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
                    .build()
                val poseDetector = PoseDetection.getClient(options)

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val image = InputImage.fromMediaImage(imageProxy.image!!, rotationDegrees)

                    poseDetector.process(image)
                        .addOnSuccessListener { pose ->
                            if (pose.allPoseLandmarks.isNotEmpty()) {
                                // Player detected! Show crosshair and notify server
                                Log.d("PlayerDetection", "Player pose detected!")
                                gameViewModel.onPlayerDetected()

                                // Check if pose is centered in crosshair (simplified logic)
                                val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
                                if (nose != null) {
                                    val imageWidth = imageProxy.width
                                    val imageHeight = imageProxy.height
                                    val centerX = imageWidth / 2
                                    val centerY = imageHeight / 2

                                    // Check if nose is within crosshair area (±50 pixels)
                                    val isInCrosshair = abs(nose.position.x - centerX) < 50 &&
                                            abs(nose.position.y - centerY) < 50

                                    if (isInCrosshair) {
                                        gameViewModel.onCrosshairCentered()
                                    }
                                }
                            } else {
                                gameViewModel.onPlayerLost()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("CameraPreview", "Pose detection failed", e)
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        context as LifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", exc)
                }

                previewView
            },
            modifier = modifier.fillMaxSize()
        )
    }
}
