package com.bimacore.usahakecil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bimacore.usahakecil.ui.theme.BrandColors

object FirstRunGuidePreferences {
    const val FILE_NAME = "first-run-guide"
    const val COMPLETED_KEY = "completed"
}

@Composable
fun FirstRunGuide(
    businessLabel: String,
    onComplete: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("first-run-guide"),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Kenalan dulu dengan $businessLabel",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Aplikasi ini punya dua mode. Pahami bagian ini sebelum mulai mencatat transaksi.",
                    style = MaterialTheme.typography.bodyLarge,
                )

                GuideCard(
                    icon = Icons.Outlined.PointOfSale,
                    title = "Mode Kasir / Pekerja",
                    body = "Mode awal aplikasi. Dipakai untuk membuka shift, memilih produk, menyelesaikan transaksi, melihat stok, dan melihat total transaksi aktif.",
                )
                GuideCard(
                    icon = Icons.Outlined.Lock,
                    title = "Mode Owner",
                    body = "Area pengelolaan yang dilindungi PIN Owner: operasional, keuangan, laporan, profil usaha, salinan data, pemulihan data, dan laporan Excel.",
                )
                GuideCard(
                    icon = Icons.Outlined.Shield,
                    title = "Cara masuk Owner",
                    body = "Tekan tombol mode di layar kasir, lalu pilih Buka Mode Owner. Buat PIN saat pertama kali atau masukkan PIN yang sudah dibuat.",
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Catatan penting", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Data tersimpan offline di HP ini. Mode yang sedang aktif akan diingat saat aplikasi dibuka lagi. Tekan Kunci Mode Owner jika ingin kembali ke Mode Kasir.")
                    }
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding-complete"),
                ) {
                    Text("Saya Mengerti, Mulai")
                }
            }
        }
    }
}

@Composable
private fun GuideCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
