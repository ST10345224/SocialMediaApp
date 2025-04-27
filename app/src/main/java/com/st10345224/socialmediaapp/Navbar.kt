package com.st10345224.socialmediaapp

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

// 1. Define the Screen Data Class
data class Screen(val route: String, val title: String, val icon: ImageVector)

// 2. Define the Screens
val profileScreen = Screen("profile", "Profile", Icons.Filled.Person)
val feedScreen = Screen("feed", "Feed", Icons.Filled.List)
val settingsScreen = Screen("settings", "Settings", Icons.Filled.Settings)
val newPostScreen = Screen("newPost", "New Post", Icons.Filled.Add)

// 3. Create the List of Screens
val screens = listOf(profileScreen, feedScreen, newPostScreen, settingsScreen)

// 4. Main App Composable with Navigation
@Composable
fun MainApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { paddingValues ->  // Use paddingValues here
        // 5. Navigation Host
        NavHost(
            navController = navController,
            startDestination = feedScreen.route,
            modifier = Modifier.padding(paddingValues)  // Apply padding to NavHost
        ) {
            composable(route = profileScreen.route) { ProfileScreen() }
            composable(route = feedScreen.route) { FeedScreen() }
            composable(route = settingsScreen.route) { SettingsScreen() }
            composable(route = newPostScreen.route) { // 6. Call CreatePostScreen and pass the callback
                CreatePostScreen(onPostCreated = {
                    // Define what happens after a post is created
                    navController.navigate(feedScreen.route) { // Go back to feed
                        popUpTo(feedScreen.route) { inclusive = true } // Remove feed screen from backstack
                    }
                    // Optionally show a message
                    Toast.makeText(
                        navController.context,
                        "Post created!",
                        Toast.LENGTH_SHORT
                    ).show()
                })
            }
        }
    }
}

// 6. Bottom Navigation Bar Composable
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar { // Use NavigationBar instead of BottomNavigation
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        screens.forEach { screen ->
            NavigationBarItem( // Use NavigationBarItem instead of BottomNavigationItem
                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        // Avoid multiple copies of the same destination when reselecting the same item
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                        //launchSingleTop = true
                    }
                }
            )
        }
    }
}

// 7. Dummy Screen Composables
@Composable
fun ProfileScreen() {
    Text("Profile Screen Content")
}

@Composable
fun FeedScreen() {
    SocialMediaFeed()
}

@Composable
fun SettingsScreen() {
    Text("Settings Screen Content")
}

@Composable
fun NewPostScreen() {
    Text("New Post Screen Content")
}
