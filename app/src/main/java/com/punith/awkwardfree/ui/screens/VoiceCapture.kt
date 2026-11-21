package com.punith.awkwardfree.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

private enum class CapturePhase { Starting, Listening, Processing, Review, Error }

@Composable
internal fun VoiceCaptureButton(onText: (String) -> Unit, fallback: () -> Unit) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        denied = !it
        open = it
    }
    OutlinedButton(onClick = {
        denied = false
        when {
            !SpeechRecognizer.isRecognitionAvailable(context) -> fallback()
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> open = true
            else -> permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }, modifier = Modifier.fillMaxWidth()) { Text("Add with voice") }
    if (denied) Text("Microphone access is needed for dictation. You can also type the details.")
    if (open) VoiceCaptureDialog({ open = false }) { words ->
        open = false
        onText(words)
    }
}

@Composable
private fun VoiceCaptureDialog(close: () -> Unit, accept: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var attempt by remember { mutableIntStateOf(0) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var phase by remember { mutableStateOf(CapturePhase.Starting) }
    var words by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var level by remember { mutableFloatStateOf(0f) }
    DisposableEffect(attempt, lifecycle) {
        var active = true
        words = ""
        level = 0f
        phase = CapturePhase.Starting
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { if (active) phase = CapturePhase.Listening }
            override fun onBeginningOfSpeech() { if (active) phase = CapturePhase.Listening }
            override fun onRmsChanged(rmsdB: Float) { if (active) level = ((rmsdB + 2f) / 12f).coerceIn(0f,1f) }
            override fun onEndOfSpeech() { if (active) phase = CapturePhase.Processing }
            override fun onPartialResults(data: Bundle?) {
                if (active) data?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull { it.isNotBlank() }?.let { words = it }
            }
            override fun onResults(data: Bundle?) {
                if (!active) return
                onPartialResults(data)
                error = "No words heard. Try again closer to the microphone."
                phase = if (words.isBlank()) CapturePhase.Error else CapturePhase.Review
            }
            override fun onError(code: Int) {
                if (!active) return
                error = when (code) {
                    SpeechRecognizer.ERROR_AUDIO -> "The microphone is unavailable."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition lost its connection."
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No clear speech heard. Try again."
                    else -> "Dictation stopped. Try again or type the details."
                }
                phase = if (words.isBlank()) CapturePhase.Error else CapturePhase.Review
            }
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
        val engine = try { SpeechRecognizer.createSpeechRecognizer(context) } catch (_: RuntimeException) { null }
        recognizer = engine
        try {
            requireNotNull(engine).setRecognitionListener(listener)
            engine.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag("en-IN").toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            })
        } catch (_: RuntimeException) {
            phase = CapturePhase.Error
            error = "Couldn't start the microphone."
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { active = false; engine?.cancel(); close() }
        }
        lifecycle.addObserver(observer)
        onDispose {
            active = false
            lifecycle.removeObserver(observer)
            engine?.cancel()
            engine?.destroy()
        }
    }
    AlertDialog(onDismissRequest = close,
        title = { Text(when (phase) {
            CapturePhase.Starting -> "Opening microphone…"
            CapturePhase.Listening -> "Listening"
            CapturePhase.Processing -> "Finishing transcript…"
            CapturePhase.Review -> "Check what you said"
            CapturePhase.Error -> "Try dictation again"
        }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (phase == CapturePhase.Listening) LinearProgressIndicator(progress = { level }, modifier = Modifier.fillMaxWidth())
                Text(if (phase == CapturePhase.Error) error else words.ifBlank { "Try “I lent Rahul 500 rupees for lunch.”" })
            }
        },
        confirmButton = {
            when (phase) {
                CapturePhase.Listening -> TextButton(onClick = { phase = CapturePhase.Processing; recognizer?.stopListening() }) { Text("Finish") }
                CapturePhase.Review -> TextButton(onClick = { accept(words) }) { Text("Use transcript") }
                CapturePhase.Error -> TextButton(onClick = { attempt++ }) { Text("Try again") }
                else -> Unit
            }
        },
        dismissButton = {
            Row {
                if (phase == CapturePhase.Review) TextButton(onClick = { attempt++ }) { Text("Retry") }
                TextButton(onClick = close) { Text("Cancel") }
            }
        })
}
