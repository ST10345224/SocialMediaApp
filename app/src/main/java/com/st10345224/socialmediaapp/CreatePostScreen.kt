package com.st10345224.socialmediaapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CreatePostScreen(onPostCreated: () -> Unit) {
    var postText by remember { mutableStateOf("") }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var cameraPhotoPath by remember { mutableStateOf("") }
    var isCameraPermissionGranted by remember { mutableStateOf(true) } // Assume granted initially if not needed

    // Check for camera permission
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //check sdk version
        isCameraPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    // Activity launcher for selecting an image from the gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val imageUri = result.data?.data
            try {
                selectedImageBitmap = imageUri?.let {
                    val input = context.contentResolver.openInputStream(it)
                    BitmapFactory.decodeStream(input)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Activity launcher for capturing a photo with the camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraPhotoPath.isNotBlank()) {
            val file = File(cameraPhotoPath)
            if (file.exists()) {
                selectedImageBitmap = BitmapFactory.decodeFile(cameraPhotoPath)
            }

        } else if (!success) {
            Toast.makeText(context, "Failed to capture image.", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher for requesting camera permission
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isCameraPermissionGranted = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                "Camera permission is required to take photos.",
                Toast.LENGTH_LONG
            ).show()
            // Optionally, open app settings so the user can manually grant permission
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)

        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Create New Post",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = postText,
            onValueChange = { postText = it },
            label = { Text("What's on your mind?") },
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Display the selected image
        if (selectedImageBitmap != null) {
            Image(
                bitmap = selectedImageBitmap!!.asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium),  // Clip the image
                contentScale = ContentScale.Crop // Ensure image fills the bounds
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = {
                // Launch gallery intent
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galleryLauncher.launch(intent)
            }) {
                Text("Select from Gallery")
            }

            Button(onClick = {
                // Launch camera intent after checking permission
                if (isCameraPermissionGranted) {
                    val photoFile = createImageFile(context)
                    cameraPhotoPath = photoFile.absolutePath
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",  // Use your app's file provider authority
                        photoFile
                    )
                    cameraLauncher.launch(uri)
                } else {
                    // Request permission
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }

            }) {
                Text("Take Photo")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (postText.isNotBlank() && currentUser != null && selectedImageBitmap != null) {
                    // 1. Convert Bitmap to Base64 String
                    val imageString = encodeToBase64(compressBitmap(selectedImageBitmap!!))

                    // 2. Create Post Data
                    val postData = Post(
                        userId = currentUser.uid,
                        username = currentUser.email?.substringBefore('@') ?: "Anonymous", // Get username or default
                        timestamp = System.currentTimeMillis(),
                        content = postText,
                        imageString = imageString,
                        likes = 0,
                        comments = 0,
                        shares = 0
                    )

                    // 3. Save Post Data to Firestore
                    firestore.collection("posts")
                        .add(postData)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Post created successfully!", Toast.LENGTH_SHORT).show()
                            onPostCreated() // Callback to navigate away or refresh
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error creating post: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e("CreatePost", "Error creating post", e)
                        }
                } else {
                    Toast.makeText(context, "Please enter text and select an image", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = currentUser != null // Disable if not logged in
        ) {
            Text("Post")
        }
    }
}

// Function to create a temporary file for storing the camera photo
private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        "JPEG_${timeStamp}_",  /* prefix */
        ".jpg",         /* suffix */
        storageDir      /* directory */
    ).apply {
        // Save the file path for use with the Uri
        //  cameraPhotoPath = absolutePath
    }
}