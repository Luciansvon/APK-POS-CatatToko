# Worklog Usaha Kecil Suite

## Tujuan

File ini mencatat pekerjaan yang benar-benar dilakukan pada project, keputusan yang sudah disetujui, bukti verifikasi, dan pekerjaan yang masih terbuka.

Worklog bukan pengganti:

- `AGENTS.md` untuk aturan kerja;
- `docs/ARCHITECTURE.md` untuk arsitektur;
- `docs/ERROR_SOLUTIONS.md` untuk bug dan root cause;
- spesifikasi fitur untuk requirement implementasi.

## Aturan pencatatan

- Tambahkan catatan secara kronologis.
- Jangan menghapus catatan lama hanya karena rencana berubah.
- Bedakan status `Selesai`, `Disetujui`, `Direncanakan`, `Ditunda`, dan `Belum diverifikasi`.
- Jangan mencatat rencana sebagai fitur yang sudah tersedia.
- Cantumkan file yang dibuat atau diubah.
- Cantumkan test, build, smoke test, atau pemeriksaan aktual yang dilakukan.
- Jika tidak ada kode yang berubah, tulis dengan jelas bahwa test dan build tidak dijalankan.
- Perubahan arsitektur harus disinkronkan dengan `docs/ARCHITECTURE.md`.
- Bugfix harus mempunyai entri di `docs/ERROR_SOLUTIONS.md`.

---

## 2026-08-02 - Perbaikan kontras header dan ukuran kolom export Excel

Status: Selesai dan diverifikasi pada APK Retail debug 0.4.8; tidak mengganti APK distribusi.

### Hasil

- Header tabel export sekarang memakai latar mint terang dengan teks teal gelap tebal agar terbaca di Excel desktop.
- Tinggi baris dibuat eksplisit: header 32pt, data 18pt, judul 24pt, dan subjudul 18pt.
- Lebar kolom diberi padding 3 karakter dengan batas 10 sampai 72 karakter supaya teks panjang tidak terlalu mepet.

### Bukti aktual

- testRetailDebugUnitTest --tests com.bimacore.usahakecil.export.ExcelWorkbookExporterTest lulus.
- Test unit Retail, Wholesale, dan Culinary, build debug, serta compile test APK tiga flavor lulus.
- APK Retail dan test APK dipasang pada emulator-5556; AnnualSalesExportTest menghasilkan workbook ulang dan lulus OK (1 test).
- Workbook terbaru ditarik dari folder Download APK ke artifacts/annual-demo/catattoko-demo-penjualan-tahun-2026.xlsx.
- Inspect workbook menemukan 20 sheet, 1.434 transaksi, 10 produk terjual, dan scan formula/error menemukan 0 entri.
- Render visual sheet Penjualan dan Produk Terjual menunjukkan header terbaca, tidak terpotong, dan kolom nomor struk memiliki ruang.

### File

- app/src/main/java/com/bimacore/usahakecil/export/ExcelWorkbookExporter.kt
- app/src/test/java/com/bimacore/usahakecil/export/ExcelWorkbookExporterTest.kt
- docs/ERROR_SOLUTIONS.md
- artifacts/annual-demo/catattoko-demo-penjualan-tahun-2026.xlsx

---

## 2026-08-02 - Fixture demo penjualan tahunan untuk audit export APK

Status: Selesai untuk validasi Retail; bukan perubahan fitur production dan tidak menaikkan versi APK.

### Hasil

- Menambahkan `AnnualSalesExportTest` yang membuat data penjualan Retail dari awal tahun berjalan sampai waktu test di database sementara APK.
- Pola data memakai kalender dan keranjang belanja deterministik: akhir pekan lebih ramai, Senin lebih sepi, periode musim sekolah menaikkan alat tulis, pakaian hanya muncul sebagai pembelian sesekali, dan pembayaran memakai campuran tunai, QRIS, transfer, serta piutang.
- Export dipanggil melalui `ExcelExportManager` milik APK, bukan generator workbook eksternal.
- File hasil ditarik dari emulator `emulator-5556` ke `artifacts/annual-demo/catattoko-demo-penjualan-tahun-2026.xlsx`.

### Bukti aktual

- Compile test Android Retail lulus.
- APK Retail dan test APK terpasang; `AnnualSalesExportTest` lulus `1/1` melalui Android instrumentation.
- Workbook hasil APK berisi periode `Tahun ini`, `1.434` transaksi, omzet `Rp38.876.500`, serta 10 produk terjual.
- Inspect workbook menemukan 20 sheet dan scan formula/error mencocokkan `0` entri error.
- Render visual 20 sheet selesai; sheet panjang memakai rentang tampilan terbatas karena render penuh melebihi batas tinggi renderer.

### Batasan

- Data demo dibuat di database sementara test agar database utama emulator tidak dihapus. File Excel sudah nyata dari jalur export APK, tetapi data demo tidak ditinggal sebagai data permanen di database aplikasi setelah test.

### File

- `app/src/androidTest/java/com/bimacore/usahakecil/report/AnnualSalesExportTest.kt`
- `artifacts/annual-demo/catattoko-demo-penjualan-tahun-2026.xlsx`

---

## 2026-08-02 - Redesign area Owner untuk pengguna UMKM

Status: Selesai dan siap diaudit sebagai APK debug `0.4.8`.

### Keputusan yang disetujui

- Memakai arah Laporan opsi 2 dan mempertahankan tombol penyimpanan Excel yang mudah dikenali.
- Memprioritaskan halaman Owner lain yang masih generik; Kasir dan isi Produk tidak diubah.
- Memakai bahasa usaha sehari-hari, angka utama yang jelas, tombol bertulisan, dan kondisi kosong yang menjelaskan langkah berikutnya.
- Mengganti label navigasi Retail `Piutang` menjadi `Keuangan`.

### Hasil implementasi

- Laporan memakai pemilih periode lebar penuh, omzet utama, tiga metrik ringkas, grafik penjualan, serta pintasan bertulisan `Buka` untuk arus kas dan rincian lengkap.
- `Simpan Laporan Excel` dan `Bagikan Excel` tampil sebagai tombol lebar penuh dengan tulisan dan ikon.
- Istilah baru yang tidak perlu berbahasa asing diganti menjadi `salinan data`, `pulihkan data`, `pemasok`, dan `pekerja panggilan`; detail perkiraan tidak lagi menampilkan istilah teknis `model`/`MAE`.
- Perbandingan sebelumnya memakai durasi berjalan yang sama; pilihan `Terjual` tidak tersedia untuk gabungan semua produk.
- Stok, Pembelian, Pekerja, Kas, Utang & Piutang, Transaksi, dan Lainnya memakai komponen Owner bersama.
- Pengaturan PIN dan keluar Owner ditempatkan pada bagian `Keamanan Owner` di Lainnya.

### Verifikasi akhir

- Unit test, lint, build debug, dan kompilasi AndroidTest Retail, Grosir, serta Kuliner lulus.
- Connected Retail lulus `47/47` per perangkat; Grosir dan Kuliner masing-masing `49` run dengan `2` test khusus Retail dilewati per perangkat, tanpa kegagalan.
- `ReportDemoTest` mengunci periode, grafik, Excel, batas semua produk, serta ketiadaan karakter rentang tanggal yang rusak.
- Visual QA Laporan dan halaman Owner lain lulus pada HP portrait serta tablet landscape; bukti ada di `design-qa.md` dan folder visualisasi `owner-redesign-0.4.8`.
- Tiga APK final disalin ke `dist/debug` dengan hash yang dicatat di `docs/RELEASE_NOTES.md`.

### File utama

- `app/src/main/java/com/bimacore/usahakecil/ui/OwnerDashboardComponents.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ReportDashboardComponents.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/AppDestination.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/ReportPeriod.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/report/ReportDemoTest.kt`

---

## 2026-08-02 - Dashboard laporan dan tren monitoring

Status: Selesai dan siap dibagikan sebagai APK debug `0.4.7`.

### Keputusan yang disetujui

- KPI laporan memakai grid dua kolom agar ringkas.
- KPI membandingkan periode aktif dengan periode sebelumnya yang setara.
- Grafik default adalah `Arus kas`; mode lain adalah `Penjualan` dan `Produk`.
- Granularitas grafik dapat dipilih terpisah: harian, mingguan, bulanan, tahunan.
- Bucket tanpa data tetap ditampilkan sebagai nol.

### Hasil implementasi

