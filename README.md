<p align="center"><img src="app/src/retail/res/mipmap-xxxhdpi/ic_launcher.png" width="128" alt="Logo CatatToko"></p>

# Usaha Kecil Suite

Satu source Android offline-first untuk tiga APK operasional:

- Retail dan UMKM;
- Grosir dan Agen;
- Kuliner dan Pedagang Kaki Lima.

Versi target publik saat ini: `0.7.3` (`versionCode 25`) dengan redesign Owner/non-Kasir dan startup branding per flavor. Kandidat production signed sudah terverifikasi; publikasi menunggu merge dan GitHub Release.

## Download APK v0.7.3

Pilih APK sesuai jenis usaha:

- [Download CatatToko Retail dan UMKM](https://github.com/Luciansvon/APK-POS-CatatToko/releases/download/v0.7.3/CatatToko-Retail.apk)
- [Download CatatToko Grosir dan Agen](https://github.com/Luciansvon/APK-POS-CatatToko/releases/download/v0.7.3/CatatToko-Grosir.apk)
- [Download CatatToko Kuliner dan PKL](https://github.com/Luciansvon/APK-POS-CatatToko/releases/download/v0.7.3/CatatToko-Kuliner.apk)

[Lihat catatan rilis CatatToko v0.7.3](https://github.com/Luciansvon/APK-POS-CatatToko/releases/tag/v0.7.3).

> **Status rilis:** artifact signed v0.7.3 sudah terverifikasi, tetapi link baru aktif setelah merge dan GitHub Release dipublikasikan. PR dan release masih sengaja ditahan.

Aplikasi selalu mulai dalam Mode Kasir/Pekerja. Pada pemasangan pertama, panduan wajib menjelaskan Mode Kasir/Pekerja dan Mode Owner tanpa tombol lewati. Pekerja hanya dapat memakai kasir, melihat stok produk dan total transaksi aktif, serta membuka shift. Owner dapat memakai kasir tanpa membuka shift pekerja. Operasional, keuangan, laporan, profil, backup, restore, impor catatan lama, dan export Excel tetap baru muncul setelah PIN Owner benar.

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
- impor histori dari JSON hasil AI eksternal dengan validasi, review Owner, dan deteksi duplikat tanpa API key di APK;
- export `.xlsx` offline terstruktur dengan `Info Export`, `Ringkasan`, tabel operasional, dan lebar kolom otomatis untuk dibagikan Owner;
- struk dan berbagi PNG.
- barcode offline Retail/Grosir dengan mapping produk, varian, dan satuan; Kuliner menyembunyikannya pada V1.

## Fungsi khusus APK

| APK | Fungsi khusus |
|---|---|
| Retail dan UMKM | pelanggan, penjualan piutang, dan barcode offline |
| Grosir dan Agen | multi-satuan pcs/pak/dus, konversi stok, harga bertingkat, pelanggan, piutang, dan barcode per satuan/varian |
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

APK produksi memakai gate terpisah dan tidak boleh diambil dari `dist/debug`.
Isi secret `CATATTOKO_RELEASE_STORE_FILE`, `CATATTOKO_RELEASE_STORE_PASSWORD`,
`CATATTOKO_RELEASE_KEY_ALIAS`, dan `CATATTOKO_RELEASE_KEY_PASSWORD`, lalu jalankan:

```powershell
.\gradlew.bat verifyReleaseSigningReady assembleRelease
.\scripts\package-release-apks.ps1
```

Script produksi menolak APK unsigned dan debug certificate sebelum menyalin hasil ke
`dist/release/`. Workflow manual `.github/workflows/android-release.yml` memakai gate yang sama.

## Dokumentasi

- `docs/superpowers/specs/2026-07-30-offline-operations-suite-design.md`: spesifikasi rilis fitur.
- `docs/ARCHITECTURE.md`: arsitektur dan batas sistem.
- `docs/WORKLOG.md`: pekerjaan serta bukti verifikasi.
- `docs/ERROR_SOLUTIONS.md`: gejala, root cause, solusi, dan bukti bugfix.
- `docs/RELEASE_NOTES.md`: riwayat versi APK.
- `docs/UI_UX_REQUIREMENTS.md`: backlog desain visual yang sengaja ditunda.
- `docs/MUMU_TESTING_GUIDE.md`: flow standar MuMu dua device, Owner test, screenshot binary-safe, dan audit vision.
