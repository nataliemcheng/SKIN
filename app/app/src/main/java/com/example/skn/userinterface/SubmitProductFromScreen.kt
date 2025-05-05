package com.example.skn.userinterface


import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.skn.viewmodel.ProductViewModel
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitProductFormScreen(
    navController: NavHostController,
    barcode: String,
    viewModel: ProductViewModel,
    onSubmit: (String, String, String, String, Uri?, String) -> Unit
) {
    val context = LocalContext.current
    var productName by remember { mutableStateOf(TextFieldValue()) }
    var brandName by remember { mutableStateOf(TextFieldValue()) }
    var description by remember { mutableStateOf(TextFieldValue()) }
    var ingredients by remember { mutableStateOf(TextFieldValue()) }


    var frontImageUri by remember { mutableStateOf<Uri?>(null) }

    var showFrontOptions by remember { mutableStateOf(false) }

    // ✅ Prefill product data from UPC
    LaunchedEffect(barcode) {
        viewModel.prefillProductInfoFromUPC(barcode) { product ->
            product?.let {
                productName = TextFieldValue(it.name ?: "")
                brandName = TextFieldValue(it.brand ?: "")
                description = TextFieldValue(it.description ?: "")
                frontImageUri = it.image_link?.let { link -> Uri.parse(link) }

            }
        }
    }

    val galleryLauncherFront = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        frontImageUri = it
    }


    val cameraLauncherFront = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        frontImageUri = bitmap?.let {
            saveBitmapToCache(context, it, "front_image_${System.currentTimeMillis()}")
        }
    }

    // ✅ UI continues as you already had
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add Product") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        onSubmit(
                            productName.text,
                            brandName.text,
                            description.text,
                            ingredients.text,
                            frontImageUri,
                            barcode
                        )
                        navController.popBackStack()
                    }) {
                        Text("Submit")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())

        ) {
            OutlinedTextField(
                value = barcode,
                onValueChange = {},
                label = { Text("Barcode") },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("Product Name") },
                placeholder = { Text("e.g. Grown Alchemist Hand Wash") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = brandName,
                onValueChange = { brandName = it },
                label = { Text("Brand Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Text("Upload Product Photo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    .clickable { showFrontOptions = true },
                contentAlignment = Alignment.Center
            ) {
                if (frontImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(frontImageUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        "Tap to upload a photo of the product",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }


            Spacer(Modifier.height(24.dp))

            Text("Description", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Text("Ingredients", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = ingredients,
                onValueChange = { ingredients = it },
                label = { Text("Ingredients") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    onSubmit(
                        productName.text,
                        brandName.text,
                        description.text,
                        ingredients.text,
                        frontImageUri,
                        barcode
                    )
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SUBMIT")
            }
        }

        if (showFrontOptions) {
            ModalBottomSheet(onDismissRequest = { showFrontOptions = false }) {
                ListItem(
                    headlineContent = { Text("Take a photo") },
                    modifier = Modifier.clickable {
                        cameraLauncherFront.launch(null)
                        showFrontOptions = false
                    }
                )
                ListItem(
                    headlineContent = { Text("Choose from gallery") },
                    modifier = Modifier.clickable {
                        galleryLauncherFront.launch("image/*")
                        showFrontOptions = false
                    }
                )
            }
        }


    }
}

fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): Uri {
    val file = File(context.cacheDir, "$fileName.jpg")
    file.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}