- Menambahkan `ReportTrendReport`, query baris penjualan/item/kas, dan agregasi bucket kalender lokal di shared core.
- Menambahkan status naik/turun/netral dengan arah penilaian berbeda untuk pendapatan dan pengeluaran.
- Menempatkan grafik pembayaran sebagai rincian sekunder; forecast tetap menjadi analisis stok.
- Menambahkan selector produk dan metrik `Omzet`/`Terjual`.

### File utama

- `app/src/main/java/com/bimacore/usahakecil/data/ReportPeriod.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/ReportTrend.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/OperationalDaos.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/ReportRepository.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ReportDashboardComponents.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`

### Verifikasi yang sudah dilakukan

- `testRetailDebugUnitTest` dan compile AndroidTest Retail lulus.
- Connected `ReportDemoTest` Retail lulus di `emulator-5554` dan `emulator-5556`.
- Test repository data kosong memeriksa bucket tren nol.

### Verifikasi akhir

- Unit test tiga flavor, assemble debug, dan compile AndroidTest tiga flavor lulus setelah patch label sumbu.
- Connected matrix tiga flavor lulus pada dua serial MuMu; Retail `47/47` per device, Wholesale dan Culinary `49` run per device dengan 2 test Retail-only dilewati.
- Targeted `ReportDemoTest` dan `ReportTrendRepositoryTest` lulus pada dua device setelah database test dikembalikan bersih.
- APK Retail `0.4.7` dipasang kembali di `emulator-5554` dan dibiarkan terpasang untuk uji user.
- Screenshot final disimpan di `C:\Users\shint\.codex\visualizations\2026\08\02\usaha-kecil-suite-0.4.7`.
- Audit visual emulator memeriksa tinggi card KPI yang seragam, mode Arus kas/Penjualan/Produk, metrik Omzet/Terjual, selector produk, empat granularitas, dan label grafik tanpa ellipsis.
- Kontrol grafik diringkas menjadi dua dropdown inti (`Tampilan` dan `Rentang`); mode Produk memakai satu menu gabungan untuk produk serta ukuran Omzet/Terjual.
- Audit popup manual ADB memastikan seluruh opsi mode, produk, dan metrik tetap tersedia tanpa memenuhi layar; screenshot final disimpan di `artifacts/ui-qa/report-audit/`.
- APK tiga flavor dipackage ulang; hash final dicatat di `docs/RELEASE_NOTES.md`.

---

## 2026-08-01 - Standarisasi flow test MuMu dua device

Status: Selesai untuk dokumentasi; hasil connected test dan audit Owner dicatat terpisah sesuai bukti aktual.

### Hasil

- `docs/MUMU_TESTING_GUIDE.md` diperbarui sebagai prosedur wajib agent untuk ADB, dua serial MuMu, tiga flavor, targeted Owner test, install ulang setelah runner uninstall, screenshot PNG binary-safe, dan audit vision.
- Profil device yang terverifikasi: ASUS AI2205 portrait `1080x1920` dan ALT AL10 landscape efektif `1600x900`.
- `README.md` dan `AGENTS.md` sekarang menunjuk langsung ke panduan MuMu agar agent tidak mengulang trial-and-error.

### File

- `docs/MUMU_TESTING_GUIDE.md`
- `README.md`
- `AGENTS.md`

---

## 2026-07-29 - Inisialisasi wadah project

Status: Selesai.

### Tujuan

Membuat project baru yang terpisah dari MAUCAFE untuk keluarga APK operasional usaha kecil.

### Hasil

- Folder `usaha-kecil-suite` dibuat sebagai project terpisah.
- Repository dan source MAUCAFE tidak dipindahkan.
- Percobaan mengganti nama folder MAUCAFE dibatalkan sesuai instruksi user.
- Belum ada repository Git baru.
- Belum ada source aplikasi.

### Verifikasi aktual

- Folder `usaha-kecil-suite` terdeteksi di filesystem.
- Folder MAUCAFE lama tetap ada dan tidak berubah.

---

## 2026-07-29 - Penguncian arah produk awal

Status: Disetujui pada tingkat konsep.

### Keputusan yang disetujui

- Produk utama berupa APK Android offline-first.
- MVP berjalan di satu HP owner.
- Operasional dasar tidak bergantung pada akun, internet, atau cloud.
- Backup lokal dan export file menjadi mekanisme pemulihan awal.
- Cloud berlangganan baru dipertimbangkan setelah APK offline stabil dan digunakan nyata.
- Satu source bersama akan menghasilkan beberapa APK.
- Varian awal:
  - Warung dan UMKM;
  - Grosir dan Agen;
  - Kuliner dan Pedagang Kaki Lima.
- Tenaga kerja mendukung pekerja harian dan freelancer/panggilan.
- Kehadiran atau pekerjaan dicatat oleh owner dari HP-nya.
- Login, PIN, GPS, selfie, atau absensi mandiri pekerja tidak termasuk MVP.

### Catatan batas

- Urutan APK yang dibuat pertama belum disetujui final.
- Teknologi aplikasi dan database belum dipilih.
- Bisnis jasa seperti laundry, salon, dan bengkel belum masuk scope yang disetujui.
- Provider, harga, dan arsitektur cloud belum dipilih.

### Verifikasi aktual

Tidak ada test atau build karena tahap ini hanya mengunci arah produk.

---

## 2026-07-29 - Pembuatan dokumentasi dasar

Status: Selesai.

### File yang dibuat

- `AGENTS.md`
- `docs/ARCHITECTURE.md`
- `docs/ERROR_SOLUTIONS.md`
- `docs/WORKLOG.md`

### Isi utama

- Aturan kerja dan batas implementasi.
- Arsitektur offline satu HP owner.
- Shared core untuk tiga varian APK.
- Domain penjualan, stok, pembelian, pengeluaran, utang-piutang, pegawai, pajak opsional, laporan, backup, dan restore.
- Pemisahan tegas dari MAUCAFE.
- Format wajib pencatatan bug, root cause, solusi, serta bukti verifikasi.

### Verifikasi aktual

- Seluruh file dokumentasi terdeteksi di folder project.
- Struktur heading diperiksa.
- Tidak ada entri bug palsu di `docs/ERROR_SOLUTIONS.md`.
- Tidak ada kode aplikasi yang dibuat atau diubah.
- Test dan build tidak dijalankan karena belum ada toolchain.

---

## 2026-07-29 - Requirement kalkulator kasir

Status: Disetujui pada tingkat requirement.

### Keputusan yang disetujui

- Setiap varian yang mempunyai layar kasir wajib menyediakan kalkulator bawaan.
- Kalkulator wajib berfungsi tanpa internet.
- Kalkulator harus mudah dibuka dari layar kasir.
- Hasil kalkulator tidak boleh mengubah transaksi atau pencatatan keuangan tanpa tindakan konfirmasi yang jelas.

### Keputusan yang masih terbuka

- Kalkulator hanya berdiri sendiri atau dapat mengisi kolom uang diterima/kembalian melalui tombol `Gunakan hasil`. Keputusan ini kemudian diselesaikan pada entri `Penguncian flow kasir tunai`.

### Verifikasi aktual

- Requirement tercatat di `AGENTS.md`, `docs/ARCHITECTURE.md`, dan `docs/WORKLOG.md`.
- Tidak ada kode aplikasi yang dibuat atau diubah.
- Test dan build tidak dijalankan karena belum ada toolchain.

---

## 2026-07-29 - Pembatasan akses laporan penjualan

Status: Disetujui pada tingkat requirement.

### Keputusan yang disetujui

- Laporan penjualan hanya dapat dibuka oleh Admin atau Owner.
- Kasir biasa tidak dapat membaca laporan penjualan.
- Akses laporan membutuhkan verifikasi yang tetap berfungsi secara offline.
- Menyembunyikan tombol laporan saja tidak dianggap sebagai pengamanan.
- PIN tidak boleh disimpan dalam bentuk plaintext.

### Rekomendasi desain

- Gunakan PIN sebagai mekanisme utama MVP.
- Kunci kembali akses laporan ketika aplikasi ditutup atau setelah batas waktu tertentu.
- Biometrik dapat ditambahkan sebagai jalan pintas, bukan satu-satunya cara masuk.

### Keputusan yang masih terbuka

- Admin dan Owner memakai satu PIN bersama atau PIN yang berbeda. Keputusan ini kemudian diselesaikan pada entri `Penguncian satu PIN Laporan`.
- Lama akses laporan tetap terbuka sebelum meminta PIN lagi.

### Verifikasi aktual

- Requirement tercatat di `AGENTS.md`, `docs/ARCHITECTURE.md`, dan `docs/WORKLOG.md`.
- Tidak ada kode aplikasi yang dibuat atau diubah.
- Test dan build tidak dijalankan karena belum ada toolchain.

