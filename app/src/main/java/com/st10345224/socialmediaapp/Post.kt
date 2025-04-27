package com.st10345224.socialmediaapp

data class Post(
    val userId: String, // Add userId
    val username: String,
    val timestamp: Long, // Changed to Long for easier sorting
    val content: String,
    val imageString: String? = null, // Store Base64 string, null if no image
    val likes: Int,
    val comments: Int,
    val shares: Int
)
