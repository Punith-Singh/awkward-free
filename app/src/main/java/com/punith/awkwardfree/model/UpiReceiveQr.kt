package com.punith.awkwardfree.model

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.common.BitMatrix
import java.net.URLEncoder

/** A receive-only UPI request without an amount; the payer chooses the amount in their UPI app. */
internal fun upiReceivePayload(upi: String, name: String): String {
    val id = upi.trim()
    require(id.length <= 200 && id.matches(Regex("[A-Za-z0-9._-]+@[A-Za-z0-9._-]+")))
    val payee = name.trim()
    require(payee.isNotBlank() && payee.length <= 200)
    return "upi://pay?pa=${URLEncoder.encode(id, "UTF-8")}&pn=${URLEncoder.encode(payee, "UTF-8")}&cu=INR"
}

internal fun upiReceiveQr(upi: String, name: String): BitMatrix =
    QRCodeWriter().encode(upiReceivePayload(upi, name), BarcodeFormat.QR_CODE, 512, 512, mapOf(EncodeHintType.MARGIN to 4))