---

## 2026-07-29 - Penguncian satu PIN Laporan

Status: Disetujui.

### Keputusan yang disetujui

- MVP memakai satu PIN Laporan yang sama untuk Admin dan Owner.
- PIN tersebut hanya membuka area laporan yang dilindungi.
- MVP tidak membedakan identitas Admin dan Owner yang memasukkan PIN.
- Keputusan memakai PIN Admin dan PIN Owner terpisah tidak lagi terbuka untuk MVP.

### Verifikasi aktual

- Keputusan diperbarui di `AGENTS.md`, `docs/ARCHITECTURE.md`, dan `docs/WORKLOG.md`.
- Tidak ada kode aplikasi yang dibuat atau diubah.
- Test dan build tidak dijalankan karena belum ada toolchain.

---

## 2026-07-29 - Penguncian flow kasir tunai

Status: Disetujui pada tingkat requirement.

### Flow yang disetujui

1. Buyer memilih barang atau menu.
2. Kasir menekan barang atau menu pada aplikasi.
3. Aplikasi menghitung jumlah dan total belanja.
4. Kasir memasukkan uang tunai yang diberikan buyer.
5. Aplikasi langsung menghitung dan menampilkan kembalian.

### Aturan perhitungan

- `Kembalian = Uang Diterima - Total Belanja`.
- Total berubah otomatis ketika isi atau jumlah keranjang berubah.
- Kembalian berubah otomatis ketika nilai uang diterima berubah.
- Kalkulator umum tetap menjadi alat bantu terpisah dan tidak mengubah transaksi.
- Uang diterima dan kembalian menjadi bagian snapshot pembayaran transaksi.

### Keputusan yang masih terbuka

- Perilaku aplikasi ketika uang diterima kurang dari total belanja. Keputusan ini kemudian diselesaikan pada entri `Penguncian validasi uang tunai kurang`.
- Tindakan konfirmasi akhir untuk menyelesaikan transaksi. Keputusan ini kemudian diselesaikan pada entri `Penguncian Bayar & Selesai`.

### Verifikasi aktual

- Requirement diperbarui di `AGENTS.md`, `docs/ARCHITECTURE.md`, dan `docs/WORKLOG.md`.
- Tidak ada kode aplikasi yang dibuat atau diubah.
- Test dan build tidak dijalankan karena belum ada toolchain.

---

## 2026-07-29 - Penguncian validasi uang tunai kurang

Status: Disetujui.

### Keputusan yang disetujui

- Jika uang diterima lebih kecil daripada total belanja, transaksi tidak dapat diselesaikan.
- Tombol penyelesaian transaksi tetap nonaktif sampai uang diterima mencukupi.
- Layar menampilkan `Uang Kurang` beserta nominal selisihnya.
- Aplikasi tidak menampilkan nilai kembalian negatif.
- Ketika uang diterima sama dengan atau lebih besar daripada total, transaksi dapat dilanjutkan.

### Verifikasi aktual

- Keputusan diperbarui di `AGENTS.md`, `docs/ARCHITECTURE.md`, dan `docs/WORKLOG.md`.
- Tidak ada kode aplikasi yang dibuat atau diubah.
- Test dan build tidak dijalankan karena belum ada toolchain.

---

## 2026-07-29 - Penguncian Bayar & Selesai

Status: Disetujui.

### Keputusan yang disetujui

- Tombol konfirmasi akhir transaksi tunai bernama `Bayar & Selesai`.
- Transaksi baru dianggap selesai setelah tombol tersebut ditekan dan proses penyimpanan berhasil.
- Penyimpanan transaksi, pengurangan stok, pencatatan kas, dan pembaruan laporan harus berhasil sebagai satu operasi konsisten.
- Jika salah satu bagian gagal, sistem tidak boleh meninggalkan transaksi atau stok dalam kondisi setengah jadi.
- Setelah transaksi berhasil, nominal kembalian ditampilkan dengan ukuran besar.

### Verifikasi aktual

- Keputusan diperbarui di `AGENTS.md`, `docs/ARCHITECTURE.md`, dan `docs/WORKLOG.md`.
- Tidak ada kode aplikasi yang dibuat atau diubah.
- Test dan build tidak dijalankan karena belum ada toolchain.

---

## 2026-07-29 - Implementasi MVP kasir offline tiga varian

Status: Diimplementasikan dan dibangun sebagai APK debug.

### Hasil implementasi

- Satu source Kotlin, Jetpack Compose, dan Room menghasilkan tiga APK:
  - Retail dan UMKM dengan tema Jade;
  - Grosir dan Agen dengan tema Cobalt;
  - Kuliner dan PKL dengan tema Terracotta.
- Katalog adaptif HP/tablet, pencarian, kategori, stok, dan varian produk.
- Keranjang disimpan lokal dan tetap ada setelah aplikasi ditutup.
- Pembayaran Tunai, QRIS, dan Transfer dengan validasi uang kurang serta konfirmasi manual.
- Penyimpanan penjualan, snapshot item, pengurangan stok, dan pergerakan stok dilakukan dalam satu transaksi Room.
- Double tap pembayaran mengembalikan struk yang sama dan tidak membuat transaksi kedua.
- Struk dapat dibagikan sebagai PNG melalui FileProvider.
- Kalkulator kasir berdiri sendiri dan tidak mengubah keranjang.
- Aplikasi tidak meminta izin internet.

### Batas MVP saat ini

- APK masih debug dan belum ditandatangani dengan release key.
- Laporan dan PIN Laporan belum dibuat.
- CRUD produk, pembelian, supplier, pegawai, pajak, backup/restore, printer Bluetooth, dan cloud belum dibuat.
- Pengujian akhir tidak memasang APK ke emulator atau HP tanpa izin user.

### Verifikasi aktual

- `testRetailDebugUnitTest`, `testWholesaleDebugUnitTest`, dan `testCulinaryDebugUnitTest`: 24 test lulus, 0 gagal.
- `assembleDebug`: APK Retail, Wholesale, dan Culinary berhasil dibuat.
- Android test APK untuk ketiga flavor berhasil dikompilasi.
- Hasil Gradle final: `BUILD SUCCESSFUL`, 234 task.
- Pemeriksaan manifest APK memastikan package dan nama aplikasi berbeda serta tidak ada izin `INTERNET`.
- Audit UI generik tidak menemukan file yang dapat dipindai karena audit tersebut belum mendukung source Kotlin/Compose; hasilnya tidak dipakai sebagai bukti aksesibilitas.
- Pengujian perangkat fisik belum dilakukan.

---

## 2026-07-30 - Penyelesaian fitur operasional offline versi 0.2.0

Status: Selesai, diverifikasi, dan dipaketkan sebagai APK debug.

### Keputusan pelaksanaan

- User menyetujui penyelesaian fungsi tiga APK sesuai scope yang sudah dikunci.
- Perombakan visual ditunda; fungsi, keamanan data, validasi, dan keterpakaian dasar didahulukan.
- User memberi izin eksplisit untuk connected test memakai MuMu Player.
- Project tetap satu shared source; fitur khusus dikendalikan oleh konfigurasi capability flavor.

### Shared core yang diselesaikan

- Database Room dinaikkan ke skema 2 dengan migrasi eksplisit dari skema 1 tanpa destructive fallback.
- Profil usaha lokal dengan identitas stabil.
- Navigasi Kasir, Operasional, Keuangan, Laporan, dan Lainnya.
- Kelola kategori, produk, varian, status aktif, stok, serta riwayat pergerakan.
- Penyesuaian stok masuk, keluar, rusak, dan hilang dengan alasan wajib serta penolakan stok negatif.
- Supplier, pelanggan, pembelian, kas, pengeluaran, utang, piutang, dan pembayaran bertahap.
- Daftar dan detail transaksi.
- Penjualan mencatat penerimaan kas atau piutang dan menyimpan snapshot finansial.
- Laporan omzet, jumlah transaksi, penerimaan per metode, arus kas, pengeluaran, utang, dan piutang tanpa klaim laba/HPP.
- Satu PIN Laporan offline memakai salted PBKDF2 hash, sesi in-memory, ganti PIN, dan penguncian saat app background.
- Pekerja harian dan freelancer/panggilan, tarif bertanggal, kehadiran, komponen upah, pekerjaan, serta pembayaran.
- Backup ZIP lokal berversi dengan manifest, SHA-256, WAL checkpoint, berbagi file, preview identitas, safety backup, restore atomik, integrity check, dan rollback.

