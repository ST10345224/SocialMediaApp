package com.st10345224.socialmediaapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import java.util.Date

@Composable
fun SocialMediaFeed() {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var loading by remember { mutableStateOf<Boolean>(true) } // Track loading state
    var error by remember { mutableStateOf<String?>(null) } // Track errors
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Fetch posts from Firestore
    LaunchedEffect(Unit) { // Fetch only once
        firestore.collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING) // Order by timestamp
            .get()
            .addOnSuccessListener { snapshot ->
                val fetchedPosts = snapshot.documents.map { document ->
                    try {
                        // Use the Post data class constructor
                        Post(
                            userId = document.getString("userId") ?: "",
                            username = document.getString("username") ?: "Anonymous",
                            timestamp = document.getLong("timestamp") ?: 0,
                            content = document.getString("text") ?: "",
                            imageString = document.getString("imageString"),
                            likes = document.getLong("likes")?.toInt() ?: 0,
                            comments = document.getLong("comments")?.toInt() ?: 0,
                            shares = document.getLong("shares")?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error converting document to Post: ${e.message}")
                        // Handle the error, e.g., show a toast or a specific error message in the UI
                        Toast.makeText(context, "Error loading post: ${e.message}", Toast.LENGTH_SHORT).show()
                        Post( // Return a default Post object in case of error.
                            userId = "",
                            username = "Error",
                            timestamp = 0,
                            content = "Error loading post",
                            likes = 0,
                            comments = 0,
                            shares = 0
                        )
                    }
                }
                posts = fetchedPosts
                loading = false // Set loading to false after successful fetch
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching posts: ${e.message}")
                error = "Failed to load posts: ${e.message}" // Set error message
                loading = false
                Toast.makeText(context, "Failed to load posts: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Show loading indicator
    if (loading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Text("Loading posts...")
        }
    } else if (error != null) {
        // Show error message
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Error: $error", // Display the error message
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }

    } else {
        // Display the list of posts
        if (posts.isEmpty()) {
            // Display a message when there are no posts
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No posts yet!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Be the first to post!", // Changed message
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(posts) { post ->  // Iterate through the list of Post objects
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = post.username, style = MaterialTheme.typography.titleMedium)  // Access post properties
                                    val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(
                                        Date(post.timestamp)
                                    )
                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = post.content, style = MaterialTheme.typography.bodyLarge)  // Access post properties
                            // Use rememberAsyncImagePainter for loading images from URLs or Base64 strings
                            if (post.imageString != null) {
                                Spacer(modifier = Modifier.height(8.dp))

                                val imageBytes = decodeBase64(post.imageString)
                                val bitmap = bytesToBitmap(imageBytes)

                                val imagePainter: Painter = if (bitmap != null) {
                                    BitmapPainter(bitmap.asImageBitmap())
                                } else {
                                    painterResource(id = R.drawable.ic_launcher_foreground) // Or a placeholder
                                }

                                Image(
                                    painter = imagePainter,
                                    contentDescription = "Post Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(MaterialTheme.shapes.medium),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Like")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = post.likes.toString())  // Access post properties
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Comment")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = post.comments.toString())  // Access post properties
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Filled.Share, contentDescription = "Share")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = post.shares.toString())  // Access post properties
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewSocialMediaFeedEmpty() {
    SocialMediaFeed()
}