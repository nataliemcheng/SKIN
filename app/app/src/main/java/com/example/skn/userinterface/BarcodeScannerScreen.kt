package com.example.skn.userinterface

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlinx.coroutines.launch
import com.example.skn.viewmodel.ProductViewModel

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    navController: NavHostController,
    viewModel: ProductViewModel,

) {
    val scope = rememberCoroutineScope()

    // ✅ Handle system back press
    BackHandler {
        scope.launch {
            viewModel.resetState()
            navController.navigate("main") {
                popUpTo("main") { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner
    val scannedProduct by viewModel.scannedProduct.collectAsState()
    var scannedBarcode by remember { mutableStateOf<String?>(null) }
    val error by viewModel.error.collectAsState()
    var isScanningDone by remember { mutableStateOf(false) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { it ->
            val image = InputImage.fromFilePath(context, it)
            BarcodeScanning.getClient().process(image)
                .addOnSuccessListener { barcodes ->
                    val result = barcodes.firstOrNull()?.rawValue
                    result?.let {
                        if (scannedBarcode != it) {
                            scannedBarcode = it
                            viewModel.fetchProductFromUpc(it)
                            showSheet = true
                            isScanningDone = true
                        }
                    }
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // Shortened camera preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp) // You can adjust this height as needed
                .align(Alignment.Center)
        ) {
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

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isScanningDone) {
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                BarcodeScanning.getClient().process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        val code = barcodes.firstOrNull()?.rawValue
                                        if (!code.isNullOrBlank() && scannedBarcode != code) {
                                            scannedBarcode = code
                                            viewModel.fetchProductFromUpc(code)
                                            showSheet = true
                                            isScanningDone = true
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val selector = CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageAnalysis
                    )
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            })
        }

        // TOP GRADIENT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
        )

        // BOTTOM GRADIENT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
        )

        // 🔙 Transparent back button in top-left
        IconButton(
            onClick = {
                scope.launch {
                    viewModel.resetState()
                    navController.navigate("main") {
                        popUpTo("main") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
            modifier = Modifier
                .padding(start = 16.dp, top = 50.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }

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
        // Bottom Sheet content
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            )
            {
                LaunchedEffect(scannedProduct) {
                    Log.d("BarcodeScanner", "Scanned Product: $scannedProduct")
                }
                if (scannedProduct != null) {
                    val product = scannedProduct!!
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("✅ Product Found", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(product.name ?: "No name")
                        Text(product.description ?: "No description")
                        Spacer(Modifier.height(8.dp))
                        product.image_link?.let {
                            Image(
                                painter = rememberAsyncImagePainter(it),
                                contentDescription = null,
                                modifier = Modifier.size(100.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { showSheet = false }) {
                            Text("Close")
                        }
                    }
                } else if (error != null) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("❌ Product not found", color = Color.Red)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            scannedBarcode?.let { barcode ->
                                navController.navigate("submitProduct/${Uri.encode(barcode)}")
                            }
                        }) {
                            Text("Submit Product Info")
                        }
                    }
                }

            }
        }
    }








