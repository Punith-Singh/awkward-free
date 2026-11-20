package com.punith.awkwardfree

import android.os.Bundle
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            key(uid) { AwkwardApp(store, auth, user?.email) }
        } }
    }
}
