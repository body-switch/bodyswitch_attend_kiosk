package com.bodyswitch.checkin.ui.scanner

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun scanQrFromImage(context: Context, uri: Uri): String? {
    val inputImage = InputImage.fromFilePath(context, uri)

    val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    return suspendCancellableCoroutine { continuation ->
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val result = barcodes.firstOrNull()?.rawValue
                continuation.resume(result)
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }
}
