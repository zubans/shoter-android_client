package com.example.shoter.ui.components

import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.example.shoter.viewmodel.GameViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import java.util.concurrent.Executors
import kotlin.math.abs

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