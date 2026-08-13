package com.bimacore.usahakecil.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bimacore.usahakecil.scanner.BarcodeFrameDecision
import com.bimacore.usahakecil.scanner.BarcodeScanGate
import com.bimacore.usahakecil.scanner.MlKitBarcodeAnalyzer
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerScreen(
    feedback: String?,
    processingVersion: Long = 0,
    paused: Boolean = false,
    onBarcode: (String) -> Unit,
    onMultipleBarcodes: () -> Unit,
    onBack: () -> Unit,
    singleScan: Boolean = false,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var requestedBefore by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        requestedBefore = true
        permissionGranted = granted
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
    }
    LaunchedEffect(Unit) {
        if (!permissionGranted && !requestedBefore) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (permissionGranted) {
        CameraScannerContent(
            feedback = feedback,
            processingVersion = processingVersion,
            paused = paused,
            onBarcode = onBarcode,
            onMultipleBarcodes = onMultipleBarcodes,
            onBack = onBack,
            singleScan = singleScan,
        )
    } else {
        val showRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
        } == true
        BarcodePermissionContent(
            permanentlyDenied = requestedBefore && !showRationale,
            onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onOpenSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            },
            onManualBarcode = onBarcode,
            onBack = onBack,
        )
    }
}

@Composable
private fun CameraScannerContent(
    feedback: String?,
    processingVersion: Long,
    paused: Boolean,
    onBarcode: (String) -> Unit,
    onMultipleBarcodes: () -> Unit,
    onBack: () -> Unit,
    singleScan: Boolean,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val gate = remember { BarcodeScanGate() }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val pausedState = rememberUpdatedState(paused)

    LaunchedEffect(processingVersion) {
        if (processingVersion > 0) gate.processed()
    }

    DisposableEffect(previewView, lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val analyzer = MlKitBarcodeAnalyzer(
            isEnabled = { !pausedState.value },
            onFrame = { codes ->
                when (val decision = gate.onFrame(codes)) {
                    is BarcodeFrameDecision.Process -> {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBarcode(decision.barcode)
                        if (singleScan) gate.processed()
                    }
                    BarcodeFrameDecision.MultipleBarcodes -> onMultipleBarcodes()
                    BarcodeFrameDecision.Ignore -> Unit
                }
            },
            onError = { cameraError = it },
        )
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        providerFuture.addListener(
            {
                if (disposed) return@addListener
                runCatching {
                    provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { it.setAnalyzer(executor, analyzer) }
                    provider?.unbindAll()
                    camera = provider?.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                }.onFailure {
                    cameraError = it.message ?: "Kamera tidak dapat dibuka"
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            disposed = true
            provider?.unbindAll()
            analyzer.close()
            executor.shutdown()
            camera = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("barcode-scanner"),
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.82f)
                .height(190.dp)
                .border(3.dp, Color.White, RoundedCornerShape(20.dp)),
        )
        Text(
            text = if (singleScan) "Arahkan satu barcode ke dalam kotak" else "Scan barang satu per satu",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 86.dp)
                .background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
        FilledTonalIconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).size(48.dp),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Tutup scanner")
        }
        val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
        if (hasFlash) {
            FilledTonalIconButton(
                onClick = {
                    torchEnabled = !torchEnabled
                    camera?.cameraControl?.enableTorch(torchEnabled)
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(48.dp),
            ) {
                Icon(
                    if (torchEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                    contentDescription = if (torchEnabled) "Matikan senter" else "Nyalakan senter",
                )
            }
        }
        if (paused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Scanner dijeda saat memilih produk",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Surface(
            color = Color.Black.copy(alpha = 0.74f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = cameraError ?: feedback ?: "Scanner aktif dan berjalan offline",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Barcode yang diam gk akan menambah barang berulang.",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun BarcodePermissionContent(
    permanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onManualBarcode: (String) -> Unit,
    onBack: () -> Unit,
) {
    var manualBarcode by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.QrCodeScanner,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text("Izin kamera dibutuhkan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Kamera cuma membaca barcode secara lokal. Kasir tetap bisa cari atau ketik barcode tanpa izin kamera.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        if (permanentlyDenied) {
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Buka Pengaturan")
            }
        } else {
            Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Izinkan Kamera")
            }
        }
        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            value = manualBarcode,
            onValueChange = { manualBarcode = it },
            label = { Text("Barcode manual") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { onManualBarcode(manualBarcode) },
            enabled = manualBarcode.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Cari Barcode") }
        OutlinedButton(
            onClick = onBack,
            border = BorderStroke(0.dp, Color.Transparent),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Kembali ke katalog") }
    }
}
