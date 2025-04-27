package com.st10345224.socialmediaapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Load the image using painterResource
        Image(
            painter = painterResource(id = R.drawable.sma_logo), // Replace 'logo' with your image file name (without extension)
            contentDescription = "App Logo", // Important for accessibility
            modifier = Modifier.size(120.dp) // Adjust the size as needed
        )
        Spacer(modifier = Modifier.height(16.dp)) // Add some space between the logo and text (optional)
        Text("Social Media Brainrot", style = MaterialTheme.typography.headlineLarge)
        // You can add more elements here if needed
    }
}

@Preview
@Composable
fun SplashScreenPreview() {
    SplashScreen()
}