### Fungsi per flavor

- Retail: kasir umum, pelanggan, penjualan piutang, serta seluruh modul bersama yang relevan.
- Grosir: multi-satuan, faktor konversi ke stok dasar, harga bertingkat, pelanggan, dan piutang.
- Kuliner: topping, catatan item, status pesanan, resep sederhana, dan pengurangan bahan secara atomik.
- Capability test dan connected navigation smoke memastikan fitur Grosir tidak muncul di Kuliner/Retail serta fitur Kuliner tidak muncul di Grosir/Retail.

### Bug yang ditemukan dan diselesaikan

- Binary mismatch `kotlinx-serialization` membuat migration test crash.
- Backup melewatkan data WAL karena cursor checkpoint tidak dibaca.
- Bagian pembayaran tunai tertutup tombol selesai pada landscape.
- Test tag transaksi baru salah terpasang pada tombol berbagi struk.
- Form penyesuaian stok mengirim kode jenis yang tidak diterima repository.
- Edit produk mengaktifkan kembali produk nonaktif dan form stok edit memberi kesan stok dapat ditimpa tanpa histori.
- MuMu system server crash saat dua flavor diuji beruntun; stack trace membuktikan crash pada `RecentTasks` milik MuMu, lalu smoke test lulus ketika flavor dijalankan satu-satu.

Detail root cause, solusi, dan bukti ada di `docs/ERROR_SOLUTIONS.md` ERR-005 sampai ERR-011.

### Dokumentasi dan versi

- Menambahkan spesifikasi `docs/superpowers/specs/2026-07-30-offline-operations-suite-design.md`.
- Menambahkan rencana `docs/superpowers/plans/2026-07-30-offline-operations-suite.md`.
- Memperbarui `README.md`, `docs/ARCHITECTURE.md`, `docs/UI_UX_REQUIREMENTS.md`, `docs/ERROR_SOLUTIONS.md`, `docs/RELEASE_NOTES.md`, dan worklog ini.
- Menaikkan `versionCode` dari 2 ke 3.
- Menaikkan `versionName` dari 0.1.1 ke 0.2.0.

### Verifikasi aktual

- Unit test Retail: 34 lulus, 0 gagal.
- Unit test Wholesale: 34 lulus, 0 gagal.
- Unit test Culinary: 34 lulus, 0 gagal.
- Total unit test: 102 lulus.
- Final command unit test + `assembleDebug` + tiga androidTest build: `BUILD SUCCESSFUL in 34s`, 234 task.
- Full connected suite Retail pada MuMu Player Android 12: 15/15 lulus.
- Connected smoke Wholesale: 4 test, 3 lulus dan 1 checkout khusus Retail dilewati.
- Connected smoke Culinary: 4 test, 3 lulus dan 1 checkout khusus Retail dilewati.
- `OperationalRepositoryTest`: 7/7 lulus sebagai bagian full connected suite Retail.
- Migrasi versi 1 ke 2 lulus dan menjaga data transaksi/stok lama.
- Backup, penolakan backup rusak, serta restore data lulus pada MuMu.
- AAPT membuktikan tiga APK memakai `versionCode 3`, `versionName 0.2.0-<flavor>`, minimum SDK 23, target SDK 36.
- Manifest ketiga APK tidak meminta permission `android.permission.INTERNET`.
- Packaging final menghasilkan:
  - `Kasir-Retail-UMKM.apk` — SHA-256 `3C688B266EBBF11264EA9B78D70E31691DBDB804D8FF8ED84F0EE54546DD1621`;
  - `Kasir-Grosir-Agen.apk` — SHA-256 `B7029AA583E29EEBB3CE02079C75DE8A2E26A517B234EB0923604D0E87D7C3E7`;
  - `Kasir-Kuliner-PKL.apk` — SHA-256 `133A66DC8E59A9FF88C329F711D9CDCECF35223F8FFC25FD48C729B41B4580E6`.
- Hash file distribusi sama dengan APK hasil build untuk setiap flavor.

### Batas yang tetap berlaku

- Visual final belum dikerjakan sesuai keputusan user.
- APK masih debug dan belum memakai release signing key.
- HP fisik belum diuji.
- HPP/laba, pajak otomatis, cloud, multi-device, BPJS, payroll formal, printer, marketplace, dan payment gateway tidak ditambahkan karena berada di luar scope.

## 2026-07-30 - Patch akses Owner, shortcut flavor, dan ikon 0.2.1

Status: Selesai, diverifikasi, dan dipaketkan sebagai APK debug.

### Keputusan user

- Pekerja tidak boleh membuka operasional.
- Pekerja hanya boleh memakai kasir serta melihat total transaksi aktif dan stok.
- Area pengelolaan hanya boleh dibuka Owner.

### Implementasi

- Mode awal aplikasi diubah menjadi Mode Kasir/Pekerja tanpa navigasi pengelolaan.
- Satu PIN Owner membuka Operasional, Keuangan, Laporan, Lainnya, profil, backup, dan restore.
- Sesi Owner dikunci saat aplikasi masuk background atau lewat `Keluar Mode Owner`.
- Shortcut Owner dibedakan: Retail menonjolkan Produk/Piutang, Grosir membuka alat Grosir, dan Kuliner membuka Pesanan/Kuliner.
- Foreground adaptive icon tiga flavor diganti dengan aset transparan yang berada di safe zone Android.
- `versionCode` dinaikkan dari 3 ke 4 dan `versionName` dari 0.2.0 ke 0.2.1.

### Verifikasi final

- Red test akses gagal sebelum implementasi karena filter akses dan state sesi belum tersedia.
- Targeted unit test akses, sesi Owner, dan presentasi flavor lulus setelah implementasi.
- Unit test `40` per flavor, total `120`, lulus tanpa kegagalan.
- `assembleDebug` dan tiga APK androidTest lulus.
- Connected Retail `15/15` lulus pada MuMu Player Android 12.
- Connected Wholesale `14` lulus dengan `1` checkout Retail-only dilewati.
- Connected Culinary `14` lulus dengan `1` checkout Retail-only dilewati.
- Smoke manual membuktikan Mode Kasir/Pekerja hanya menampilkan kasir, stok produk, dan total transaksi aktif.
- Setelah PIN Owner dibuat, navigasi Operasional, Piutang, Laporan, dan Lainnya muncul.
- Launcher MuMu menampilkan ikon Retail, Grosir, dan Kuliner tanpa logo terpotong.
- Metadata build ketiga flavor memakai `versionCode 4` dan `versionName 0.2.1-<flavor>`.
- Packaging menghasilkan:
  - `Kasir-Retail-UMKM.apk` — SHA-256 `C07EB80C016F311C16192701C89FF82E11CA156EB571303B4A255D18A982A5D5`;
  - `Kasir-Grosir-Agen.apk` — SHA-256 `B5C1A41110C3CCA3460DC50CE3831716F24124E6B29E40D67D395EE7B812B29D`;
  - `Kasir-Kuliner-PKL.apk` — SHA-256 `DBAC358CAD53302EC538A0D4F9188C8E4694FFC7720AC9CE0A1A349A28912259`.

### Dokumentasi

- Root cause dan solusi dicatat sebagai ERR-012 sampai ERR-014 di `docs/ERROR_SOLUTIONS.md`.
- Keputusan akses diperbarui pada `AGENTS.md`, arsitektur, spesifikasi, dan kebutuhan UI/UX.

## 2026-07-30 - Catatan audit eksternal dan rencana desain Stitch

Status: Dicatat, belum diverifikasi, dan belum diimplementasikan.

### Arahan user

- Simpan hasil pembacaan repo dari koneksi GitHub sebagai bahan pemeriksaan lanjutan.
- Tahap berikutnya adalah membuat desain UI di Stitch dengan bantuan Codex.
- Permintaan ini belum menjadi persetujuan untuk mengubah source, memperbaiki bug, atau menerapkan desain.

### Temuan audit yang perlu dibuktikan dari source dan runtime

1. Snapshot nama usaha pada transaksi atau struk diduga masih memakai label flavor statis, bukan nama profil usaha terbaru.
2. Pembayaran awal saat membuat utang/piutang manual diduga belum membuat catatan kas, sedangkan pembayaran lanjutan sudah.
3. Pemeriksaan akses Owner diduga belum konsisten pada seluruh fungsi pengelolaan di lapisan domain/data.
4. Backup masih berupa database dalam ZIP dengan checksum integritas, belum terenkripsi, dan pembacaan ukuran file perlu diperiksa batas amannya.
5. PIN Owner diduga belum mempunyai progressive delay atau pembatasan percobaan gagal.
6. Relasi Room diduga belum memakai foreign key sehingga risiko record yatim perlu diperiksa sebelum fitur hapus/import berkembang.

