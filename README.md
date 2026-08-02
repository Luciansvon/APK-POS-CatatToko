# Usaha Kecil Suite

Satu source Android offline-first untuk tiga APK operasional:

- Retail dan UMKM;
- Grosir dan Agen;
- Kuliner dan Pedagang Kaki Lima.

Versi fitur saat ini: `0.4.8` (`versionCode 17`). Semua fungsi utama berjalan lokal di HP owner tanpa akun, server, atau internet.

Aplikasi selalu mulai dalam Mode Kasir/Pekerja. Pada pemasangan pertama, panduan wajib menjelaskan Mode Kasir/Pekerja dan Mode Owner tanpa tombol lewati. Pekerja hanya dapat memakai kasir, melihat stok produk dan total transaksi aktif, serta membuka shift. Owner dapat memakai kasir tanpa membuka shift pekerja. Operasional, keuangan, laporan, profil, backup, restore, dan export Excel tetap baru muncul setelah PIN Owner benar.

## Fungsi bersama

- kasir, katalog, kategori, produk, varian, keranjang, dan kalkulator;
- pembayaran Tunai, QRIS, Transfer, serta piutang pada flavor yang mengizinkan;
- stok berbasis riwayat pergerakan dan penyesuaian wajib alasan;
- supplier, pembelian, kas, pengeluaran, utang, piutang, dan cicilan;
- daftar serta detail transaksi;
- laporan omzet, metode pembayaran, kas, pengeluaran, utang, dan piutang;
- satu PIN Owner offline yang disimpan sebagai hash;
- pekerja harian, freelancer/panggilan, kehadiran, pekerjaan, dan pembayaran;
- profil usaha;
- backup lokal berversi, pemeriksaan integritas, berbagi file, dan restore aman;
- export `.xlsx` offline terstruktur dengan `Info Export`, `Ringkasan`, tabel operasional, dan lebar kolom otomatis untuk dibagikan Owner;
- struk dan berbagi PNG.

## Fungsi khusus APK

| APK | Fungsi khusus |
|---|---|
| Retail dan UMKM | pelanggan dan penjualan piutang |
| Grosir dan Agen | multi-satuan pcs/pak/dus, konversi stok, harga bertingkat, pelanggan dan piutang |
| Kuliner dan PKL | topping, catatan item, antrean/status pesanan, resep sederhana, pengurangan bahan |

Cloud, pajak otomatis, HPP/laba, BPJS, payroll formal, printer, marketplace, payment gateway, serta sinkronisasi multi-device belum termasuk rilis ini.

## Build dan test

Gunakan JDK 17 atau lebih baru. Pull request diverifikasi otomatis melalui
`.github/workflows/android-ci.yml` untuk seluruh flavor.

```powershell
.\gradlew.bat testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRetailDebugAndroidTest assembleWholesaleDebugAndroidTest assembleCulinaryDebugAndroidTest
.\gradlew.bat lintRetailDebug lintWholesaleDebug lintCulinaryDebug
```

Perintah unit test di atas tidak menjalankan test pada `app/src/androidTest`.
Test instrumentasi harus dijalankan pada emulator/perangkat untuk masing-masing flavor.

Setelah seluruh verifikasi lulus:

```powershell
.\scripts\package-apks.ps1
```

APK debug:

```text
dist/debug/CatatToko-Retail.apk
dist/debug/CatatToko-Grosir.apk
dist/debug/CatatToko-Kuliner.apk
```

## Dokumentasi

- `docs/superpowers/specs/2026-07-30-offline-operations-suite-design.md`: spesifikasi rilis fitur.
- `docs/ARCHITECTURE.md`: arsitektur dan batas sistem.
- `docs/WORKLOG.md`: pekerjaan serta bukti verifikasi.
- `docs/ERROR_SOLUTIONS.md`: gejala, root cause, solusi, dan bukti bugfix.
- `docs/RELEASE_NOTES.md`: riwayat versi APK.
- `docs/UI_UX_REQUIREMENTS.md`: backlog desain visual yang sengaja ditunda.
- `docs/MUMU_TESTING_GUIDE.md`: flow standar MuMu dua device, Owner test, screenshot binary-safe, dan audit vision.
