package com.st10345224.socialmediaapp

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

fun handleLike(postId: String, userId: String) {
    val firestore = FirebaseFirestore.getInstance()
    val postRef = firestore.collection("posts").document(postId)
    val likeRef = postRef.collection("likes").document(userId)

    likeRef.get()
        .addOnSuccessListener { document ->
            if (!document.exists()) {
                // User hasn't liked the post yet - Like it
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val currentLikes = snapshot.getLong("likes") ?: 0
                    transaction.update(postRef, "likes", currentLikes + 1)
                    transaction.set(likeRef, mapOf("userId" to userId))
                }.addOnSuccessListener {
                    Log.d("Like", "Post $postId liked by user $userId")
                    // Optionally, update UI here to show as liked
                }.addOnFailureListener { e ->
                    Log.e("Like", "Error liking post $postId by user $userId: ${e.message}")
                    // Optionally, show error to user
                }
            } else {
                // User has already liked the post - Unlike it
                firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(postRef)
                    val currentLikes = snapshot.getLong("likes") ?: 0
                    if (currentLikes > 0) {
                        transaction.update(postRef, "likes", currentLikes - 1)
                    }
                    transaction.delete(likeRef)
                }.addOnSuccessListener {
                    Log.d("Like", "Post $postId unliked by user $userId")
                    // Optionally, update UI here to show as unliked
                }.addOnFailureListener { e ->
                    Log.e("Like", "Error unliking post $postId by user $userId: ${e.message}")
                    // Optionally, show error to user
                }
            }
        }
        .addOnFailureListener { e ->
            Log.e("Like", "Error checking like status for post $postId by user $userId: ${e.message}")
            // Optionally, show error to user
        }
}