Temuan di atas berasal dari audit eksternal melalui koneksi GitHub. Temuan tidak boleh dimasukkan ke `docs/ERROR_SOLUTIONS.md` atau disebut sebagai bug terkonfirmasi sebelum reproduksi, pemeriksaan root cause, dan bukti aktual dilakukan.

### Arah desain Stitch

- Mulai dari flow P0 Retail: `Kasir -> Keranjang -> Pembayaran -> Struk`.
- Gunakan behavior aplikasi yang sudah disetujui sebagai batas desain; desain tidak boleh melemahkan validasi pembayaran, akses Owner, histori stok, atau fungsi offline.
- Siapkan state normal, aktif, nonaktif, stok habis, uang kurang, error, kosong, dan sukses.
- Setelah flow Retail disetujui, turunkan sistem visual yang sama ke Grosir dan Kuliner dengan kemampuan serta warna flavor masing-masing.

### Verifikasi aktual

- Isi catatan audit dibaca dan diringkas ke worklog.
- Rencana kerja Stitch ditambahkan ke `docs/UI_UX_REQUIREMENTS.md`.
- Tidak ada source aplikasi yang diubah.
- Test, build, dan smoke test tidak dijalankan karena pekerjaan ini hanya pencatatan requirement dan temuan belum diverifikasi.

## 2026-07-30 - Persetujuan arah visual kasir Retail

Status: Desain visual disetujui, belum diimplementasikan.

### Keputusan user

- Tujuh gambar flow kasir yang dibuat melalui GPT Web sudah sesuai dengan arah visual yang diinginkan.
- Gambar tersebut menjadi acuan visual utama, bukan bahan untuk dibuat ulang dengan arah desain berbeda.

### Hasil pencatatan

- Menyalin tujuh gambar ke `docs/design-references/retail-cashier-approved-2026-07-30/`.
- Menetapkan urutan layar Mode Kasir/Pekerja, daftar produk, pencarian, keranjang, pembayaran cukup, pembayaran kurang, dan transaksi berhasil.
- Membuat `docs/superpowers/specs/2026-07-30-retail-cashier-visual-design.md`.
- Memperbarui checklist desain P0 di `docs/UI_UX_REQUIREMENTS.md`.

### Batas

- Persetujuan ini mengunci arah visual, bukan memberi persetujuan coding.
- Copy sinkronisasi, E-Wallet, `Lainnya`, catatan Retail, dan scan barcode harus mengikuti scope produk yang sudah disetujui tanpa mengubah gaya visual.

### Verifikasi aktual

- Ketujuh file referensi terdeteksi dengan ukuran `841 x 1870 px`.
- Ketujuh file berhasil disalin dan mempunyai hash SHA-256.
- Tidak ada source aplikasi yang diubah.
- Test, build, dan smoke test tidak dijalankan karena pekerjaan ini hanya pencatatan desain.

## 2026-07-30 - Implementasi flow kasir Retail dari Stitch

Status: Implementasi Compose dan verifikasi otomatis berjalan.

### Perubahan

- Menambahkan layar awal `Mode Kasir / Pekerja` dengan jumlah transaksi hari ini, stok menipis, dan stok habis dari data lokal.
- Menambahkan navigasi `CASHIER_HOME -> CATALOG -> CART -> PAYMENT -> RECEIPT`.
- Mengubah katalog menjadi daftar vertikal yang lebih dekat dengan acuan Stitch.
- Menambahkan header kasir dan indikator empat langkah pada katalog, keranjang, pembayaran, dan transaksi selesai.
- Mempertahankan kalkulator offline, varian, satuan, topping Kuliner, QRIS, Transfer, Piutang, validasi uang kurang, berbagi struk, dan PIN Owner.
- Memperbarui smoke test agar masuk melalui tombol `Mulai Transaksi`.

### Verifikasi aktual

- `testRetailDebugUnitTest` lulus memakai JDK 17 lokal.
- Unit test Retail, Wholesale, dan Culinary lulus.
- `assembleDebug` untuk tiga flavor lulus.
- Compile APK androidTest Retail, Wholesale, dan Culinary lulus.
- Connected test belum dijalankan karena belum ada konfirmasi target emulator atau perangkat.
- Visual QA masih berstatus blocked sampai ada screenshot runtime dari emulator/perangkat.

---

## 2026-07-31 - Debugging Terarah Integritas POS Suite Versi 0.3.1

Status: Selesai, diverifikasi murni, dan dipaketkan sebagai APK debug v0.3.1.

### Ringkasan Pekerjaan

Melakukan debugging terarah untuk 8 area integritas data dan kestabilan aplikasi:
1. **Stok Produk Bervarian**: Menambahkan validasi `variantId` wajib pada `adjustStock()` dan `recordPurchase()`.
2. **Inkonsistensi Pembayaran Utang & Kas**: Menambahkan pencatatan `CashEntryEntity` saat pembuat utang awal bayar DP > 0, serta `DebtPaymentEntity` saat `recordPurchase()` membayar DP > 0.
3. **Pembersihan Note & Topping Kuliner**: Menghapus `cart_line_notes` dan `cart_line_toppings` di DAO saat kuantitas item keranjang menjadi 0 (`setQuantity(0)`).
4. **Produk & Varian Nonaktif**: Menambahkan validasi `isActive` pada `addProduct()` dan `completeSale()`.
5. **Proteksi Area Owner**: Menambahkan helper `requireOwner()` pada `ReportSession` dan dipanggil di `createBackup()`, `inspectBackup()`, `confirmRestore()`.
6. **Resilience Checkout**: Membungkus `completeSale()` dalam `try/finally` di `PosViewModel` untuk menjamin `_isSaving = false` selalu dieksekusi saat error/exception.
7. **Keamanan & Validasi Backup**: Menambahkan validasi pembatasan `businessType` agar backup tidak silang flavor, membatasi ukuran ZIP max 256MB database & 1MB manifest.
8. **Pengoptimalan Indeks Query**: Memverifikasi keberadaan indeks Room pada `categoryId`, `productId`, `variantId`, `receiptNumber`, `saleId`.
9. **UI & Accessibility Fix**: Memperbaiki `ProductCard` agar mengoper parameter `onClick = onClick` ke Material3 `Card`, serta menambahkan `.verticalScroll()` pada `CashierLandingScreen`.

### Verifikasi Aktual

- **Unit Tests**: Lulus 100% pada ketiga flavor (Retail, Wholesale, Culinary) — 84/84 unit tests PASSED.
- **Connected UI Tests**: Lulus pada emulator MuMu Player Android 12 (`127.0.0.1:7555`) — 24/24 connected UI tests PASSED (`connectedRetailDebugAndroidTest`).
- **Build & Packaging**: `assembleDebug` sukses. `scripts/package-apks.ps1` memperbarui 3 APK versi `0.3.1` (versionCode 6) di `dist/debug/`:
  - `Kasir-Retail-UMKM.apk`
  - `Kasir-Grosir-Agen.apk`
  - `Kasir-Kuliner-PKL.apk`
- **Dokumentasi**: Memperbarui `docs/ERROR_SOLUTIONS.md` (ERR-016 s/d ERR-023), `docs/RELEASE_NOTES.md` (v0.3.1), dan `app/build.gradle.kts`.

---

## 2026-08-01 - Perbaikan Audit Konsolidasi PR & Pengujian 3 Varian APK (Versi 0.3.2)

Status: Selesai, diverifikasi murni pada 3 varian APK, dan dipaketkan di `dist/debug/`.

### Ringkasan Pekerjaan

