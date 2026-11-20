package com.punith.awkwardfree.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.punith.awkwardfree.ui.components.*
import com.punith.awkwardfree.ui.theme.*

@Composable
internal fun AccountScreen(auth: FirebaseAuth?, emailAddress: String?, back: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createAccount by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var signOutPrompt by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BackHeader("Your account", back)
        Heading("YOUR ACCOUNT", if (emailAddress != null) "You’re signed in." else "Welcome back.", "Sign in with your email and password.")
        if (auth == null) {
            EmptyCard("Account setup needed", "This build is not connected to a Firebase project yet. Your existing device-only data is still available.")
        } else if (emailAddress != null) {
            Column(Modifier.fillMaxWidth().background(Lavender, RoundedCornerShape(18.dp)).padding(18.dp)) {
                Text("SIGNED IN AS", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(emailAddress, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text("Your dues stay on this device for now. Signing in does not sync them to another phone.", color = Muted, fontSize = 13.sp)
            OutlinedButton(onClick = { signOutPrompt = true }, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
        } else {
            Text(if (createAccount) "Create your account" else "Sign in to your account", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = email, onValueChange = { email = it; status = "" }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Email address") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(16.dp))
            OutlinedTextField(value = password, onValueChange = { password = it; status = "" }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Password") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp))
            if (status.isNotEmpty()) Text(status, color = Violet, fontSize = 13.sp)
            Button(onClick = {
                val address = email.trim()
                if (address.isBlank() || password.isBlank()) { status = "Enter your email address and password." }
                else if (createAccount && password.length < 6) { status = "Choose a password with at least 6 characters." }
                else {
                    busy = true
                    val task = if (createAccount) auth.createUserWithEmailAndPassword(address, password) else auth.signInWithEmailAndPassword(address, password)
                    task.addOnCompleteListener { result -> busy = false; if (!result.isSuccessful) status = "Couldn’t ${if (createAccount) "create your account" else "sign in"}. Check your details and connection, then try again." }
                }
            }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp)) { Text(if (busy) "Please wait…" else if (createAccount) "Create account" else "Sign in", fontWeight = FontWeight.Bold) }
            TextButton(enabled = !busy, onClick = { createAccount = !createAccount; status = "" }) { Text(if (createAccount) "Already have an account? Sign in" else "New here? Create an account") }
            if (!createAccount) TextButton(enabled = !busy, onClick = {
                val address = email.trim()
                if (address.isEmpty()) status = "Enter your email address above first." else { busy = true; auth.sendPasswordResetEmail(address).addOnCompleteListener { busy = false; status = "If an account exists for this email, reset instructions may be sent." } }
            }) { Text("Forgot password?") }
            Text("Account details are managed by Firebase Authentication. Dues are stored on this device and are not synced yet.", color = Muted, fontSize = 12.sp)
        }
    }
    if (signOutPrompt && auth != null) AlertDialog(onDismissRequest = { signOutPrompt = false }, title = { Text("Sign out?") }, text = { Text("Your account’s dues stay on this device. Device-only entries will appear after you sign out.") }, confirmButton = { TextButton(onClick = { signOutPrompt = false; auth.signOut() }) { Text("Sign out") } }, dismissButton = { TextButton(onClick = { signOutPrompt = false }) { Text("Cancel") } })
}
