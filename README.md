# CatatToko

POS Android offline-first untuk usaha kecil, dengan tiga APK dari satu source code:

- **Retail & UMKM**
- **Grosir & Agen**
- **Kuliner & PKL**

Versi publik terbaru: **v0.7.1** (`versionCode 23`) — rilis produksi signed, tetap berjalan lokal di HP tanpa akun, server, atau koneksi internet untuk fungsi utama.

## Download APK

Pilih APK sesuai jenis usaha:

- **Retail & UMKM** — [Download CatatToko-Retail.apk](https://github.com/Luciansvon/APK-POS-CatatToko/releases/download/v0.7.1/CatatToko-Retail.apk)
- **Grosir & Agen** — [Download CatatToko-Grosir.apk](https://github.com/Luciansvon/APK-POS-CatatToko/releases/download/v0.7.1/CatatToko-Grosir.apk)
- **Kuliner & PKL** — [Download CatatToko-Kuliner.apk](https://github.com/Luciansvon/APK-POS-CatatToko/releases/download/v0.7.1/CatatToko-Kuliner.apk)

[Catatan rilis v0.7.1](https://github.com/Luciansvon/APK-POS-CatatToko/releases/tag/v0.7.1) · [Semua release](https://github.com/Luciansvon/APK-POS-CatatToko/releases)

> **Update dari v0.7.0:** dapat dipasang langsung karena memakai certificate produksi yang sama.
>
> **Update dari v0.6.0:** buat dan bagikan backup ke luar HP terlebih dahulu, uninstall v0.6.0, install v0.7.1, lalu restore backup. v0.6.0 memakai debug certificate yang berbeda sehingga Android tidak menerima update langsung.

## Yang baru di v0.7.1

- **Backup terenkripsi** memakai AES-GCM + PBKDF2 dan PIN backup, termasuk perlindungan file serta media/foto produk.
- **Restore lebih aman** dengan validasi ZIP, path, hash, integritas data, serta kompatibilitas dengan backup versi lama.
- **Barcode lebih stabil** saat produk, varian, dan satuan diedit; ditambah gate anti-double-scan dan feedback jumlah item di keranjang.
- **Keamanan Owner diperkuat** melalui hashing PIN, lockout, pembersihan state sensitif, pembatalan proses saat sesi berubah, dan proteksi layar saat Owner terkunci.
- **Validasi data diperketat** untuk stok, harga tier, kategori, pembayaran, resep, tenaga kerja, serta import histori agar relasi atau nilai tidak valid tidak lolos ke database.
- **Laporan dan export diperbaiki**, termasuk pemisahan kas/nonkas, state grafik kosong, cache receipt/export, dan format barcode pada Excel.

## Fitur utama

### Kasir & transaksi

- katalog, kategori, produk, varian, keranjang, dan kalkulator;
- pembayaran **Tunai, QRIS, Transfer**, serta piutang pada flavor yang mendukung;
- daftar dan detail transaksi;
- struk transaksi dan berbagi sebagai PNG;
- mode Kasir/Pekerja sebagai mode awal aplikasi;
- Owner tetap dapat memakai kasir tanpa membuka shift pekerja.

### Stok & operasional

- stok berbasis riwayat pergerakan;
- penyesuaian stok wajib alasan;
- supplier dan pembelian;
- kas, pengeluaran, utang, piutang, dan cicilan;
- pekerja harian, freelancer/panggilan, kehadiran, pekerjaan, dan pembayaran.

### Barcode offline

Tersedia pada **Retail** dan **Grosir** tanpa koneksi internet:

- EAN-13;
- EAN-8;
- UPC-A;
- UPC-E;
- Code 128;
- mapping ke produk, varian, dan satuan;
- scan-to-fill untuk pendaftaran barcode;
- fallback input manual jika izin kamera tidak tersedia.

Kuliner menyembunyikan fitur barcode pada versi saat ini.

### Import catatan lama

Owner dapat mengimpor histori dari JSON hasil pembacaan AI eksternal tanpa menaruh API key atau model AI di dalam APK.

- schema `catattoko.history-import.v1`;
- validasi isi sebelum import;
- review per record;
- status data siap, perlu dicek, tidak diterapkan, atau duplikat;
- deteksi file dan record ganda;
- tanggal transaksi lama tetap dipertahankan;
- import tidak membuka shift dan tidak mengubah stok aktif secara sembarangan.

### Backup & restore

- backup lokal berversi;
- backup terenkripsi AES-GCM + PBKDF2;
- PIN khusus backup;
- validasi integritas, ZIP, path, dan hash;
- media/foto produk ikut dilindungi;
- berbagi file backup ke luar HP;
- restore aman dan tetap mendukung backup lama yang kompatibel.

### Laporan & export

- omzet;
- metode pembayaran;
- kas dan pengeluaran;
- utang dan piutang;
- export `.xlsx` offline;
- sheet `Info Export`, `Ringkasan`, data operasional, dan `Barcode Produk` bila tersedia;
- lebar kolom otomatis agar hasil export lebih mudah dibaca.

### Keamanan Owner

Operasional sensitif hanya tersedia setelah PIN Owner benar, termasuk:

- keuangan dan laporan;
- profil usaha;
- backup dan restore;
- import catatan lama;
- export Excel;
- pengelolaan data master dan fungsi Owner lain.

PIN Owner disimpan sebagai hash dan v0.7.1 memperkuat lockout serta lifecycle state sensitif.

## Perbedaan tiap APK

| APK | Fitur khusus |
|---|---|
| **Retail & UMKM** | pelanggan, penjualan piutang, barcode offline |
| **Grosir & Agen** | multi-satuan pcs/pak/dus, konversi stok, harga bertingkat, pelanggan, piutang, barcode per satuan/varian |
| **Kuliner & PKL** | topping, catatan item, antrean/status pesanan, resep sederhana, pengurangan bahan |

## Belum termasuk

Cloud, pajak otomatis, HPP/laba, BPJS, payroll formal, printer thermal, marketplace, payment gateway, sinkronisasi multi-device, dan QR barcode belum termasuk rilis v0.7.1.

## Build dan test

Gunakan **JDK 17 atau lebih baru**. Pull request diverifikasi otomatis melalui `.github/workflows/android-ci.yml` untuk seluruh flavor.

```powershell
.\gradlew.bat testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRetailDebugAndroidTest assembleWholesaleDebugAndroidTest assembleCulinaryDebugAndroidTest
.\gradlew.bat lintRetailDebug lintWholesaleDebug lintCulinaryDebug
```

Perintah unit test di atas tidak menjalankan test pada `app/src/androidTest`. Test instrumentasi harus dijalankan pada emulator/perangkat untuk masing-masing flavor.

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
Isi secret `CATATTOKO_RELEASE_STORE_FILE`, `CATATTOKO_RELEASE_STORE_PASSWORD`, `CATATTOKO_RELEASE_KEY_ALIAS`, dan `CATATTOKO_RELEASE_KEY_PASSWORD`, lalu jalankan:

```powershell
.\gradlew.bat verifyReleaseSigningReady assembleRelease
.\scripts\package-release-apks.ps1
```

Script produksi menolak APK unsigned dan debug certificate sebelum menyalin hasil ke `dist/release/`. Workflow manual `.github/workflows/android-release.yml` memakai gate yang sama.

## Dokumentasi

- `docs/superpowers/specs/2026-07-30-offline-operations-suite-design.md` — spesifikasi rilis fitur;
- `docs/ARCHITECTURE.md` — arsitektur dan batas sistem;
- `docs/WORKLOG.md` — pekerjaan serta bukti verifikasi;
- `docs/ERROR_SOLUTIONS.md` — gejala, root cause, solusi, dan bukti bugfix;
- `docs/RELEASE_NOTES.md` — riwayat versi APK;
- `docs/UI_UX_REQUIREMENTS.md` — backlog desain visual yang sengaja ditunda;
- `docs/MUMU_TESTING_GUIDE.md` — flow standar MuMu dua device, Owner test, screenshot binary-safe, dan audit vision.