Menerapkan perbaikan bug P0 & P1 dari dokumen audit konsolidasi `docs/AUDIT_2026-07-31.md` dan menguji 3 varian APK (Retail, Grosir, Kuliner) di emulator MuMuPlayer:
1. **Integritas Stok Pembelian (CON-001 / ERR-024)**: Mengelompokkan penambahan stok per target `(productId, variantId)` pada `OperationsRepository.recordPurchase()` agar item ganda tidak menimpa stok snapshot lama.
2. **Resilient Owner Session (CON-002 / ERR-025)**: Mereset `externalOwnerFlowDepth = 0` saat `ReportSession.lock()` dipanggil agar sesi Owner di-lock dengan tepat.
3. **Themes Compat API 27 (CON-003 / ERR-026)**: Memindahkan `android:windowLightNavigationBar` ke `values-v27/themes.xml` sehingga lulus Android Lint untuk minSdk 23.
4. **Metadata Version Bump (CON-004)**: Memperbarui `versionCode = 7` dan `versionName = "0.3.2"` di `app/build.gradle.kts`.
5. **Validasi Checkout Varian (CON-005 / ERR-027)**: Memasukkan validasi `variant.productId == product.id` di `PosRepository.kt`.
6. **Active Customers Empty State (CON-006 / ERR-028)**: Menggunakan `activeCustomers` pada `PaymentScreen.kt` agar empty state tampil benar jika semua pelanggan nonaktif.
7. **Stok Varian Layar Owner (CON-007 / ERR-029)**: Menampilkan total stok varian aktif pada kartu produk di `ManagementScreens.kt`.
8. **Pembersihan Workflow Bootstrap (CON-009)**: Menghapus 2 workflow bootstrap sementara (`apply-audit-patch.yml` dan `apply-priority-audit-fixes.yml`).

### Verifikasi Aktual

- **Unit Tests**: Lulus 100% pada 3 flavor (`testRetailDebugUnitTest`, `testWholesaleDebugUnitTest`, `testCulinaryDebugUnitTest`) — 84/84 unit tests PASSED.
- **Android Lint**: Lulus 100% pada 3 flavor (`lintRetailDebug`, `lintWholesaleDebug`, `lintCulinaryDebug`) — BUILD SUCCESSFUL.
- **Build & Packaging**: `assembleDebug` sukses. `scripts/package-apks.ps1` memperbarui 3 APK versi `0.3.2` (versionCode 7) di `dist/debug/`:
  - `Kasir-Retail-UMKM.apk` | SHA256 `CB193D9CF3E28AA60EEA5C072601F439DBF30F037F31696555B3ECDE8D6A2CFF`
  - `Kasir-Grosir-Agen.apk` | SHA256 `02DACF1A5B585C48BBEF475868B0904FC2BEED92DE93F83253A33AE2766516AD`
  - `Kasir-Kuliner-PKL.apk` | SHA256 `79F2DBC48528FB04911FED93D36D289FCC9FF03A2B6788AB1ECA19BE024BDFEF`
- **Pengujian MuMuPlayer**: Ketiga varian APK dipasang, diluncurkan, dan diambil screenshot secara sukses di emulator (`127.0.0.1:7555`).
- **Dokumentasi**: Memperbarui `docs/AUDIT_2026-07-31.md`, `docs/RELEASE_NOTES.md` (v0.3.2), `docs/ERROR_SOLUTIONS.md` (ERR-024 s/d ERR-029), `docs/WORKLOG.md`, dan `walkthrough.md`.

---

## 2026-08-01 - Fondasi forecasting penjualan tervalidasi

Status: Diimplementasikan pada domain shared core (`antigravity/forecasting-foundation-0.3.3`); integrasi Room dan UI belum dilakukan.

### Hasil

- menambahkan moving average, simple exponential smoothing, Holt linear, Holt-Winters additive, dan Croston-SBA;
- memilih kandidat melalui rolling-origin one-step backtesting;
- mencatat MAE, RMSE, sMAPE, WAPE, dan bias;
- menambahkan validasi histori, normalisasi tanggal kosong, serta batas komputasi;
- menambahkan unit test untuk pola stabil, tren, musiman, intermittent, data nol, gap, duplikat, input negatif, dan histori kurang.

### Verifikasi aktual

- `testRetailDebugUnitTest`, `testWholesaleDebugUnitTest`, `testCulinaryDebugUnitTest`: LULUS 100% (8/8 skenario SalesForecastEngineTest + seluruh unit test repo lulus).
- `lintRetailDebug`, `lintWholesaleDebug`, `lintCulinaryDebug`: BUILD SUCCESSFUL.
- `assembleDebug`: BUILD SUCCESSFUL.
- **Pengujian MuMuPlayer**: Ketiga varian APK (`Retail`, `Wholesale`, `Culinary`) dipasang, diluncurkan, dan diambil screenshot secara sukses di emulator (`127.0.0.1:7555`), aplikasi berjalan mulus tanpa crash.

### File

- `app/src/main/java/com/bimacore/usahakecil/domain/forecast/SalesForecastEngine.kt`
- `app/src/test/java/com/bimacore/usahakecil/domain/forecast/SalesForecastEngineTest.kt`
- `docs/superpowers/specs/2026-08-01-sales-forecasting-foundation.md`
- `docs/handoffs/ANTIGRAVITY_FORECASTING_FOUNDATION_2026-08-01.md`

---

## 2026-08-01 - Perbaikan Bug Tampilan Katalog Kasir Terpotong (3 Varian APK)

Status: Selesai, diuji pada 3 varian APK, dipaketkan di `dist/debug/`.

### Hasil

- **Merampingkan Header Kasir & Menyatukan Tombol Kalkulator (ERR-030)**: Memindahkan tombol kalkulator ke dalam header topBar di samping tombol Mode Owner untuk menghemat 48dp tinggi vertikal.
- **Mengoptimalkan Spacing & Layout Weight**: Merampingkan padding langkah transaksi (6dp) dan spacing pencarian/kategori (4dp) serta mengunci `LazyColumn` dengan `Modifier.weight(1f)` agar daftar barang selalu tampil utuh dan bisa di-scroll lancar pada orientasi landscape/HP/Tablet di 3 APK.

### Verifikasi aktual

- `testRetailDebugUnitTest`, `testWholesaleDebugUnitTest`, `testCulinaryDebugUnitTest`: LULUS 100%.
- `lintRetailDebug`, `lintWholesaleDebug`, `lintCulinaryDebug`: BUILD SUCCESSFUL.
- `assembleDebug`: BUILD SUCCESSFUL.
- **Packaging APK**: `scripts/package-apks.ps1` memperbarui 3 APK di `dist/debug/`.
- **Pengujian MuMuPlayer**: Ketiga varian APK (`Retail`, `Wholesale`, `Culinary`) dipasang, diluncurkan, dan diambil screenshot secara sukses di emulator (`127.0.0.1:7555`), tampilan katalog tidak lagi terpotong.

---

## 2026-08-01 - Integrasi forecasting penjualan ke transaksi dan Laporan Owner (Versi 0.4.0)

Status: Diimplementasikan dan diverifikasi lewat test, lint, build, serta kompilasi AndroidTest tiga flavor.

### Perubahan

- Menambah kolom snapshot `sale_items.baseQuantity` melalui migrasi Room 2 ke 3; transaksi lama mengisi nilai awal dari `quantity`.
- Menambah tabel shift melalui migrasi Room 3 ke 4, `shiftId` pada sales dan jurnal kas, serta unique index untuk satu shift aktif.
- Menghubungkan `ReportRepository` ke histori transaksi lokal dan `SalesForecastEngine` dengan kuantitas dasar untuk konversi satuan Grosir.
- Menampilkan perkiraan tujuh hari di Laporan Owner setelah PIN terbuka; data forecast dibersihkan saat Owner mengunci laporan.
- Menambahkan alur Owner Buka Shift/Tutup Shift, snapshot kas seharusnya, uang fisik, selisih, dan riwayat.
- Checkout wajib memiliki shift aktif; transaksi lama tetap memiliki `shiftId = null` dan tidak ditebak masuk shift baru.
- Menambah regresi AndroidTest untuk migrasi, forecast Grosir, dan akses laporan saat terkunci.
- Menaikkan metadata menjadi `versionCode 9` dan `versionName 0.4.0`.

### Verifikasi aktual

- Unit test tiga flavor: `BUILD SUCCESSFUL`.
- Lint tiga flavor: `BUILD SUCCESSFUL`.
- `assembleDebug`: `BUILD SUCCESSFUL`.
- Kompilasi AndroidTest tiga flavor: `BUILD SUCCESSFUL`.
- Backup manifest diselaraskan ke schema database `4`.
- Connected test, visual QA, dan uji offline perangkat: belum dijalankan karena target perangkat belum dikonfirmasi.

### Batas fitur

- Forecasting restock, retur/refund, void, HPP/laba, notifikasi, dan cloud masih belum diimplementasikan.

---

## 2026-08-01 - Sinkronisasi branch dan release identity 0.3.3

Status: Source sudah disinkronkan dengan `origin/main`; APK debug `0.3.3` sudah dibuild dan dipaketkan dari HEAD terbaru.

### Perubahan

