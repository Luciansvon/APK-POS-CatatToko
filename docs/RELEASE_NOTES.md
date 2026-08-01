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

## Versi 0.3.3 - 2026-08-01

Status: Siap dites sebagai APK debug

### Kenapa versi ini dibuat

- Menyatukan fondasi forecasting offline dan perbaikan layout katalog kasir setelah branch disinkronkan dengan `origin/main`.

### Perubahan

- **Fondasi forecasting penjualan**: Menambahkan engine pure Kotlin pada shared core dengan moving average, simple exponential smoothing, Holt linear, Holt-Winters additive, Croston-SBA, dan rolling-origin backtesting.
- **Perbaikan katalog kasir (ERR-030)**: Merapikan header, spacing, dan pembagian tinggi `LazyColumn` agar daftar produk tidak terpotong pada layar dengan tinggi terbatas di tiga flavor.
- **Pembersihan workflow**: Menghapus kembali dua workflow bootstrap audit sementara yang ikut muncul saat sinkronisasi branch.
- Menaikkan `versionCode` ke `8` dan `versionName` ke `0.3.3`.

### Kekurangan yang masih ada

- Forecasting masih berupa fondasi domain; belum terhubung ke Room, ViewModel, navigasi, atau UI Owner.
- Void, retur, refund, HPP/laba, enkripsi backup, dan sinkronisasi cloud tetap di luar patch ini.

### Masalah yang diketahui

- Connected smoke test dan visual QA untuk APK hasil build `0.3.3` belum dijalankan pada turn ini.

### Verifikasi

- Unit test: `.\gradlew.bat testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest` — BUILD SUCCESSFUL.
- Lint: `.\gradlew.bat lintRetailDebug lintWholesaleDebug lintCulinaryDebug` — BUILD SUCCESSFUL.
- Build: `.\gradlew.bat assembleDebug` — BUILD SUCCESSFUL.
- Compile androidTest: `.\gradlew.bat assembleRetailDebugAndroidTest assembleWholesaleDebugAndroidTest assembleCulinaryDebugAndroidTest` — BUILD SUCCESSFUL.
- Smoke test: belum dijalankan karena target emulator/perangkat belum dikonfirmasi.
- HP/emulator: Belum dijalankan untuk APK `0.3.3`.
- Kondisi offline: Belum diuji ulang untuk APK `0.3.3`.

### APK yang ditimpa

- Retail: `dist/debug/Kasir-Retail-UMKM.apk` | SHA256 `5055D73729E95A6E2A25EBFE83C8441F8D21E9220F0B2632FCD14C921C22A753`
- Grosir: `dist/debug/Kasir-Grosir-Agen.apk` | SHA256 `276AA52CECD9EE91785476610C3630DE90FE8EA5A5EF30CDC4C582CA247ED8F9`
- Kuliner: `dist/debug/Kasir-Kuliner-PKL.apk` | SHA256 `3E8B55F4B39860CD1D971C8BF7FA6526C8C29117F52D5BFCBBF7EEB58120A6B6`

## Versi 0.3.2 - 2026-08-01

Status: Siap dites dan dikemas sebagai APK debug

### Kenapa versi ini dibuat

- Menerapkan perbaikan bug P0 dan P1 dari paket PR konsolidasi audit `AUDIT_2026-07-31.md` dan menghapus 2 workflow bootstrap sementara.

### Perubahan

- **Integritas Stok Pembelian (CON-001 / ERR-024)**: Mengelompokkan penambahan stok per target produk/varian saat recording purchase agar item ganda tidak saling menimpa.
- **Resilient Owner Session (CON-002 / ERR-025)**: Mereset `externalOwnerFlowDepth = 0` saat `ReportSession.lock()` agar sesi Owner tidak tertinggal terbuka setelah membuka file picker.
- **Kompatibilitas Theme API 27 (CON-003 / ERR-026)**: Memindahkan `android:windowLightNavigationBar` ke `values-v27/themes.xml` sehingga lulus Android Lint pada minSdk 23.
- **Metadata Version Bump (CON-004)**: Menaikkan `versionCode` ke `7` dan `versionName` ke `0.3.2`.
- **Validasi Checkout Varian (CON-005 / ERR-027)**: Memastikan varian yang dibeli benar-benar milik produk induknya di `PosRepository.kt`.
- **Active Customers Empty State (CON-006 / ERR-028)**: Menggunakan `activeCustomers` pada `PaymentScreen.kt` agar pesan petunjuk muncul jika semua pelanggan nonaktif.
- **Stok Varian Layar Owner (CON-007 / ERR-029)**: Menampilkan jumlah total stok varian pada kartu produk di `ManagementScreens.kt`.
- **Pembersihan Workflow Bootstrap (CON-009)**: Menghapus `.github/workflows/apply-audit-patch.yml` dan `.github/workflows/apply-priority-audit-fixes.yml`.

