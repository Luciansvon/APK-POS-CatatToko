# Catatan Patch APK

## Aturan pencatatan

- File APK distribusi memakai nama tetap dan build terbaru menimpa build sebelumnya.
- Riwayat perubahan disimpan di dokumen ini, bukan pada nama file APK.
- Setiap patch wajib menaikkan `versionCode`.
- `versionName` mengikuti pola:
  - patch perbaikan: `0.1.1`;
  - fitur baru yang masih kompatibel: `0.2.0`;
  - rilis stabil besar: `1.0.0`.
- Sebelum APK ditimpa, catat alasan update, perubahan, kekurangan, masalah yang diketahui, dan bukti tes.
- Patch belum dianggap siap dibagikan jika catatan versinya belum ada.

## Nama APK tetap

```text
dist/debug/Kasir-Retail-UMKM.apk
dist/debug/Kasir-Grosir-Agen.apk
dist/debug/Kasir-Kuliner-PKL.apk
```

## Template patch berikutnya

```markdown
## Versi x.y.z - YYYY-MM-DD

Status: Draft / Siap dites / Siap dibagikan

### Kenapa versi ini dibuat

- Alasan utama pembaruan.

### Perubahan

- Fitur, tampilan, atau bug yang diperbarui.

### Kekurangan yang masih ada

- Fitur yang belum tersedia.

### Masalah yang diketahui

- Masalah yang masih bisa terjadi dan cara menghindarinya.

### Verifikasi

- Unit test:
- Build:
- Smoke test:
- HP/emulator:
- Kondisi offline:

### APK yang ditimpa

- Retail:
- Grosir:
- Kuliner:
```

## Versi 0.2.1 - 2026-07-30

Status: Siap dibagikan sebagai APK debug

### Kenapa versi ini dibuat

- Memisahkan akses pekerja dan Owner sesuai operasional satu HP.
- Membuat fungsi khusus tiap APK lebih gampang ditemukan.
- Memperbaiki logo launcher yang terpotong atau tidak pas pada bentuk ikon Android.

### Perubahan

- Aplikasi selalu mulai dalam Mode Kasir/Pekerja.
- Pekerja hanya dapat membuka kasir serta melihat stok produk dan total transaksi aktif.
- Operasional, keuangan, laporan, profil, backup, dan restore baru muncul setelah PIN Owner benar.
- Satu PIN Owner menggantikan istilah PIN Laporan dan tetap disimpan sebagai salted hash.
- Sesi Owner terkunci saat aplikasi masuk background atau Owner menekan `Keluar Mode Owner`.
- Shortcut utama Retail membuka Produk dan Piutang, Grosir membuka alat Grosir, dan Kuliner membuka Pesanan/Kuliner.
- Foreground ikon adaptif tiga flavor diganti dengan PNG transparan yang berada di safe zone Android.
- Menaikkan `versionCode` ke `4` dan `versionName` ke `0.2.1`.

### Kekurangan yang masih ada

- Perombakan visual penuh tetap ditunda.
- Belum ada akun pekerja individual, histori siapa yang menjalankan transaksi, atau pembagian akses per pekerja.
- Laporan laba/HPP dan fitur cloud tetap di luar scope.

### Masalah yang diketahui

- APK masih memakai debug signing key.
- Pengujian HP Android fisik belum dilakukan.

### Verifikasi

- Unit test: `40` test per flavor, total `120`, seluruhnya lulus.
- Build: `assembleDebug` dan tiga APK androidTest lulus.
- Connected Retail: `15/15` lulus pada MuMu Player Android 12.
- Connected Wholesale: `14` lulus dan `1` checkout Retail-only dilewati.
- Connected Culinary: `14` lulus dan `1` checkout Retail-only dilewati.
- Smoke manual MuMu membuktikan Mode Kasir/Pekerja tidak menampilkan navigasi Owner, stok dan total transaksi aktif tetap terlihat, serta navigasi muncul setelah PIN Owner dibuat.
- Launcher MuMu menampilkan ketiga ikon tanpa logo terpotong.
- Manifest build memakai `versionCode 4` dan `versionName 0.2.1-<flavor>`.
- Kondisi offline tetap terjaga karena fungsi inti memakai Room lokal dan APK tidak meminta permission internet.
- Packaging final:
  - Retail SHA-256 `C07EB80C016F311C16192701C89FF82E11CA156EB571303B4A255D18A982A5D5`;
  - Grosir SHA-256 `B5C1A41110C3CCA3460DC50CE3831716F24124E6B29E40D67D395EE7B812B29D`;
  - Kuliner SHA-256 `DBAC358CAD53302EC538A0D4F9188C8E4694FFC7720AC9CE0A1A349A28912259`.

