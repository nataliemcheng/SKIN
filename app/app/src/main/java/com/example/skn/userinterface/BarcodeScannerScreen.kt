package com.example.skn.userinterface

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun BarcodeScannerScreen(
    navController: NavHostController,
    onScanComplete: (String) -> Unit, // ← optional callback if needed
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner
    var isScanningDone by remember { mutableStateOf(false) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val image = InputImage.fromFilePath(context, it)
            BarcodeScanning.getClient().process(image)
                .addOnSuccessListener { barcodes ->
                    val result = barcodes.firstOrNull()?.rawValue ?: "No barcode found"
                    Log.d("Barcode", "🖼️ Scanned from gallery: $result")

                    // ✅ Navigate and show snackbar
                    navController.navigate("scanOrSearch?barcode=${Uri.encode(result)}") {
                        popUpTo("barcodeScanner") { inclusive = true }
                    }
                }
                .addOnFailureListener {
                    Log.e("Barcode", "Gallery scan failed", it)
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val barcodeScanner = BarcodeScanning.getClient()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (!isScanningDone) {
                        processImageProxy(barcodeScanner, imageProxy) { result ->
                            isScanningDone = true
                            cameraProvider.unbindAll()

                            navController.navigate("scanOrSearch?barcode=${Uri.encode(result)}") {
                                popUpTo("barcodeScanner") { inclusive = true }
                            }
                        }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("Camera", "Camera setup failed", e)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        })

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(250.dp, 150.dp)
                .border(3.dp, Color.Green, RoundedCornerShape(12.dp))
        )

        Button(
            onClick = { galleryLauncher.launch("image/*") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Choose from Gallery")
        }
    }
}



@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onScanSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    onScanSuccess(barcodes.first().rawValue ?: "")
                }
            }
            .addOnFailureListener {
                Log.e("Barcode", "❌ Camera scan failed", it)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
