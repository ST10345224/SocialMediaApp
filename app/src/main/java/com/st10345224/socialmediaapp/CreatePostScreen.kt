package com.st10345224.socialmediaapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid
    val username = auth.currentUser?.email?.substringBefore('@') ?: "Anonymous" // Simple username

    // Function to create a temporary file for camera intent
    fun createImageFile(context: Context): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return try {
            val imageFile = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
            )
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // Camera image launcher
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraImageUri?.let { uri -> // 'uri' here is the result URI, not the one we provided
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(context, "Failed to load image from camera", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val uri = createImageFile(context)
            cameraImageUri = uri // Initialize cameraImageUri here, before launching
            uri?.let { takePicture.launch(it) }
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery image launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }



    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let { uri ->
            imageBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }

    fun savePostToFirestore() {
        userId?.let { uid ->
            val post = Post(
                userId = uid,
                username = username,
                timestamp = System.currentTimeMillis(),
                content = postText,
                imageString = imageBitmap?.let { compressBitmap(it) }?.let { encodeToBase64(it) },
                likes = 0,
                comments = 0,
                shares = 0
            )

            firestore.collection("posts")
                .add(post)
                .addOnSuccessListener { documentReference ->
                    Log.d("CreatePost", "Post added with ID: ${documentReference.id}")
                    // Now that the post is added, we have the document ID
                    // You might want to update your local Post object if you're still using it
                    onPostCreated()
                    Toast.makeText(context, "Post created!", Toast.LENGTH_SHORT).show()
                    postText = ""
                    selectedImageUri = null
                    imageBitmap = null
                }
                .addOnFailureListener { e ->
                    Log.e("CreatePost", "Error adding post: ${e.message}")
                    Toast.makeText(context, "Failed to create post.", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Create New Post", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
        OutlinedTextField(
            value = postText,
            onValueChange = { postText = it },
            label = { Text("What's on your mind?") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!.asImageBitmap(),
                contentDescription = "Selected Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = { galleryLauncher.launch("image/*") }) {
                Text("Select Image")
            }
            Button(onClick = {
                val uri = createImageFile(context)
                cameraImageUri = uri
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    uri?.let { takePicture.launch(it) } // Explicitly use 'it' which is the Uri
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }) {
                Text("Take Photo")
            }

        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (postText.isNotBlank() || imageBitmap != null) {
                    savePostToFirestore()
                } else {
                    Toast.makeText(context, "Post cannot be empty", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Post")
        }
    }
}