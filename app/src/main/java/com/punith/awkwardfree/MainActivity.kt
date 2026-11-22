package com.punith.awkwardfree

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.punith.awkwardfree.data.*
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.punith.awkwardfree.navigation.*
import com.punith.awkwardfree.ui.theme.*
import com.punith.awkwardfree.ui.screens.OnboardingScreen

class MainActivity : ComponentActivity() {
    private var quickAddPending by mutableStateOf(false)

    private fun handleShortcut(intent: Intent?) {
        if (intent?.action == "com.punith.awkwardfree.action.QUICK_ADD") {
            quickAddPending = true
            intent.action = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcut(intent)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShortcut(intent)
        window.statusBarColor = android.graphics.Color.rgb(245, 244, 251)
        window.navigationBarColor = android.graphics.Color.WHITE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Violet, onPrimary = Color.White, surface = Color.White, background = Background, onSurface = Ink), typography = appTypography()) {
            val auth = remember { if (FirebaseApp.getApps(this).isEmpty()) null else FirebaseAuth.getInstance() }
            var user by remember { mutableStateOf(auth?.currentUser) }
            DisposableEffect(auth) {
                val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
                auth?.addAuthStateListener(listener)
                onDispose { auth?.removeAuthStateListener(listener) }
            }
            val uid = user?.uid
            val store = remember(uid) { Store(this, uid) }
            val onboardingPrefs = remember { getSharedPreferences("awkward_free_onboarding", MODE_PRIVATE) }
            var onboardingComplete by remember { mutableStateOf(onboardingPrefs.getBoolean("complete", false)) }
            if (onboardingComplete) {
                key(uid) { AwkwardApp(store, auth, user?.email, quickAddPending) { quickAddPending = false } }
            } else {
                OnboardingScreen {
                    onboardingPrefs.edit().putBoolean("complete", true).apply()
                    onboardingComplete = true
                }
            }
        } }
    }
}