### APK yang ditimpa

- Retail: `dist/debug/Kasir-Retail-UMKM.apk`
- Grosir: `dist/debug/Kasir-Grosir-Agen.apk`
- Kuliner: `dist/debug/Kasir-Kuliner-PKL.apk`

## Versi 0.2.0 - 2026-07-30

Status: Siap dibagikan sebagai APK debug

### Kenapa versi ini dibuat

- Menyelesaikan fungsi operasional offline yang sudah disetujui untuk tiga APK.
- Mengganti build kasir awal yang masih prematur dengan shared core operasional yang dapat dipakai owner dari satu HP.

### Perubahan

- Menambahkan profil usaha lokal dan navigasi Kasir, Operasional, Keuangan, Laporan, serta Lainnya.
- Menambahkan kelola kategori, produk, varian, status aktif, stok, dan riwayat pergerakan.
- Menambahkan supplier, pembelian atomik, kas masuk/keluar, pengeluaran, utang, piutang, dan pembayaran bertahap.
- Menambahkan daftar serta detail transaksi.
- Menambahkan laporan omzet, transaksi, penerimaan per metode, kas, pengeluaran, utang, dan piutang.
- Menambahkan PIN Laporan offline yang disimpan sebagai salted hash, flow ganti PIN, dan penguncian sesi saat aplikasi masuk background.
- Menambahkan pekerja harian, freelancer/panggilan, kehadiran, komponen upah, pekerjaan, dan pembayaran.
- Menambahkan perubahan tarif harian bertanggal serta input cicilan pembayaran freelancer.
- Menambahkan backup lokal berversi, SHA-256, berbagi file, preview identitas, safety backup, restore atomik, integrity check, dan rollback.
- Menambahkan multi-satuan serta harga bertingkat khusus Grosir.
- Menambahkan topping, catatan item, status pesanan, resep sederhana, dan pengurangan bahan khusus Kuliner.
- Menambahkan penjualan piutang khusus Retail dan Grosir.
- Menambahkan migrasi database eksplisit dari skema 1 ke skema 2.
- Memperbaiki checkpoint backup WAL, dependency serialization test, layout pembayaran landscape, test tag struk, dan kode jenis penyesuaian stok.
- Menaikkan `versionCode` ke `3` dan `versionName` ke `0.2.0`.

### Kekurangan yang masih ada

- Perombakan visual khusus ditunda sesuai keputusan user.
- Laporan laba/HPP belum tersedia karena metode penilaian HPP belum disetujui.
- Cloud, akun online, sinkronisasi, multi-device, pajak otomatis, BPJS, payroll formal, printer, marketplace, dan payment gateway belum termasuk scope.
- Nama produk dan branding final belum dikunci.
- APK masih ditandatangani memakai debug key.

### Masalah yang diketahui

- MuMu Player sempat membuat proses sistem Android crash di `RecentTasks` ketika suite Wholesale dan Culinary dijalankan beruntun. APK tidak crash; menjalankan satu flavor per command berhasil.
- Belum diuji pada HP Android fisik.

### Verifikasi

- Unit test: `34` test per flavor, total `102`, seluruhnya lulus.
- Build: `assembleDebug` dan tiga APK androidTest lulus dalam final command (`BUILD SUCCESSFUL`).
- Connected test Retail: full suite `15/15` lulus pada MuMu Player Android 12.
- Connected smoke Wholesale: `4` test, `3` lulus dan `1` checkout Retail-only dilewati.
- Connected smoke Culinary: `4` test, `3` lulus dan `1` checkout Retail-only dilewati.
- Migrasi, operasi atomik, multi-satuan, resep/bahan, PIN, backup/restore, serta penolakan backup rusak diuji pada MuMu.
- Manifest: ketiga APK memakai `versionCode 3`, target SDK 36, minimum SDK 23, dan tidak meminta permission `INTERNET`.
- Kondisi offline: fungsi inti memakai Room lokal; manifest ketiga flavor tidak mempunyai izin internet.

### APK yang ditimpa

