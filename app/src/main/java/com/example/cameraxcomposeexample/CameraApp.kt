package com.example.cameraxcomposeexample

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.launch
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
    val context = LocalContext.current
    var currentSurfaceRequest: SurfaceRequest? by remember { mutableStateOf(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    val preview = remember {
        Preview.Builder()
            .build()
            .apply {
                setSurfaceProvider {
                    currentSurfaceRequest = it
                }
            }
    }

    val scope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        var cameraProvider: ProcessCameraProvider? = null

        scope.launch {
            cameraProvider = ProcessCameraProvider.awaitInstance(context)
            cameraProvider!!.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        }

        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    currentSurfaceRequest?.let { surfaceRequest ->
        CameraXViewfinder(
            surfaceRequest = surfaceRequest,
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
        )
    }
}
