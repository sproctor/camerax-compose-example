package com.example.cameraxcomposeexample

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraApp() {
    var showCamera by remember { mutableStateOf(false) }
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    if (showCamera) {
        CameraPreview {
            image = it.asImageBitmap()
            showCamera = false
        }
    } else {
        Column {
            if (image != null) {
                Image(bitmap = image!!, contentDescription = null)
            }
            Button(onClick = { showCamera = true }) {
                Text(text = "Take a picture")
            }
        }
    }
}

@Composable
fun CameraPreview(
    onCaptured: (Bitmap) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    var cameraProvider: ProcessCameraProvider? = null

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                imageCapture.takePicture(cameraExecutor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        @OptIn(ExperimentalGetImage::class)
                        override fun onCaptureSuccess(image: ImageProxy) {
                            onCaptured(image.toBitmap())
                            image.close()
                        }

                        override fun onError(exception: ImageCaptureException) {
                            exception.printStackTrace()
                            onError(exception)
                        }
                    }
                )
            },
        factory = { context ->
            PreviewView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Preview is incorrectly scaled in Compose on some devices without this
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                post {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                    cameraProviderFuture.addListener({
                        cameraProvider = cameraProviderFuture.get()

                        // Preview
                        val preview = Preview.Builder()
                            .build()
                        preview.setSurfaceProvider(surfaceProvider)

                        try {
                            cameraProvider?.unbindAll()

                            cameraProvider?.bindToLifecycle(
                                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                            )
                        } catch (exc: Exception) {
                            Log.e("CameraxCompose", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            }
        },
        onRelease = {
            cameraProvider?.unbindAll()
        }
    )
}