- Menggabungkan update `origin/main` ke branch `antigravity/forecasting-foundation-0.3.3` tanpa menulis ulang tiga commit lokal forecasting/UI.
- Menghapus kembali workflow bootstrap audit sementara yang muncul dari merge dan tidak dipakai untuk CI aktif.
- Menyamakan `versionCode`, `versionName`, README, release notes, dan handoff forecasting ke `0.3.3` / `8`.

### Batas status

- Unit test, lint, build, dan compile androidTest seluruh flavor lulus.
- Packaging tiga APK `0.3.3` lulus dan hash dicatat di release notes.
- Connected smoke test, visual QA, dan uji offline perangkat belum dijalankan.

### Verifikasi aktual

- Unit test tiga flavor: `BUILD SUCCESSFUL`.
- Lint tiga flavor: `BUILD SUCCESSFUL`.
- `assembleDebug`: `BUILD SUCCESSFUL`.
- Compile androidTest tiga flavor: `BUILD SUCCESSFUL`.
- `scripts/package-apks.ps1`: sukses memperbarui tiga APK debug.
- Connected test dan visual QA: belum dijalankan karena target emulator/perangkat belum dikonfirmasi.

---

## 2026-08-01 - Owner QA dua device dan standardisasi MuMu

Status: Connected test Owner lulus dan panduan MuMu diperbarui.

### Verifikasi aktual

- `owner_mode_covers_all_relevant_screens_and_locks_again` lulus `6/6`: Retail, Wholesale, Culinary pada `emulator-5554` ASUS portrait dan `emulator-5556` ALT landscape.
- Ditemukan dan diperbaiki layout kontrol Laporan Owner yang bertumpuk pada layar kecil/tablet landscape.
- Dicatat workaround screenshot dan tombol keluar Owner saat snackbar backup aktif di `docs/MUMU_TESTING_GUIDE.md`.
- Screenshot portrait dan tablet diaudit vision: tiga kontrol laporan lulus tanpa wrap, overlap, atau clipping; temuan sisa kontras status bar dicatat sebagai revisi visual terpisah.
- `AGENTS.md`, README, dan `docs/ERROR_SOLUTIONS.md` mengacu ke panduan MuMu sebagai flow standar agent.

---

## 2026-08-01 - Adaptive catalog, laporan chart, dan navigasi compact

Status: Diimplementasikan dan diverifikasi pada dua emulator MuMu serta tiga flavor.

### Perubahan

- Mengubah katalog menjadi grid adaptif: dua kolom pada HP dan tiga kolom pada tablet landscape, dengan kartu produk berbentuk persegi rounded.
- Menghapus indikator empat langkah yang memenuhi layar kasir dan mempertahankan header Mode Kasir/Owner dengan ikon storefront internal.
- Menambahkan chart batang native Compose untuk penerimaan per metode pembayaran beserta empty state saat belum ada transaksi.
- Menjaga sesi Owner tetap terbuka sampai aksi kunci manual dan menerapkan tema aktif pada area Owner serta navigasi bawah.
- Menyesuaikan label `Operasional` agar satu baris pada layar compact tanpa mengubah ukuran label lain.

### Verifikasi aktual

- Unit test, lint, build APK, dan compile AndroidTest tiga flavor: lulus.
- Connected `MainActivitySmokeTest`: Retail, Wholesale, dan Culinary lulus pada `emulator-5554` HP portrait serta `emulator-5556` tablet landscape.
- Screenshot valid diambil memakai `cmd /c adb exec-out screencap -p` dan diaudit vision untuk katalog, Owner, laporan chart, serta navigasi.
- Screenshot QA disimpan di `artifacts/ui-qa/` dan dikecualikan dari Git.

---

## 2026-08-01 - Brand CatatToko pada layar awal

Status: Diimplementasikan; menunggu verifikasi build dan visual dua device.

### Perubahan

- Mengganti ikon Material `Storefront` pada layar awal dengan asset launcher HD dari flavor aktif.
- Menambahkan teks brand `CatatToko` tepat di bawah logo.
- Memastikan asset tetap flavor-aware: Retail jade, Grosir biru, dan Kuliner orange.
- Menambahkan assertion AndroidTest untuk logo dan teks brand.
- APK tetap dipasang setelah patch untuk visual QA dan tidak di-uninstall kecuali diganti patch baru.
- Packaging final: `CatatToko-Retail.apk` (`E2F05D2C4E020CCCEC1DD6759B304F2C4C1010A1B60EDCCD09A1A4CC7EA7F36A`), `CatatToko-Grosir.apk` (`A41F3A37823389409D5C2F0B40F34C8E89D6BCC22A53272046A2CFCAD4CBBE32`), dan `CatatToko-Kuliner.apk` (`7641E6B3AB481B63BC7D28851A586A721056EA1D433EB205AE869993F6710A59`).

---

## 2026-08-01 - Header kasir compact dan buka shift oleh pekerja

Status: Diimplementasikan; verifikasi otomatis dan visual sedang dijalankan.

### Perubahan

- Mengecilkan tinggi header hijau kasir menjadi minimum 52dp.
- Subtitle flavor memakai ukuran 11sp, satu baris, dan ellipsis agar `Retail & UMKM` tidak terpotong secara vertikal atau melebar.
- Menambahkan status shift pada layar kasir dan tombol `Buka Shift` yang dapat dipakai pekerja tanpa PIN Owner.
- Membuat observasi shift aktif tetap tersedia secara offline tanpa membuka ringkasan kas atau riwayat shift.
- `Tutup Shift`, ringkasan kas, dan riwayat shift tetap dilindungi sesi Owner.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/data/OperationalDaos.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/OperationsRepository.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/CashierLandingScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/HomeScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/PosApp.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/OperationalRepositoryTest.kt`

---

## 2026-08-01 - Export Excel offline Owner-only

Status: Diimplementasikan; verifikasi otomatis dan runtime Android lulus.

### Perubahan

- Menambahkan generator workbook OpenXML `.xlsx` tanpa dependency spreadsheet eksternal.
- Menambahkan export snapshot offline dari tabel operasional yang relevan.
- Menambahkan guard sesi Owner pada manager dan ViewModel.
- Menambahkan tombol `Export Excel` dan `Bagikan Excel` di area Owner.
- Menambahkan test format ZIP/XML, escaping karakter, nama sheet, serta test Android untuk guard Owner dan isi data.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/export/ExcelWorkbookExporter.kt`
- `app/src/main/java/com/bimacore/usahakecil/export/ExcelExportManager.kt`
- `app/src/main/java/com/bimacore/usahakecil/PosApplication.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/main/res/xml/file_paths.xml`
- `app/src/test/java/com/bimacore/usahakecil/export/ExcelWorkbookExporterTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/export/ExcelExportTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`

### Verifikasi aktual

- Unit test Retail, Wholesale, dan Culinary: lulus.
- Lint Retail, Wholesale, dan Culinary: lulus.
- `assembleDebug`: lulus.
- Compile AndroidTest Retail, Wholesale, dan Culinary: lulus.
- Connected `ExcelExportTest`: lulus pada `emulator-5554` portrait dan `emulator-5556` landscape.
- Connected `ExcelExportTest` juga lulus pada Wholesale dan Culinary di kedua device.
- Connected UI smoke export sampai `Bagikan Excel`: lulus pada kedua device.
- Percobaan awal landscape menemukan snackbar Owner menutup tombol export; layout dan busy guard diperbaiki lalu dites ulang lulus.

---

## 2026-08-01 - First-run guide, full feature coverage, dan Excel final

Status: Diimplementasikan, diuji lintas flavor, dan dipackage sebagai `0.4.2` / `versionCode 11`.

### Perubahan

- Menambahkan guide wajib first-run yang menjelaskan Mode Kasir/Pekerja, Mode Owner, cara membuka Owner, dan kondisi offline. Guide tidak dapat dilewati dengan back atau klik di luar.
- Menambah regression coverage untuk pembayaran tunai/QRIS/kredit, rekonsiliasi laporan, utang/piutang, tenaga kerja, status order kuliner, serta capability flavor.
- Mengubah Excel menjadi workbook terkurasi dengan `Info Export`, `Ringkasan`, transaksi, stok, kas, utang-piutang, shift, pembelian, dan tenaga kerja.
- Menambahkan auto-width per kolom dari teks terpanjang dengan batas 10–72 karakter.
- Menambahkan test export 500 order dan validasi runtime memakai spreadsheet artifact tool.
- Mengunci regresi bahwa Mode Owner tidak membutuhkan shift aktif untuk membuka Laporan dan Export Excel; shift hanya wajib untuk checkout kasir.
- Menstabilkan smoke checkout agar memilih nominal cepat yang tersedia, bukan mengandalkan nominal hardcode.

