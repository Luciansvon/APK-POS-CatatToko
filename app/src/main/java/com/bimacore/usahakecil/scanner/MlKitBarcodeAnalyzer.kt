package com.bimacore.usahakecil.scanner

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import java.io.Closeable

class MlKitBarcodeAnalyzer(
    private val isEnabled: () -> Boolean = { true },
    private val onFrame: (List<String>) -> Unit,
    private val onError: (String) -> Unit,
) : ImageAnalysis.Analyzer, Closeable {
    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
            )
            .build(),
    )

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (!isEnabled()) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { detected ->
                onFrame(detected.mapNotNull(Barcode::getRawValue).distinct())
            }
            .addOnFailureListener { error ->
                onError(error.message ?: "Barcode gagal dibaca")
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    override fun close() {
        scanner.close()
    }
}