- Retail: `dist/debug/Kasir-Retail-UMKM.apk`
- Grosir: `dist/debug/Kasir-Grosir-Agen.apk`
- Kuliner: `dist/debug/Kasir-Kuliner-PKL.apk`

## Versi 0.1.1 - 2026-07-29

Status: Siap dibagikan sebagai APK debug

### Kenapa versi ini dibuat

- Mengganti ikon launcher sementara dengan logo POS Material 3 yang sudah disiapkan untuk setiap jenis usaha.

### Perubahan

- Retail dan UMKM memakai ikon POS Jade.
- Grosir dan Agen memakai ikon POS Cobalt.
- Kuliner dan PKL memakai ikon POS Terracotta.
- Menambahkan ukuran ikon Android mdpi sampai xxxhdpi, versi bulat, dan adaptive icon Android 8 ke atas.
- Menaikkan `versionCode` ke `2` dan `versionName` ke `0.1.1`.

### Kekurangan yang masih ada

- Nama produk dan branding final masih belum dikunci.
- APK masih ditandatangani memakai debug key.

### Masalah yang diketahui

- Ikon belum diperiksa langsung pada launcher HP fisik atau emulator.
- Tampilan akhir dapat mengikuti bentuk mask icon yang dipilih launcher Android.

### Verifikasi

- Unit test: 24 lulus, 0 gagal pada tiga flavor.
- Build: tiga APK debug dan tiga APK androidTest berhasil dikompilasi.
- Smoke test: manifest APK versi `0.1.1` menunjuk adaptive launcher icon; semua ukuran mdpi sampai xxxhdpi dan foreground berwarna sesuai flavor terbukti ada di APK.
- HP/emulator: tidak dijalankan karena connected test membutuhkan konfirmasi user.
- Kondisi offline: tidak ada perubahan pada izin atau alur data aplikasi.

### APK yang ditimpa

- Retail: `dist/debug/Kasir-Retail-UMKM.apk`
- Grosir: `dist/debug/Kasir-Grosir-Agen.apk`
- Kuliner: `dist/debug/Kasir-Kuliner-PKL.apk`

## Versi 0.1.0 - 2026-07-29

Status: Siap dites sebagai APK debug

### Kenapa versi ini dibuat

- Membuat fondasi kasir Android offline dari satu source bersama.
- Memisahkan Retail dan UMKM, Grosir dan Agen, serta Kuliner dan PKL sebagai APK dengan identitas dan warna berbeda.

### Perubahan

- Menambahkan katalog, pencarian, kategori, stok, dan varian.
- Menambahkan keranjang persisten.
- Menambahkan pembayaran Tunai, QRIS, dan Transfer.
- Menambahkan validasi uang kurang dan perhitungan kembalian.
- Menambahkan transaksi Room atomik, snapshot item, dan perlindungan double submit.
- Menambahkan pengurangan serta riwayat pergerakan stok.
- Menambahkan struk PNG dan kalkulator offline.
- Menambahkan layout adaptif HP dan tablet.

### Kekurangan yang masih ada

- Belum ada kelola produk dan kategori dari UI.
- Belum ada pembelian dan supplier.
- Belum ada laporan dan PIN Laporan.
- Belum ada pengeluaran, utang, piutang, pajak, pegawai, shift, dan gaji.
- Belum ada backup dan restore.
- Belum ada printer Bluetooth atau cloud.
- APK masih ditandatangani memakai debug key.

### Masalah yang diketahui

- Smoke test terakhir pada emulator tidak diulang setelah assertion test diperbaiki karena akses emulator dihentikan sesuai permintaan user.
- Pengujian pada HP fisik belum dilakukan.
- Audit UI generik belum mendukung source Kotlin/Compose sehingga tidak menjadi bukti aksesibilitas.

### Verifikasi

- Unit test: 24 lulus, 0 gagal.
- Build: `BUILD SUCCESSFUL`, tiga APK dan tiga androidTest APK berhasil dikompilasi.
- Smoke test: flow Retail sempat mencapai struk dengan kembalian `Rp8.000`.
- HP/emulator: belum diuji ulang; penggunaan emulator membutuhkan izin user.
- Kondisi offline: manifest APK tidak mempunyai izin internet.

### APK yang ditimpa

- Retail: `dist/debug/Kasir-Retail-UMKM.apk`
- Grosir: `dist/debug/Kasir-Grosir-Agen.apk`
- Kuliner: `dist/debug/Kasir-Kuliner-PKL.apk`
