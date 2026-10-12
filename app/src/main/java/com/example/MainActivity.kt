package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AuthScreen
import com.example.ui.LRDownloaderViewModel
import com.example.ui.LRMassDownloaderScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check & request notification permission for Android 13+ (API 33+)
        // to ensure WorkManager foreground notifications display properly in the background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            MyApplicationTheme {
                AppContent()
            }
        }
    }

    @Composable
    fun AppContent() {
        var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

        DisposableEffect(Unit) {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                currentUser = auth.currentUser
            }
            Firebase.auth.addAuthStateListener(listener)
            onDispose {
                Firebase.auth.removeAuthStateListener(listener)
            }
        }

        val user = currentUser
        if (user == null) {
            AuthScreen(
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
        } else {
            val viewModel: LRDownloaderViewModel = viewModel(
                key = user.uid
            )
            LRMassDownloaderScreen(viewModel = viewModel)
        }
    }
}