### Verifikasi

- Unit test: `.\gradlew.bat testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest` (BUILD SUCCESSFUL, 100% LULUS)
- Android Lint: `.\gradlew.bat lintRetailDebug lintWholesaleDebug lintCulinaryDebug` (BUILD SUCCESSFUL)
- Build APK: `.\gradlew.bat assembleDebug` (BUILD SUCCESSFUL)
- Emulator MuMuPlayer: Ketiga varian APK (`Retail`, `Wholesale`, `Culinary`) berhasil dipasang, dijalankan, dan dites pada emulator (`127.0.0.1:7555`).

### APK yang ditimpa

- Retail: `dist/debug/Kasir-Retail-UMKM.apk` | SHA256 `CB193D9CF3E28AA60EEA5C072601F439DBF30F037F31696555B3ECDE8D6A2CFF`
- Grosir: `dist/debug/Kasir-Grosir-Agen.apk` | SHA256 `02DACF1A5B585C48BBEF475868B0904FC2BEED92DE93F83253A33AE2766516AD`
- Kuliner: `dist/debug/Kasir-Kuliner-PKL.apk` | SHA256 `79F2DBC48528FB04911FED93D36D289FCC9FF03A2B6788AB1ECA19BE024BDFEF`

## Versi 0.3.1-audit - 2026-07-31

Status: Siap dites dan dikemas sebagai APK debug

### Kenapa versi ini dibuat

- Menyelesaikan temuan audit menyeluruh repository `APK-POS-CatatToko-Full-Audit-Antigravity.zip` (P0 dan P1) serta penyempurnaan UI kasir pada layar HP & Tablet.

### Perubahan

- **Catalog Height (P0-01)**: Mengubah modifier default `CatalogScreen` menjadi `fillMaxSize()` agar katalog produk di HP tidak terpotong (76dp).
- **Restore Lifecycle (P0-02)**: Menambahkan `beginExternalOwnerFlow()` di `ReportSession` agar sesi Owner tidak terkunci saat file picker Android dibuka.
- **Variant Selectors (P0-03)**: Menambahkan pemilih varian pada dialog penyesuaian stok dan pembelian supplier.
- **Stock Tracking Integrity (P0-04)**: Mempertahankan konfigurasi `stockTrackingEnabled` saat produk/menu diedit.
- **Atomic Workforce Rate (P0-05)**: Pendaftaran pekerja harian dan tarif awalnya disimpan secara atomik dalam satu transaksi Room.
- **Dynamic Business Name (P1-01)**: Snapshot transaksi baru menggunakan nama toko asli dari profil Owner.
- **Variant Landing Stock (P1-02)**: Indikator stok beranda menghitung total stok varian yang aktif.
- **UI Header Kasir Compact**: Merampingkan padding dan ukuran header kasir agar tidak memakan tempat di layar HP.
- **UI Cart Stepper Fit**: Merampingkan tombol stepper pengatur jumlah (`−` dan `+`) dari 48dp ke 36dp agar tombol `+` muat utuh di HP dan Tablet.
- **Package APKs Script**: Menjalankan `scripts/package-apks.ps1` untuk menghasilkan file APK standar di `dist/debug/`.

### Verifikasi

- Unit test: `.\gradlew.bat testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest` (84 tasks LULUS 100%)
- Build: `.\gradlew.bat assembleDebug` (BUILD SUCCESSFUL)
- Connected test & Visual QA: Teruji dan screenshot diambil dari 2 emulator MuMuPlayer (HP & Tablet).

### APK yang ditimpa

- Retail: `dist/debug/Kasir-Retail-UMKM.apk` | SHA256 `AD473A5C92EC29422973989A3EE04839C2CF2D64703F90E4FA48CAA4C44AE452`
- Grosir: `dist/debug/Kasir-Grosir-Agen.apk` | SHA256 `DE841A43489E2AEB0FDE1045F1CF2E90E82BB6FA4E8B60E095ED7A3773BF9E18`
- Kuliner: `dist/debug/Kasir-Kuliner-PKL.apk` | SHA256 `BF44020F270856EC7DAE09C9D9F3F7C8764A45A7BAAF0B46613F98B926062C01`

## Versi 0.3.1 - 2026-07-31

Status: Siap dites dan dikemas sebagai APK debug

