package com.punith.awkwardfree.model

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.*
import org.junit.Test

class UpiReceiveQrTest {
    @Test fun qrDecodesToSavedPayeeWithoutAnAmount() {
        val matrix = upiReceiveQr("punith@upi", "Punith Singh")
        val pixels = IntArray(matrix.width * matrix.height) { i ->
            if (matrix[i % matrix.width, i / matrix.width]) 0xff000000.toInt() else 0xffffffff.toInt()
        }
        val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(matrix.width, matrix.height, pixels)))).text
        assertEquals("upi://pay?pa=punith%40upi&pn=Punith+Singh&cu=INR", decoded)
        assertFalse(decoded.contains("&am="))
    }

    @Test fun rejectsInvalidOrMissingPayee() {
        assertThrows(IllegalArgumentException::class.java) { upiReceivePayload("not-a-upi", "Punith") }
        assertThrows(IllegalArgumentException::class.java) { upiReceivePayload("punith@upi", "") }
    }
}
