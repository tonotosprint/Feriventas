package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.myapplication.ui.AuthScreen
import com.example.myapplication.ui.MainScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.util.FirestoreSyncHelper
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val auth = remember { FirebaseAuth.getInstance() }
            var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
            var bypassAuth by remember { mutableStateOf(false) }

            MyApplicationTheme {
                if (isLoggedIn || bypassAuth) {
                    MainScreen(
                        onLogout = {
                            FirestoreSyncHelper.clearLocalAppCache(applicationContext)
                            auth.signOut()
                            isLoggedIn = false
                            bypassAuth = false
                        }
                    )
                } else {
                    AuthScreen(
                        onLoginSuccess = { isLoggedIn = true },
                        onContinueOffline = { bypassAuth = true }
                    )
                }
            }
        }
    }
}