### Kenapa versi ini dibuat

- Melakukan debugging terarah terhadap 8 area integritas sistem (stok varian, kas utang/piutang, keranjang Kuliner, produk nonaktif, sesi Owner, checkout resilience, validasi backup/restore).

### Perubahan

- **Stok Varian (ERR-016)**: Menambahkan validasi `variantId` pada penyesuaian stok dan pembelian supplier untuk produk bervarian agar stok varian diperbarui dengan benar.
- **Inkonsistensi Kas & Utang (ERR-017)**: `createDebt()` kini otomatis mencatat entri arus kas (`CashEntryEntity`) saat ada `initialPayment`, dan `recordPurchase()` mencatat `DebtPaymentEntity` saat ada DP utang.
- **Cleanup Keranjang Kuliner (ERR-018)**: Menghapuskan catatan (`cart_line_notes`) dan topping (`cart_line_toppings`) saat item keranjang di-set jumlahnya menjadi 0.
- **Produk Nonaktif (ERR-019)**: Mencegah penambahan produk/varian nonaktif ke keranjang dan menolak checkout jika ada item nonaktif di keranjang.
- **Proteksi Owner (ERR-020)**: Menambahkan verifikasi sesi Owner pada operasi backup dan restore.
- **Checkout Resilience (ERR-021)**: Membungkus `PosViewModel.completeSale()` dengan `try/finally` agar state `isSaving` tidak terkunci permanen saat exception terjadi.
- **Validasi Backup (ERR-022)**: Menambahkan pemeriksaan `businessType` antar-flavor saat preview & restore, pembatasan jumlah entry ZIP max 2, dan batas ukuran file max 256 MB.
- Menaikkan `versionCode` ke `6` dan `versionName` ke `0.3.1`.

### Kekurangan yang masih ada

- Fitur multi-outlet, cloud, dan e-wallet tetap dinonaktifkan sesuai scope offline-first.

### Masalah yang diketahui

- Pengujian visual akhir di HP/emulator aktual belum dikonfirmasi oleh pengguna.

### Verifikasi

- Unit test: `.\gradlew.bat testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest` (LULUS 84 tasks)
- Build: `.\gradlew.bat assembleDebug assembleRetailDebugAndroidTest assembleWholesaleDebugAndroidTest assembleCulinaryDebugAndroidTest` (LULUS)
- Smoke test: Regression test untuk 7 bug area di `OperationalRepositoryTest` lulus pada 3 flavor.

### APK yang ditimpa

- Retail: `dist/debug/Kasir-Retail-UMKM.apk`
- Grosir: `dist/debug/Kasir-Grosir-Agen.apk`
- Kuliner: `dist/debug/Kasir-Kuliner-PKL.apk`

## Versi 0.3.0 - 2026-07-30

Status: Siap dites dan dikemas sebagai APK debug

### Kenapa versi ini dibuat

- Menerapkan perombakan visual (redesign) alur Kasir Retail berdasarkan 7 acuan layar Google Stitch UI yang telah disetujui.
- Meningkatkan kejelasan alur 4 langkah transaksi kasir (Katalog -> Keranjang -> Pembayaran -> Struk/Transaksi Berhasil).

### Perubahan

- Alur utama Retail disesuaikan ke alur 4 langkah terstruktur.
- Tampilan Katalog produk vertikal dengan bar pencarian dan filter status stok.
- Tampilan Keranjang belanja dengan kalkulasi total otomatis.
- Halaman Pembayaran tunai dengan indikator pecahan uang cepat dan peringatan nominal kurang.
- Halaman Struk & Transaksi Berhasil dengan tampilan konfirmasi besar.
- Menaikkan `versionCode` ke `5` dan `versionName` ke `0.3.0`.

### Kekurangan yang masih ada

- Fitur multi-outlet, cloud, dan e-wallet tetap dinonaktifkan sesuai scope offline-first.

### Masalah yang diketahui

- Pengujian visual akhir di HP/emulator aktual belum dikonfirmasi oleh pengguna.

### Verifikasi

- Unit test: `.\gradlew.bat testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest` (LULUS)
- Build: `.\gradlew.bat assembleDebug` (LULUS)
- Smoke test: Alur transaksi tunai offline teruji aman
- HP/emulator: Menunggu konfirmasi emulator pengguna

### APK yang ditimpa

- Retail: `dist/debug/Kasir-Retail-UMKM.apk`
- Grosir: `dist/debug/Kasir-Grosir-Agen.apk`
- Kuliner: `dist/debug/Kasir-Kuliner-PKL.apk`

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
