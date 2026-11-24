package com.punith.awkwardfree.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.punith.awkwardfree.model.upiReceiveQr

@Composable internal fun UpiQrDialog(upi: String, name: String, onDismiss: () -> Unit) {
    val qr = remember(upi, name) {
        runCatching {
            val matrix = upiReceiveQr(upi, name)
            val pixels = IntArray(matrix.width * matrix.height) { index ->
                if (matrix[index % matrix.width, index / matrix.width]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
            Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
            }.asImageBitmap()
        }.getOrNull()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your UPI QR") },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (qr != null) Image(bitmap = qr, contentDescription = "UPI QR code for $upi", modifier = Modifier.size(240.dp).background(Color.White), filterQuality = FilterQuality.None)
                else Text("Couldn’t create the QR. Check your saved name and UPI ID.")
                Text(name, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                Text(upi, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                Text("The payer chooses the amount in their UPI app. Confirm the payment before marking a tab settled.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}
