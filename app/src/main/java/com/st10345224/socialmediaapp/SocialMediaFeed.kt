package com.st10345224.socialmediaapp

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PostWithUser(val post: Post, val user: User?)

@Composable
fun SocialMediaFeed() {
    var postsWithUsers by remember { mutableStateOf<List<PostWithUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        firestore.collection("posts")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { postSnapshot ->
                val fetchedPosts = postSnapshot.documents.mapNotNull { postDocument ->
                    try {
                        val post = Post(
                            userId = postDocument.getString("userId") ?: "",
                            username = postDocument.getString("username") ?: "Anonymous",
                            timestamp = postDocument.getLong("timestamp") ?: 0,
                            content = postDocument.getString("text") ?: "",
                            imageString = postDocument.getString("imageString"),
                            likes = postDocument.getLong("likes")?.toInt() ?: 0,
                            comments = postDocument.getLong("comments")?.toInt() ?: 0,
                            shares = postDocument.getLong("shares")?.toInt() ?: 0
                        )
                        post // Return the Post object if creation is successful
                    } catch (e: Exception) {
                        Log.e("Firestore", "Error converting post document: ${e.message}")
                        Toast.makeText(
                            context,
                            "Error loading post: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        null // Skip this post if there's an error
                    }
                }

                // Fetch user data for each post
                val postsWithUserData = mutableListOf<PostWithUser>()
                fetchedPosts.forEach { post ->
                    firestore.collection("users").document(post.userId)
                        .get()
                        .addOnSuccessListener { userDocument ->
                            val user = if (userDocument.exists()) {
                                User(
                                    userId = userDocument.getString("userId") ?: "",
                                    firstName = userDocument.getString("firstName") ?: "",
                                    lastName = userDocument.getString("lastName") ?: "",
                                    email = userDocument.getString("email") ?: "",
                                    profilePicString = userDocument.getString("profilePicString")
                                )
                            } else {
                                null // User document not found
                            }
                            postsWithUserData.add(PostWithUser(post, user))
                            // Only update the state when all user data is fetched (crude way, improve later for large lists)
                            if (postsWithUserData.size == fetchedPosts.size) {
                                postsWithUsers = postsWithUserData.toList()
                                loading = false
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(
                                "Firestore",
                                "Error fetching user data for post ${post.userId}: ${e.message}"
                            )
                            postsWithUserData.add(
                                PostWithUser(
                                    post,
                                    null
                                )
                            ) // Show post even if user fetch fails
                            if (postsWithUserData.size == fetchedPosts.size) {
                                postsWithUsers = postsWithUserData.toList()
                                loading = false
                            }
                        }
                }
                if (fetchedPosts.isEmpty()) {
                    loading = false
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching posts: ${e.message}")
                error = "Failed to load posts: ${e.message}"
                loading = false
                Toast.makeText(context, "Failed to load posts: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    // UI based on loading, error, and post data
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
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Error: $error",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    } else {
        if (postsWithUsers.isEmpty()) {
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
                    "Be the first to post!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(postsWithUsers) { postWithUser ->
                    val post = postWithUser.post
                    val user = postWithUser.user
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val profilePainter: Painter = if (user?.profilePicString != null) {

                                    val imageBytes = decodeBase64(user.profilePicString)
                                    val bitmap = bytesToBitmap(imageBytes)
                                    if (bitmap != null) {
                                        BitmapPainter(bitmap.asImageBitmap())
                                    } else {
                                        painterResource(id = R.drawable.ic_launcher_foreground)
                                    }

                                } else {
                                    painterResource(id = R.drawable.ic_launcher_foreground)
                                }
                                Image(
                                    painter = profilePainter,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = post.username,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    val formattedDate = SimpleDateFormat(
                                        "dd/MM/yyyy HH:mm",
                                        Locale.getDefault()
                                    ).format(
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
                            Text(text = post.content, style = MaterialTheme.typography.bodyLarge)
                            if (post.imageString != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val imageBytes = decodeBase64(post.imageString)
                                val bitmap = bytesToBitmap(imageBytes)
                                val imagePainter: Painter = if (bitmap != null) {
                                    BitmapPainter(bitmap.asImageBitmap())
                                } else {
                                    painterResource(id = R.drawable.ic_launcher_foreground)
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
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = "Like"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = post.likes.toString())
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Comment"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = post.comments.toString())
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = post.shares.toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}