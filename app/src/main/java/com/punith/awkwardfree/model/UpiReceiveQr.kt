package com.punith.awkwardfree.model

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.common.BitMatrix
import java.net.URLEncoder

/** A receive-only UPI request without an amount; the payer chooses the amount in their UPI app. */
internal fun upiReceivePayload(upi: String): String {
    val id = upi.trim()
    require(id.length <= 200 && id.matches(Regex("[A-Za-z0-9._-]+@[A-Za-z0-9._-]+")))
    return "upi://pay?pa=${URLEncoder.encode(id, "UTF-8")}&cu=INR"
}

internal fun upiReceiveQr(upi: String): BitMatrix =
    QRCodeWriter().encode(upiReceivePayload(upi), BarcodeFormat.QR_CODE, 512, 512, mapOf(EncodeHintType.MARGIN to 4))
