package com.st10345224.socialmediaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.st10345224.socialmediaapp.ui.theme.SocialMediaAppTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()

        setContent {
            SocialMediaAppTheme {
                var isSplashScreenVisible by remember { mutableStateOf(true) }
                var isRegistering by remember { mutableStateOf(false) } // Now starts with Login
                var isLoggedIn by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(key1 = true) {
                    delay(3000) // Wait for 3 seconds
                    isSplashScreenVisible = false // Hide the splash screen
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {


                    if (isSplashScreenVisible) {
                        SplashScreen() // Now calling the SplashScreen composable from its own file
                    } else if (isLoggedIn) {
                        MainApp() // Navbar and main screen

                    } else {
                        if (isRegistering) {
                            RegisterScreen(
                                onNavigateToLogin = { isRegistering = false },
                                onRegistrationSuccess = { isLoggedIn = true }
                            )
                        } else {
                            LoginScreen(
                                onNavigateToRegister = { isRegistering = true },
                                onLoginSuccess = { isLoggedIn = true }
                            )
                        }
                    }
                }
            }
        }
    }

}