### Verifikasi aktual

- Full Gradle matrix: unit test, lint, assemble debug, dan compile AndroidTest tiga flavor lulus (`259 actionable tasks`).
- Full connected smoke: Retail 38/38 per emulator; Wholesale dan Culinary 38/38 per emulator dengan test penjualan tunai Retail-only dilewati.
- Smoke regresi Owner tanpa shift: Retail 7/7 per emulator; Wholesale dan Culinary 7 lulus + 1 test Retail-only dilewati per emulator.
- Domain coverage: 4/4 test pada Retail, Wholesale, dan Culinary di dua emulator.
- Excel coverage: 2/2 test pada Retail, Wholesale, dan Culinary di dua emulator; test kedua mengekspor 500 order.
- Onboarding: test portrait dan landscape lulus; guide wajib terbukti dapat di-scroll dan dikonfirmasi.
- Artifact final: `artifacts/ui-qa/retail-export-result.xlsx`, tervalidasi `Info Export!B4`, `Ringkasan`, dan render width kolom.

### APK final

- Retail: `dist/debug/CatatToko-Retail.apk` | SHA256 `69A74F403049FFF5C62076DACBAE7A6927B625338BE3D588BF2112C13096987B`
- Grosir: `dist/debug/CatatToko-Grosir.apk` | SHA256 `73751F81598C831AADAB747D4900997F6D18F5FF0BCEAFC206FF579A4A6DD6CA`
- Kuliner: `dist/debug/CatatToko-Kuliner.apk` | SHA256 `8E4989C76DC63EAC305BD303B51308C1AB6878682E51F16F093450560121C892`

### Batas rilis

- Export masih seluruh data offline; periode harian/mingguan/bulanan/tahunan, chart Excel, forecasting Excel, dan analisis laba/HPP belum termasuk.
- Fitur agen bank/e-wallet belum ditambahkan dan tetap di luar scope rilis ini.

---

## 2026-08-02 - Popup buka shift dari tombol transaksi

Status: Diimplementasikan, diverifikasi, dan dipackage sebagai `0.4.3` / `versionCode 12`.

### Perubahan

- Menambahkan guard pada tombol `Mulai Transaksi` agar pekerja yang belum membuka shift langsung melihat popup `Buka Shift`.
- Menjaga alur normal: ketika shift aktif, tombol tetap membuka katalog produk/menu.
- Menambahkan regression test untuk kondisi tanpa shift.
- Menaikkan versi ke `0.4.3` / `versionCode 12`.

### Verifikasi aktual

- Full Gradle matrix lulus: unit test, lint, assemble debug, dan compile AndroidTest tiga flavor (`259 actionable tasks`).
- Connected smoke lulus: Retail 8/8 per device; Wholesale dan Culinary 8 lulus + 1 test Retail-only dilewati per device.
- Regression tanpa shift lulus pada emulator portrait dan landscape.
- APK dipackage:
  - Retail SHA256 `BE4D77121CEBE57CF483441DD7D0C0995CCD90FD5ED62E3F96CE5C79F1EB06F5`.
  - Grosir SHA256 `F5331CD6CF7A4D3BB971C6319EB9D219C141AE5759BB71F586C8B5E0D47D0AC3`.
  - Kuliner SHA256 `A1DAADC9DF52E5B1C5EEFA7118B4E3424DBDCBBF1BA455C03BE2FF9F50AE4ECC`.

## 2026-08-02 - Owner kasir tanpa shift dan grid Operasional

Status: Diimplementasikan, diverifikasi, dan dipackage sebagai `0.4.4` / `versionCode 13`.

### Perubahan

- Membedakan aturan checkout Owner dan pekerja di `PosRepository`; Owner tidak lagi wajib membuka shift.
- Menjaga `shiftId = null` untuk transaksi Owner tanpa shift agar rekonsiliasi shift pekerja tidak tercampur.
- Mengurangi dominasi header hijau pada layar Owner.
- Mengganti tab/bar horizontal Operasional dan Keuangan menjadi grid dua kolom.
- Mengganti daftar kategori vertikal menjadi kartu kategori dua kolom dan membatasi teks tile agar tidak bertabrakan.
- Menambahkan regression test domain, UI Owner tanpa shift, dan grid Operasional.

### Bukti verifikasi aktual

- Full Gradle matrix tiga flavor lulus: unit test, lint, debug build, dan AndroidTest APK.
- Connected smoke lulus pada `emulator-5554` dan `emulator-5556`; Retail `42/42`, Grosir/Kuliner `44` run per emulator tanpa failure.
- Screenshot portrait dan landscape untuk Owner, Operasional, Laporan, dan Lainnya sudah diperiksa.
- APK `0.4.4` sudah dipackage dengan SHA256 tercatat di `docs/RELEASE_NOTES.md`.

## 2026-08-02 - Grafik laporan kosong, demo report, dan foto menu

Status: Diimplementasikan, diverifikasi, dan dipackage sebagai `0.4.5` / `versionCode 14`.

### Perubahan

- Grafik penerimaan tidak lagi diganti pesan kosong; empat metode pembayaran selalu dirender dan metode tanpa transaksi bernilai `Rp0`.
- Menambahkan `ReportDemoTest` dengan transaksi, kas, pengeluaran, utang, piutang, dan histori fiktif untuk memeriksa kartu laporan, periode harian/mingguan/bulanan/tahunan, forecast, ranking analisis, dan export Excel.
- Menambahkan tombol pilih/ganti foto menu pada form produk; URI foto disimpan di database lokal dan foto lama dipertahankan saat edit tanpa foto baru.
- Menambahkan regression test untuk chart kosong, demo laporan, dan pemilih foto menu.
- Menaikkan versi ke `0.4.5` / `versionCode 14`.

### Bukti verifikasi aktual

- Target connected `ReportDemoTest` lulus pada `emulator-5554` dan `emulator-5556`.
- Full Gradle matrix tiga flavor lulus: unit test, lint, debug build, dan AndroidTest APK.
- Connected smoke final lulus pada `emulator-5554` dan `emulator-5556`; Retail `45/45`, Grosir/Kuliner `47` run per emulator dengan `2` test Retail-only dilewati.
- Target `ReportDemoTest`, chart kosong, export Excel dengan histori shift kosong, dan pemilih foto menu lulus pada dua emulator.
- APK `0.4.5` sudah dipackage dengan SHA256 tercatat di `docs/RELEASE_NOTES.md`.

## 2026-08-02 - Periode laporan, export Excel di Laporan, dan grafik forecast terbaca

Status: Selesai, diverifikasi lintas flavor, dan dipackage sebagai `0.4.6` / `versionCode 15`.

### Perubahan

- Menambahkan pemilih periode `Hari ini`, `Minggu ini`, `Bulan ini`, dan `Tahun ini` pada Laporan.
- Menggunakan periode aktif untuk ringkasan dan sheet event Excel; master/catalog tetap snapshot saat export.
- Memindahkan `Export Excel` dan `Bagikan Excel` dari Lainnya ke bagian atas Laporan.
- Menambahkan angka setiap hari, satuan, dan skala minimum-maksimum pada grafik forecast.
- Menambahkan test periode workbook dan memperkuat smoke test UI export untuk protected report serta Owner tanpa shift.
- Menaikkan versi ke `0.4.6` / `versionCode 15`.

### Bukti verifikasi aktual

- Unit test tiga flavor, lint tiga flavor, `assembleDebug`, dan AndroidTest APK tiga flavor lulus; `259 actionable tasks`.
- Retail connected lulus `46/46` per emulator.
- Wholesale dan Culinary connected masing-masing `48` run per emulator, dengan dua test Retail-only dilewati dan tanpa failure.
- `ReportDemoTest` dan `ExcelExportTest` periode lulus pada dua emulator.
- APK final:
  - Retail SHA256 `1A71CCE4A56E548E2DC8528666FF31FCCD5C5E8E443EE2AAEEDDD0ED17C19DEC`.
  - Grosir SHA256 `4B36F1DA7C736FDB77BE3E43A0ED5B19F2C46FA83919E09DAEEAC8374DB18EEE`.
  - Kuliner SHA256 `A206F4014F3893FBDD0606385950A57AEB9120B944A0FE6068180F2E8AD426BA`.

### File utama

- `app/src/main/java/com/bimacore/usahakecil/data/ReportPeriod.kt`
- `app/src/main/java/com/bimacore/usahakecil/export/ExcelExportManager.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ForecastScreen.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/export/ExcelExportTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/report/ReportDemoTest.kt`
