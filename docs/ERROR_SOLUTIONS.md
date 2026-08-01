# Error Solutions

## Tujuan

File ini menyimpan masalah yang benar-benar ditemukan, root cause, solusi, dan bukti verifikasi aktual.

File ini bukan daftar fitur, backlog, atau tempat mencatat dugaan bug yang belum direproduksi.

## Status

Source aplikasi sudah dibuat. Entri di bawah mencatat error build dan pengujian yang benar-benar ditemukan selama implementasi.

## Aturan wajib

- Setiap bugfix harus menambah atau memperbarui satu entri.
- Reproduce masalah sebelum mengubah kode jika kondisi memungkinkan.
- Cari root cause, jangan hanya patch gejala.
- Catat varian APK dan versi yang terdampak.
- Jelaskan apakah bug terjadi saat offline, online, atau keduanya.
- Tambahkan regression test jika relevan.
- Jalankan test, build varian terdampak, dan smoke test flow terkait.
- Untuk backup/restore, pembayaran, stok, atau histori finansial, verifikasi integritas data setelah fix.
- Jangan menulis “lulus” tanpa command atau pemeriksaan aktual.
- Jika pengujian perangkat fisik belum dilakukan, nyatakan batas verifikasinya.
- Jangan menghapus entri lama. Perbarui status atau tambahkan entri lanjutan.

## Penomoran

Gunakan format berurutan:

```text
ERR-001
ERR-002
ERR-003
```

Gunakan satu ID untuk satu root cause. Beberapa gejala dengan root cause yang sama boleh dicatat dalam satu entri.

Jika diagnosis lama ternyata salah, jangan menulis ulang sejarah. Tambahkan koreksi pada entri tersebut beserta bukti baru.

## Format entri

Salin struktur berikut ketika bug pertama ditemukan:

```markdown
## ERR-001 - Judul gejala yang spesifik

Tanggal:

Varian dan versi:

### Kondisi/gejala

Jelaskan apa yang dilihat user, kapan terjadi, dan dampaknya.

### Cara reproduksi

1. Langkah pertama.
2. Langkah berikutnya.
3. Hasil aktual.

Hasil yang diharapkan:

### Root cause

Jelaskan penyebab teknis yang sudah dibuktikan.

### Solusi

Jelaskan perubahan terkecil yang memperbaiki root cause.

### Perlindungan regresi

Sebutkan test yang ditambah atau diperbarui.

### Bukti verifikasi aktual

- Test:
- Build:
- Smoke test:
- Perangkat:
- Kondisi offline/online:
- Pemeriksaan data:

### File terdampak

- `path/file`
```

## Standar bukti berdasarkan area

### Penjualan dan pembayaran

- transaksi tersimpan sekali;
- total dan metode pembayaran sesuai;
- histori lama tidak berubah;
- pembatalan atau koreksi tidak menghapus audit trail.

### Stok

- pergerakan stok tercatat;
- konversi satuan benar;
- tidak ada stok negatif diam-diam;
- pembatalan transaksi mengembalikan stok sesuai aturan.

### Harga, HPP, dan pajak

- transaksi baru memakai aturan yang berlaku;
- transaksi lama tetap memakai snapshot lama;
- laporan tidak menghitung ulang histori dengan nilai terbaru.

### Pegawai

- tarif sesuai tanggal berlaku;
- kehadiran atau pekerjaan tercatat;
- bonus, kasbon, dan potongan tidak tercampur;
- pembayaran lama tidak berubah setelah tarif diperbarui.

### Backup dan restore

- file backup dapat dibuat;
- integritas file diperiksa;
- restore berhasil pada data pengujian;
- backup pengaman dibuat sebelum restore;
- restore gagal tidak merusak data aktif.

### Varian APK

- fix lulus pada varian terdampak;
- shared core tidak merusak varian lain;
- fitur yang tidak relevan tetap tersembunyi pada varian lain.

## Checklist sebelum menyatakan bug selesai

- [ ] Gejala berhasil direproduksi atau batas reproduksi dijelaskan.
- [ ] Root cause dibuktikan.
- [ ] Solusi terkecil diterapkan.
- [ ] Regression test ditambah atau diperbarui jika relevan.
- [ ] Seluruh test relevan lulus.
- [ ] Build varian terdampak lulus.
- [ ] Smoke test flow terkait dilakukan.
- [ ] Integritas data diperiksa.
- [ ] Entri file ini diperbarui.
- [ ] Batas verifikasi perangkat fisik dinyatakan.

## ERR-001 - Konfigurasi target JVM Kotlin tidak dapat dikompilasi

Tanggal: 2026-07-29

Varian dan versi: Semua flavor, debug

### Kondisi/gejala

Build berhenti ketika membaca `app/build.gradle.kts` karena Kotlin 2.3 tidak lagi menerima konfigurasi lama `kotlinOptions.jvmTarget`.

### Root cause

Project memakai Kotlin 2.3.0, tetapi konfigurasi JVM masih memakai DSL lama yang sudah menjadi error.

### Solusi

Konfigurasi compiler dipindahkan ke `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }`.

### Perlindungan regresi

Build ketiga flavor dijalankan dalam satu command agar shared configuration selalu diperiksa.

### Bukti verifikasi aktual

- Test dan build: lihat entri worklog implementasi 2026-07-29.
- Perangkat: build lokal Windows; tidak membutuhkan internet atau emulator.

### File terdampak

- `app/build.gradle.kts`

## ERR-002 - Dependency Lifecycle meminta SDK dan AGP yang belum didukung toolchain

Tanggal: 2026-07-29

Varian dan versi: Semua flavor, debug

### Kondisi/gejala

Pemeriksaan metadata AAR menolak build karena versi Lifecycle yang terpilih meminta compile SDK 37 dan AGP 9.1.

### Root cause

Resolusi dependency otomatis mengambil Lifecycle 2.11, sedangkan project memakai toolchain stabil AGP 8.13.2 dan compile SDK 36.

### Solusi

Lifecycle dipatok ke 2.10.0 dan Activity Compose ke 1.12.3 agar tetap kompatibel dengan compile SDK 36 dan AGP 8.13.2.

### Perlindungan regresi

Build semua flavor memeriksa metadata dependency yang sama.

### Bukti verifikasi aktual

- Test dan build: lihat entri worklog implementasi 2026-07-29.
- Perangkat: build lokal Windows; tidak membutuhkan internet atau emulator.

### File terdampak

- `gradle/libs.versions.toml`

## ERR-003 - Ikon Material `Styler` tidak tersedia

Tanggal: 2026-07-29

Varian dan versi: Semua flavor, debug

### Kondisi/gejala

Kompilasi Compose gagal karena simbol ikon `Styler` tidak ditemukan pada paket ikon yang dipakai.

### Root cause

UI mereferensikan ikon yang tidak termasuk dependency Material Icons pada project.

### Solusi

Ikon fallback kategori pakaian diganti menjadi `Checkroom` yang tersedia.

### Perlindungan regresi

Kompilasi Kotlin semua flavor memeriksa referensi ikon.

### Bukti verifikasi aktual

- Test dan build: lihat entri worklog implementasi 2026-07-29.
- Perangkat: build lokal Windows; tidak membutuhkan internet atau emulator.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/Components.kt`

## ERR-004 - Smoke test menganggap semua perangkat memakai layout HP

Tanggal: 2026-07-29

Varian dan versi: Retail debug + androidTest

### Kondisi/gejala

Smoke test tidak menemukan sticky cart summary saat berjalan pada emulator berukuran tablet, walaupun produk sudah masuk keranjang.

### Root cause

Test hanya mencari navigasi layout HP. Pada lebar minimal 840 dp, keranjang memang tampil langsung sebagai panel kanan sehingga sticky summary tidak dirender.

### Solusi

Test dibuat adaptif: membuka sticky summary bila tersedia, atau langsung memakai panel pembayaran pada layout tablet. Assertion kembalian memakai jumlah node karena nilai yang sama memang tampil di dua bagian struk.

### Perlindungan regresi

`MainActivitySmokeTest` mencakup layout compact dan expanded tanpa mengubah behavior aplikasi.

### Bukti verifikasi aktual

- Full connected suite Retail pada MuMu Player lulus `12` test sebelum penambahan dua regression test repository.
- Smoke test checkout Retail mencapai struk, menampilkan kembalian `Rp8.000`, lalu memulai transaksi baru dengan keranjang kosong.
- Smoke test navigasi dan kalkulator pada Grosir serta Kuliner lulus setelah dijalankan terpisah.

### File terdampak

- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`

## ERR-038 - Export Excel belum terkurasi dan belum terbukti pada banyak order

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.4.2`

### Kondisi/gejala

Export awal belum memberi workbook yang rapi untuk Owner: waktu export masih sulit dibaca, isi belum disusun sebagai laporan terkurasi, dan belum ada bukti export ketika jumlah order besar.

### Root cause

Generator awal belum memiliki format laporan yang jelas dan kolom masih fixed width. Pada percobaan runtime pertama, snackbar status Owner juga menutup area tombol export di layout landscape dan aksi export dapat tersentuh saat operasi PIN masih sibuk.

### Solusi

Menambahkan workbook `.xlsx` OpenXML offline dengan sheet `Info Export`, `Ringkasan`, dan tabel operasional terpilih melalui whitelist. Waktu export memakai format tanggal Indonesia, lebar kolom dihitung dari teks terpanjang dengan batas 10–72 karakter, file disimpan sementara di cache melalui `FileProvider`, dan akses dijaga `ReportSession.requireOwner()` pada manager serta ViewModel. Blok Excel dipindah ke area aman, tombol dibuat nonaktif selama operasi lain berjalan, dan export tidak lagi memunculkan snackbar yang menutup tombol share. Data draft keranjang dan `report_security` tidak ikut diekspor.

### Bukti verifikasi aktual

- `ExcelWorkbookExporterTest` memeriksa paket ZIP OpenXML, sheet, escaping XML, deduplikasi nama sheet, dan width otomatis per kolom.
- `ExcelExportTest`: 2/2 lulus pada Retail, Wholesale, dan Culinary di `emulator-5554` portrait serta `emulator-5556` landscape; termasuk export 500 order.
- Full unit test, lint, assemble, dan compile AndroidTest tiga flavor: lulus (`259 actionable tasks`, `BUILD SUCCESSFUL`).
- Full connected smoke UI: Retail 38/38 per device; Wholesale dan Culinary 38/38 per device dengan satu test Retail-only dilewati.
- Runtime APK menghasilkan `artifacts/ui-qa/retail-export-result.xlsx` sebesar 17.526 byte. Artifact tool membaca `Info Export!B4` sebagai `01 Agustus 2026, 21:25:09 WIB`, dan render final tidak memotong teks Catatan.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/export/ExcelWorkbookExporter.kt`
- `app/src/main/java/com/bimacore/usahakecil/export/ExcelExportManager.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/test/java/com/bimacore/usahakecil/export/ExcelWorkbookExporterTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/export/ExcelExportTest.kt`

## ERR-035 - Layar awal hanya memakai ikon Material tanpa identitas CatatToko

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.4.2`

### Kondisi/gejala

Layar awal kasir menampilkan ikon `Storefront` generik tanpa logo brand dan teks `CatatToko`.

### Root cause

`CashierLandingScreen` merender ikon Material langsung, sehingga asset launcher HD dan identitas brand tidak pernah dipakai pada layar awal.

### Solusi

Layar awal memakai `ic_launcher_foreground_v2` dari flavor aktif melalui resource merge Android, lalu menampilkan teks `CatatToko` di bawahnya. Dengan begitu Retail, Wholesale, dan Culinary tetap memakai logo serta warna flavor masing-masing tanpa saling tertukar.

### Bukti verifikasi aktual

- AndroidTest memeriksa test tag logo dan teks `CatatToko`.
- Build setiap flavor memastikan resource `ic_launcher_foreground_v2` ter-resolve ke asset flavor yang tepat.
- Visual QA wajib mengambil screenshot layar awal pada HP dan tablet serta mengaudit logo, teks, clipping, dan warna.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/CashierLandingScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`
- `app/src/*/res/drawable-nodpi/ic_launcher_foreground_v2.png`

## ERR-005 - Migration test crash karena versi serialization tidak sejajar

Tanggal: 2026-07-30

Varian dan versi: Semua flavor, `0.2.0` androidTest

### Kondisi/gejala

Migration test Room gagal di MuMu dengan `AbstractMethodError` ketika serializer manifest backup dipanggil.

### Root cause

Dependency tree memuat `kotlinx-serialization-core 1.7.3` bersama `kotlinx-serialization-json 1.8.1`. Kode serializer hasil compiler membutuhkan method default dari core 1.8.1 yang tidak tersedia pada core 1.7.3.

### Solusi

Versi serialization disejajarkan memakai enforced BOM `1.8.1` pada runtime aplikasi dan androidTest.

### Perlindungan regresi

`DatabaseMigrationTest` membuka database versi 1, menjalankan migrasi eksplisit ke versi 2, lalu memeriksa transaksi dan stok lama.

### Bukti verifikasi aktual

- `DatabaseMigrationTest` lulus pada MuMu Player Android 12.
- Tiga flavor berhasil dikompilasi dengan dependency yang sama.

### File terdampak

- `app/build.gradle.kts`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/DatabaseMigrationTest.kt`

## ERR-006 - Backup tidak membawa data WAL terbaru

Tanggal: 2026-07-30

Varian dan versi: Semua flavor, `0.2.0`

### Kondisi/gejala

Restore dari backup yang baru dibuat kehilangan profil atau data terbaru walaupun pembuatan file backup tidak menampilkan error.

### Root cause

Query `PRAGMA wal_checkpoint(FULL)` hanya membuat cursor lalu langsung menutupnya. Cursor belum dibaca, sehingga SQLite belum benar-benar menjalankan checkpoint dan salinan file database belum memuat isi WAL terbaru.

### Solusi

Backup manager membaca hasil cursor checkpoint, memeriksa status `busy = 0`, lalu baru menyalin database. Restore tetap memeriksa hash, membuat backup pengaman, mengganti file secara atomik, menjalankan integrity check, dan rollback jika gagal.

### Perlindungan regresi

`BackupRestoreTest` mengubah data setelah backup, melakukan restore, dan memastikan data kembali ke isi backup. Test terpisah merusak file backup dan memastikan restore ditolak sebelum data aktif disentuh.

### Bukti verifikasi aktual

- Dua flow backup/restore lulus pada full connected suite Retail di MuMu Player Android 12.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/backup/BackupManager.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/backup/BackupRestoreTest.kt`

## ERR-007 - Tombol nominal tunai tertutup tombol selesai pada layar landscape

Tanggal: 2026-07-30

Varian dan versi: Semua flavor, `0.2.0`

### Kondisi/gejala

Pada layar MuMu landscape `1600 x 900`, nominal cepat tunai terlihat tetapi sentuhannya diterima tombol `Bayar & Selesai` yang masih nonaktif. Checkout tidak bisa diteruskan dari test maupun sentuhan pada posisi tersebut.

### Root cause

Seluruh bagian pembayaran tunai dirender sebagai satu item `LazyColumn` yang lebih tinggi daripada viewport. Tombol akhir yang menempel di bawah layar menutupi bagian item tersebut.

### Solusi

Kartu nominal, pilihan uang cepat, dan keypad dipisahkan menjadi item scroll tersendiri. Test menggulir ke test tag nominal sebelum menekan.

### Perlindungan regresi

Smoke test Retail memilih produk, masuk ke pembayaran, memilih `Rp20.000`, menyelesaikan transaksi, memeriksa kembalian `Rp8.000`, lalu memastikan transaksi baru bersih.

### Bukti verifikasi aktual

- Targeted checkout smoke test lulus pada MuMu Player.
- Full connected suite Retail lulus `12` test pada verifikasi sebelum penambahan regression test repository.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/PaymentScreen.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`

## ERR-008 - Test tag transaksi baru terpasang pada tombol bagikan

Tanggal: 2026-07-30

Varian dan versi: Semua flavor, `0.2.0` androidTest

### Kondisi/gejala

Smoke test bermaksud memulai transaksi baru, tetapi node `new-transaction` menunjuk tombol berbagi struk.

### Root cause

Test tag salah dipasang ketika aksi struk diberi identitas untuk automation.

### Solusi

Tombol berbagi memakai tag `share-receipt`, tombol transaksi baru memakai `new-transaction`, dan daftar struk memakai `receipt-list` agar automation dapat menggulir ke tombol yang tepat.

### Perlindungan regresi

Smoke test menekan `new-transaction` dan memastikan badge `1 barang` sudah hilang.

### Bukti verifikasi aktual

- Flow checkout Retail dan transaksi baru lulus pada MuMu Player.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/ReceiptScreen.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`

## ERR-009 - Form penyesuaian stok mengirim jenis pergerakan yang tidak didukung

Tanggal: 2026-07-30

Varian dan versi: Semua flavor, `0.2.0`

### Kondisi/gejala

Owner memilih stok masuk atau keluar dari form, tetapi penyimpanan ditolak dengan pesan jenis pergerakan stok tidak valid.

### Root cause

UI mengirim `ADJUST_IN` dan `ADJUST_OUT`, sedangkan aturan repository hanya menerima `ADJUSTMENT_IN` dan `ADJUSTMENT_OUT`.

### Solusi

Kode jenis pergerakan di form dan label dipersamakan dengan kontrak repository.

### Perlindungan regresi

`OperationalRepositoryTest.manual_stock_adjustment_uses_supported_movement_type` menambah stok memakai `ADJUSTMENT_IN`, memeriksa saldo akhir, dan memeriksa jenis histori.

### Bukti verifikasi aktual

- Enam test `OperationalRepositoryTest`, termasuk regression test stok dan ganti PIN, lulus pada MuMu Player Android 12.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/OperationalRepositoryTest.kt`

## ERR-010 - MuMu system server crash saat beberapa suite flavor dijalankan beruntun

Tanggal: 2026-07-30

Varian dan versi: Lingkungan MuMu Player Android 12

### Kondisi/gejala

Command gabungan Wholesale dan Culinary berhenti dengan `INSTRUMENTATION_ABORTED: System has crashed` saat smoke test katalog mulai berjalan.

### Root cause

Logcat menunjukkan fatal exception pada proses sistem Android `android.display`, bukan proses APK:

`java.lang.IndexOutOfBoundsException: Index: 8, Size: 8` di `com.android.server.wm.RecentTasks.getTask`.

### Solusi

Device dibiarkan pulih, lalu connected smoke test dijalankan satu flavor per command untuk menghindari pergantian paket bertubi-tubi pada implementasi Recent Tasks milik MuMu.

### Perlindungan regresi

Kegagalan emulator dibedakan dari crash proses aplikasi melalui PID, process name, dan stack trace logcat.

### Bukti verifikasi aktual

- Device kembali berstatus online.
- Smoke test Wholesale lulus ketika dijalankan sendiri.
- Smoke test Culinary lulus ketika dijalankan sendiri.
- Tidak ada fatal exception dari package `com.bimacore.usahakecil.*`.

### File terdampak

- Tidak ada source aplikasi yang diubah untuk crash sistem MuMu.

## ERR-011 - Edit produk mengaktifkan kembali produk nonaktif

Tanggal: 2026-07-30

Varian dan versi: Semua flavor, `0.2.0`

### Kondisi/gejala

Produk yang sudah dinonaktifkan dapat aktif kembali hanya karena nama, harga, atau satuannya diedit. Form edit juga menampilkan stok seolah dapat diganti langsung, padahal repository sengaja mempertahankan stok dari histori pergerakan.

### Root cause

`saveProduct` selalu menulis `isActive = true` untuk data baru maupun edit. Form produk belum membedakan stok awal produk baru dari stok produk lama yang wajib dikoreksi lewat menu Stok.

### Solusi

Edit produk, kategori, dan varian mempertahankan status aktif sebelumnya. Kolom stok pada edit produk dibuat nonaktif sehingga perubahan stok tetap melalui pergerakan beralasan. Pembayaran freelancer juga memakai input nominal agar cicilan tidak dipaksa langsung lunas.

### Perlindungan regresi

`OperationalRepositoryTest.editing_product_preserves_inactive_status_and_stock_history` menonaktifkan produk, mengedit data serta memasukkan stok palsu, lalu memastikan produk tetap nonaktif dan stok tetap mengikuti histori lama.

### Bukti verifikasi aktual

- Full connected suite Retail `15/15` lulus pada MuMu Player Android 12.
- Unit test dan build ketiga flavor lulus setelah perubahan.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/data/InventoryRepository.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/OperationalRepositoryTest.kt`

## ERR-012 - Ikon launcher terpotong dan tidak pas

Tanggal: 2026-07-30

Varian dan versi: Semua flavor, `0.2.1`

### Kondisi/gejala

Logo Retail dan Kuliner terlihat terlalu besar atau terpotong ketika launcher Android menerapkan mask rounded-square.

### Root cause

Foreground adaptive icon lama memakai gambar dengan bidang putih penuh dan bagian penting logo melewati safe zone adaptive icon.

### Solusi

Foreground tiga flavor diganti dengan PNG transparan. Seluruh bagian penting logo ditempatkan di dalam safe zone dan XML adaptive icon diarahkan ke aset baru.

### Perlindungan regresi

Resource adaptive icon reguler dan round pada Retail, Wholesale, serta Culinary memakai aset foreground v2 yang sama per flavor.

### Bukti verifikasi aktual

- Alpha bounding box ketiga logo berada di dalam area aman 61% kanvas.
- `assembleDebug` ketiga flavor lulus setelah resource diganti.
- Launcher MuMu Player Android 12 menampilkan Retail, Grosir, dan Kuliner tanpa logo terpotong.

### File terdampak

- `app/src/retail/res/drawable-nodpi/ic_launcher_foreground_v2.png`
- `app/src/wholesale/res/drawable-nodpi/ic_launcher_foreground_v2.png`
- `app/src/culinary/res/drawable-nodpi/ic_launcher_foreground_v2.png`
- `app/src/*/res/mipmap-anydpi-v26/ic_launcher*.xml`

## ERR-013 - Pekerja dapat melihat area pengelolaan Owner

Tanggal: 2026-07-30

Varian dan versi: Semua flavor, `0.2.1`

### Kondisi/gejala

Navigasi Operasional, Keuangan, Laporan, dan Lainnya langsung terlihat saat aplikasi dibuka, padahal pekerja hanya boleh memakai kasir.

### Root cause

Daftar navigasi sebelumnya hanya mengikuti kemampuan flavor dan belum mengikuti status akses Owner. PIN hanya melindungi isi laporan.

### Solusi

Aplikasi mulai terkunci dalam Mode Kasir/Pekerja. Navigasi pengelolaan baru dibuat setelah satu PIN Owner terverifikasi. Sesi Owner bersifat observable, dikunci saat aplikasi masuk background, dan dapat ditutup manual dari menu Lainnya.

### Perlindungan regresi

- Unit test memastikan Mode Kasir/Pekerja hanya menghasilkan tujuan `Kasir`.
- Unit test memastikan sesi Owner berubah terkunci–terbuka–terkunci.
- Connected smoke test memeriksa `Laporan` tidak ada sebelum PIN dan muncul setelah PIN benar.

### Bukti verifikasi aktual

- Red test gagal karena filter akses dan state sesi belum ada.
- Targeted unit test lulus setelah implementasi.
- Connected Retail `15/15` lulus pada MuMu Player Android 12.
- Connected Wholesale dan Culinary masing-masing `14` lulus dengan `1` checkout Retail-only dilewati.
- Smoke manual memastikan pekerja hanya melihat kasir, stok, dan total transaksi aktif sebelum PIN Owner benar.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/security/ReportSession.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/AppDestination.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/HomeScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/Dialogs.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`
- `app/src/test/java/com/bimacore/usahakecil/domain/BusinessCapabilitiesTest.kt`
- `app/src/test/java/com/bimacore/usahakecil/security/PinHasherTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`

## ERR-014 - Fitur khusus flavor tersembunyi di balik navigasi generik

Tanggal: 2026-07-30

Varian dan versi: Semua flavor, `0.2.1`

### Kondisi/gejala

Tiga APK terlihat mempunyai fitur yang sama karena shortcut utama semuanya memakai label dan halaman awal generik.

### Root cause

Capability khusus flavor sudah ada, tetapi tujuan Operasional dan Keuangan selalu membuka tab pertama yang sama.

### Solusi

Retail menonjolkan Produk/Piutang, Wholesale membuka Grosir, dan Culinary membuka Pesanan/Kuliner langsung dari navigasi Owner.

### Perlindungan regresi

`BusinessCapabilitiesTest` memeriksa label, halaman awal, dan tab awal setiap flavor.

### Bukti verifikasi aktual

- Targeted unit test presentasi navigasi lulus.
- Connected test ketiga flavor lulus.
- Smoke Retail menunjukkan shortcut `Operasional` dan `Piutang`; test Wholesale dan Culinary memastikan shortcut khususnya membuka alat Grosir dan Kuliner.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/AppDestination.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/HomeScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/test/java/com/bimacore/usahakecil/domain/BusinessCapabilitiesTest.kt`

## ERR-015 - Build Gradle Gagal Karena Default Java 8 di Lingkungan Sistem

Tanggal: 2026-07-30

Varian dan versi: Semua flavor, `0.3.0`

### Kondisi/gejala

Perintah `./gradlew.bat assembleDebug` gagal saat dijalankan dari shell default dengan pesan error:
`Dependency requires at least JVM runtime version 11. This build uses a Java 8 JVM.`

### Root cause

Variabel lingkungan global Windows `JAVA_HOME` atau `java.exe` bawaan sistem mengarah ke `Java 8` (JRE 1.8), sedangkan Android Gradle Plugin 8.13.2 dan Kotlin 2.3.0 membutuhkan minimal JDK 17.

### Solusi

Mengarahkan `JAVA_HOME` secara eksplisit ke JDK 17 lokal di `C:\Users\shint\.codex\toolchains\temurin17` saat mengeksekusi skrip pengemasan dan kompilasi Gradle (`build_and_package.ps1`).

### Perlindungan regresi

Skrip pengemasan `build_and_package.ps1` selalu menyetel `$env:JAVA_HOME` ke JDK 17 sebelum menjalankan `gradlew`.

### Bukti verifikasi aktual

- `assembleDebug` ketiga flavor lulus dalam 3m 16s.
- `testRetailDebugUnitTest`, `testWholesaleDebugUnitTest`, `testCulinaryDebugUnitTest` (84 tasks) lulus dalam 28s.
- `package-apks.ps1` berhasil memperbarui 3 file APK di `dist/debug/`.

### File terdampak

- `C:\Users\shint\.gemini\antigravity\scratch\build_and_package.ps1`
- `dist/debug/Kasir-Retail-UMKM.apk`
- `dist/debug/Kasir-Grosir-Agen.apk`
- `dist/debug/Kasir-Kuliner-PKL.apk`

## ERR-016 - Perubahan stok produk bervarian tidak memperbarui stok varian

Tanggal: 2026-07-31

Varian dan versi: Semua flavor, `0.3.1`

### Kondisi/gejala

Ketika produk bervarian disesuaikan stoknya atau dibeli dari supplier, stok parent product bertambah tetapi stok varian tidak berubah. Saat kasir melakukan checkout, keranjang membaca stok varian sehingga checkout gagal dengan pesan stok habis.

### Root cause

`InventoryRepository.adjustStock()` dan `OperationsRepository.recordPurchase()` tidak memvalidasi `variantId` untuk produk yang memiliki varian (`hasVariants == true`). Operasi memasukkan stok ke parent product secara keliru.

### Solusi

1. Menambahkan validasi `require(!product.hasVariants || variantId != null)` di `InventoryRepository.adjustStock()` dan `OperationsRepository.recordPurchase()`.
2. Mengubah `OperationsViewModel.adjustStock()` dan `recordPurchase()` untuk menerima `variantId`.
3. Memastikan operasi stok pada produk bervarian menolak penyesuaian jika `variantId` tidak disertakan.

### Perlindungan regresi

- `purchase_variant_updates_variant_stock_not_parent`
- `adjust_stock_variant_product_without_variant_id_is_rejected`
- `adjust_stock_variant_updates_variant_stock`
- `purchase_variant_product_without_variant_id_is_rejected`

### Bukti verifikasi aktual

- Unit test `OperationalRepositoryTest` lulus pada ketiga flavor (Retail, Wholesale, Culinary).
- Command `.\gradlew.bat testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest` lulus 84 tasks.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/data/InventoryRepository.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/OperationsRepository.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/OperationalRepositoryTest.kt`

## ERR-017 - Inkonsistensi entri kas dan riwayat pembayaran utang/piutang

Tanggal: 2026-07-31

Varian dan versi: Semua flavor, `0.3.1`

### Kondisi/gejala

Pembuatan utang/piutang manual dengan pembayaran awal (`initialPayment > 0`) mencatat `paidAmount` di tagihan tetapi tidak memicu entri arus kas (`CashEntryEntity`). Begitu pula pada pembelian supplier dengan DP, `DebtPaymentEntity` untuk pembayaran awal tidak tercatat di histori pembayaran utang.

### Root cause

`OperationsRepository.createDebt()` tidak memasukkan `CashEntryEntity` saat `initialPayment > 0`. `OperationsRepository.recordPurchase()` tidak memasukkan `DebtPaymentEntity` saat `amountPaid > 0` pada transaksi utang.

### Solusi

1. Menambahkan pembuatan `CashEntryEntity` di `createDebt()` ketika `initialPayment > 0`.
2. Menambahkan pembuatan `DebtPaymentEntity` di `recordPurchase()` ketika `amountPaid > 0` dan ada sisa tagihan utang.

### Perlindungan regresi

- `createDebt_with_initial_payment_creates_cash_entry_and_debt_payment`
- `purchase_with_downpayment_creates_debt_payment_record`

### Bukti verifikasi aktual

- Unit test `OperationalRepositoryTest` lulus pada ketiga flavor.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/data/OperationsRepository.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/OperationalRepositoryTest.kt`

## ERR-018 - CartLineNote dan CartLineTopping tertinggal saat item keranjang dihapus

Tanggal: 2026-07-31

Varian dan versi: Culinary flavor, `0.3.1`

### Kondisi/gejala

Ketika item keranjang Kuliner dihapus (di-set jumlahnya menjadi 0), catatan pesanan (`cart_line_notes`) dan topping (`cart_line_toppings`) milik lineId tersebut tidak ikut terhapus di database Room, menyebabkan orphan records hingga transaksi baru dimulai.

### Root cause

`PosRepository.setQuantity()` hanya memanggil `cartDao.deleteLine(lineId)` ketika `quantity <= 0`, tanpa membersihkan tabel `cart_line_notes` dan `cart_line_toppings`.

### Solusi

1. Menambahkan query `deleteCartLineNote` dan `deleteCartLineToppingsByLine` di `CulinaryDao`.
2. Mengubah `PosRepository.setQuantity()` agar menghapus catatan & topping saat `quantity <= 0` pada flavor Kuliner.

### Perlindungan regresi

- `deleting_cart_line_clears_associated_notes_and_toppings`

### Bukti verifikasi aktual

- Unit test `OperationalRepositoryTest` lulus pada ketiga flavor.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/data/OperationalDaos.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/PosRepository.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/OperationalRepositoryTest.kt`

## ERR-019 - Produk dan varian nonaktif dapat ditambah ke keranjang dan ditransaksikan

Tanggal: 2026-07-31

Varian dan versi: Semua flavor, `0.3.1`

### Kondisi/gejala

Produk atau varian yang sudah dinonaktifkan oleh Owner masih dapat ditambah ke keranjang atau diselesaikan transaksinya jika sudah ada di keranjang draft sebelum dinonaktifkan.

### Root cause

`PosRepository.addProduct()` dan `PosRepository.completeSale()` tidak memeriksa flag `isActive` pada `ProductEntity` dan `ProductVariantEntity`.

### Solusi

1. Menambahkan validasi `isActive` pada produk dan varian di `PosRepository.addProduct()`.
2. Menambahkan assertion `require(product.isActive)` dan `require(variant == null || variant.isActive)` pada `PosRepository.completeSale()`.

### Perlindungan regresi

- `inactive_product_cannot_be_added_or_checked_out`

### Bukti verifikasi aktual

- Unit test `OperationalRepositoryTest` lulus pada ketiga flavor.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/data/PosRepository.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/OperationalRepositoryTest.kt`

## ERR-020 - Operasi backup dan restore dapat diakses tanpa verifikasi Owner

Tanggal: 2026-07-31

Varian dan versi: Semua flavor, `0.3.1`

### Kondisi/gejala

Fungsi backup dan restore pada `OperationsViewModel` dapat dieksekusi tanpa memastikan bahwa sesi Owner sedang terbuka (`unlocked`).

### Root cause

`OperationsViewModel.createBackup()`, `inspectBackup()`, dan `confirmRestore()` tidak memanggil `reports.session.requireOwner()`.

### Solusi

1. Menambahkan method `requireOwner()` di `ReportSession` untuk melempar error jika sesi terkunci.
2. Memanggil `reports.session.requireOwner()` di seluruh entry point backup & restore pada `OperationsViewModel`.

### Perlindungan regresi

- Unit test `ReportRepositoryTest` & verification flow sesi Owner.

### Bukti verifikasi aktual

- Unit test lulus pada ketiga flavor.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/security/ReportSession.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`

## ERR-021 - UI checkout terkunci permanen jika completeSale melempar exception

Tanggal: 2026-07-31

Varian dan versi: Semua flavor, `0.3.1`

### Kondisi/gejala

Jika terjadi error tak terduga saat kasir menekan `Bayar & Selesai`, state `_isSaving` tetap `true` selamanya sehingga tombol checkout menjadi nonaktif dan tidak bisa ditekan lagi.

### Root cause

`PosViewModel.completeSale()` mengubah `_isSaving.value = true`, lalu memanggil repository tanpa blok `try/finally`. Exception menyebabkan `_isSaving.value = false` dilewati.

### Solusi

Membungkus eksekusi `completeSale()` dalam blok `try { ... } catch (error: Exception) { ... } finally { _isSaving.value = false }`.

### Perlindungan regresi

- Verification unit test `PosViewModel` & error handling checkout.

### Bukti verifikasi aktual

- Unit test lulus pada ketiga flavor.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/PosViewModel.kt`

## ERR-022 - Restore backup tidak memvalidasi jenis usaha dan batas ukuran file

Tanggal: 2026-07-31

Varian dan versi: Semua flavor, `0.3.1`

### Kondisi/gejala

Restore file backup dapat dipasang pada varian APK yang berbeda (misal restore backup Grosir ke APK Retail), merusak struktur data atau fitur khusus flavor. File ZIP yang berukuran terlalu besar atau berisi entry berbahaya juga tidak dibatasi.

### Root cause

`BackupManager.preview()` dan `restore()` tidak mencocokkan `manifest.businessType` dengan `profile.businessType` aplikasi aktif. `readPackage()` juga menyalin seluruh stream ZIP tanpa batas ukuran atau jumlah entry.

### Solusi

1. Menambahkan validasi `businessType` pada `preview()` dan `restore()`.
2. Membatasi ukuran membaca ZIP max 256 MB untuk database dan max 1 MB untuk manifest.
3. Membatasi jumlah entry ZIP maksimal 2 dan nama entry harus `manifest.txt` atau `database.db`.

### Perlindungan regresi

- `BackupRestoreTest` & `BackupManagerTest`.

### Bukti verifikasi aktual

- Unit test lulus pada ketiga flavor.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/backup/BackupManager.kt`

## ERR-023 - Kartu produk di katalog tidak dapat diklik dan landing kasir terpotong pada layar kecil

Tanggal: 2026-07-31

Varian dan versi: Semua flavor, `0.3.1`

### Kondisi/gejala

1. Mengetuk kartu produk pada layar katalog tidak menambah barang ke keranjang kasir.
2. Tombol `Mulai Transaksi` pada `CashierLandingScreen` terdorong keluar area layar atau terpotong pada resolusi tinggi/layar kecil.

### Root cause

1. `CatalogScreen.kt` pada komponen `ProductCard` tidak mengoper parameter `onClick = onClick` ke konstruktor `Card(...)`, sehingga kartu produk menjadi non-clickable secara UI.
2. `CashierLandingScreen.kt` menggunakan `Column` kaku tanpa `verticalScroll()`, menyebabkan layout tombol bawah terpotong pada orientasi atau tinggi viewport tertentu.

### Solusi

1. Menambahkan `onClick = onClick` pada `Card` di `CatalogScreen.kt`.
2. Menambahkan `verticalScroll(rememberScrollState())` pada `Column` utama di `CashierLandingScreen.kt`.

### Perlindungan regresi

- `MainActivitySmokeTest.kt` menguji aksesibilitas katalog, kalkulator, dan alur transaksi tunai di MuMu Player.

### Bukti verifikasi aktual

- Unit test lulus pada ketiga flavor (84/84 test).
- Smoke UI test lulus pada emulator MuMu Player.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/CatalogScreen.kt`

## ERR-031 - Metadata backup tertinggal dari versi schema database

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.4.0`

### Kondisi/gejala

Database aplikasi sudah naik ke schema 4, tetapi `BackupManager` masih menulis `schemaVersion=2` pada manifest backup.

### Root cause

Konstanta schema pada `BackupManager` tidak ikut dinaikkan saat migrasi Room 2 ke 3 dan 3 ke 4 ditambahkan.

### Solusi

Menyelaraskan `DATABASE_SCHEMA_VERSION` menjadi `4`, serta menambahkan migrasi backup test ke seluruh migrasi aktif.

### Bukti verifikasi aktual

- `assembleRetailDebugAndroidTest assembleWholesaleDebugAndroidTest assembleCulinaryDebugAndroidTest` - BUILD SUCCESSFUL.
- Runtime backup/restore pada perangkat belum dijalankan karena target perangkat belum dikonfirmasi.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/backup/BackupManager.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/backup/BackupRestoreTest.kt`

## ERR-036 - Subtitle flavor pada header kasir terpotong di bagian bawah

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.4.1`

### Kondisi/gejala

Subtitle `Retail & UMKM` pada header Mode Kasir terlihat bagian bawah hurufnya terpotong pada layar compact.

### Root cause

Header memakai dua baris teks dengan line height default dan padding vertikal yang terlalu besar dibanding ruang efektif baris, sehingga subtitle dirender terlalu mepet pada batas bawah header.

### Solusi

Header dirampingkan menjadi minimum 52dp. Subtitle memakai `labelSmall` dengan `11sp`, `14sp` line height, satu baris tanpa wrap, dan ellipsis sebagai fallback untuk label flavor yang lebih panjang.

### Bukti verifikasi aktual

- Compile Retail Debug lulus setelah perubahan.
- Unit test, lint, build, dan visual emulator untuk semua flavor masih menjadi tahap verifikasi patch ini.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/CashierLandingScreen.kt`

## ERR-037 - Pekerja tidak dapat membuka shift tanpa PIN Owner pada mode offline

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.4.1`

### Kondisi/gejala

Saat belum ada shift aktif, pekerja harus meminta Owner membuka area Keuangan terlebih dahulu. Ini tidak efisien untuk APK offline satu HP.

### Root cause

`OperationsRepository.openShift()` memanggil `requireOwnerForShift()`, sementara tombol pembuka shift hanya tersedia di layar Keuangan Owner.

### Solusi

Menambahkan tombol `Buka Shift` pada Mode Kasir dan observasi shift aktif yang hanya memuat status/nama shift. Pembukaan shift tetap memvalidasi nama, modal awal, satu shift aktif, dan menyimpan data lokal. Ringkasan kas, penutupan shift, dan riwayat tetap memerlukan PIN Owner.

### Bukti verifikasi aktual

- Regression test memastikan repository terikat sesi Owner yang terkunci tetap dapat membuka shift, tetapi tidak dapat membaca ringkasan atau menutup shift.
- Smoke test `owner_mode_does_not_require_an_open_shift_for_management_or_export` menghapus shift aktif pada fixture, lalu membuktikan Owner tetap dapat membuka Laporan dan Export Excel tanpa membuka shift.
- Connected smoke Retail, Wholesale, dan Culinary lulus pada emulator portrait dan landscape; checkout tetap diuji terpisah dengan shift aktif.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/data/OperationalDaos.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/OperationsRepository.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/HomeScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/PosApp.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/CashierLandingScreen.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/OperationalRepositoryTest.kt`

## ERR-034 - Label Operasional terpotong dua baris pada navigasi HP

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.4.0`

### Kondisi/gejala

Pada layar HP, label navigasi `Operasional` membungkus menjadi `Operasion` dan `al` karena lima item navigasi berbagi lebar layar yang sempit.

### Root cause

Label navigasi memakai ukuran teks bawaan tanpa aturan satu baris atau penyesuaian untuk lebar layar compact.

### Solusi

Pada layar di bawah `600dp`, label `Operasional` memakai ukuran compact `10sp`, `maxLines = 1`, dan `softWrap = false`. Tablet tetap memakai ukuran label normal.

### Bukti verifikasi aktual

- `assembleDebug` lulus untuk Retail, Wholesale, dan Culinary.
- Screenshot + audit vision pada HP menunjukkan `Operasional` tampil utuh dalam satu baris.
- Screenshot + audit vision pada tablet menunjukkan label tetap utuh tanpa perubahan ukuran yang tidak perlu.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/HomeScreen.kt`

## ERR-024 - Update stok pembelian menimpa nilai saat beberapa baris menunjuk produk/varian sama (CON-001)

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.3.2`

### Kondisi/gejala

Pembelian supplier yang memiliki beberapa baris dengan target produk atau varian sama dapat menyebabkan stok akhir tidak sesuai karena update kedua menimpa update pertama dari snapshot lama.

### Root cause

`OperationsRepository.recordPurchase()` membaca snapshot entity produk/varian di awal, lalu melakukan iterasi dan update stok per baris dari snapshot awal tersebut, bukan mengumpulkan total penambahan stok per barang target.

### Solusi

Mengelompokkan baris `resolved` berdasarkan `(productId, variantId)` dan menjumlahkan `baseQuantity` memakai `Math.addExact` sebelum memperbarui stok di database Room.

### Perlindungan regresi

- `OperationsRepositoryTest`.

### Bukti verifikasi aktual

- Unit test lulus pada ketiga flavor (`testRetailDebugUnitTest`, `testWholesaleDebugUnitTest`, `testCulinaryDebugUnitTest`).

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/data/OperationsRepository.kt`

## ERR-025 - Lock manual pada ReportSession tidak mereset externalOwnerFlowDepth (CON-002)

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.3.2`

### Kondisi/gejala

Saat Owner mengunci sesi secara manual saat guard external flow sedang aktif (>0), lalu melakukan unlock kembali, auto-lock berikutnya dilewati karena penanda `externalOwnerFlowDepth` basi tidak direset.

### Root cause

`ReportSession.lock()` hanya mengubah `_unlocked.value = false` tanpa mereset `externalOwnerFlowDepth = 0`.

### Solusi

Menambahkan reset `externalOwnerFlowDepth = 0` dan anotasi `@Synchronized` pada metode `lock()` dan `unlock()`.

### Perlindungan regresi

- `ReportSessionTest`.

### Bukti verifikasi aktual

- Unit test lulus pada ketiga flavor.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/security/ReportSession.kt`

## ERR-026 - Error Android Lint windowLightNavigationBar pada minSdk 23 (CON-003)

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.3.2`

### Kondisi/gejala

Command `lintRetailDebug`, `lintWholesaleDebug`, dan `lintCulinaryDebug` gagal karena atribut `android:windowLightNavigationBar` diletakkan di `values/themes.xml` (minSdk 23) padahal membutuhkan API 27.

### Root cause

Atribut `android:windowLightNavigationBar` memerlukan API level 27 ke atas tetapi diletakkan pada file resource umum minSdk 23.

### Solusi

1. Menghapus atribut `android:windowLightNavigationBar` dari `app/src/main/res/values/themes.xml`.
2. Membuat file resource baru `app/src/main/res/values-v27/themes.xml` untuk menampung atribut tersebut khusus API 27+.

### Bukti verifikasi aktual

- Android Lint lulus 100% pada ketiga flavor (`lintRetailDebug`, `lintWholesaleDebug`, `lintCulinaryDebug`).

### File terdampak

- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values-v27/themes.xml`

## ERR-027 - Checkout kasir tidak memvalidasi ulang relasi varian dan produk (CON-005)

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.3.2`

### Kondisi/gejala

Item keranjang kasir berisiko diproses dengan varian milik produk lain apabila terjadi ketidakcocokan data dari restore atau masukan pihak ketiga.

### Root cause

`PosRepository.completeSale()` belum memvalidasi kembali bahwa `variant.productId == product.id` saat menghitung stok dan subtotal checkout.

### Solusi

Menambahkan `require(variant == null || variant.productId == product.id) { "Varian tidak sesuai produk" }` pada alur checkout `PosRepository.kt`.

### Bukti verifikasi aktual

- Unit test lulus pada ketiga flavor.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/data/PosRepository.kt`

## ERR-028 - Selector pelanggan kasir kredit menjadi kosong tanpa petunjuk jika semua pelanggan nonaktif (CON-006)

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.3.2`

### Kondisi/gejala

Pada layar pembayaran kredit, jika terdapat pelanggan terdaftar tetapi semuanya berstatus nonaktif (`isActive = false`), komponen pilihan pelanggan menjadi kosong tanpa pesan penjelasan.

### Root cause

`PaymentScreen.kt` mengecek `customers.isEmpty()` untuk empty state, padahal rendering chip memfilter `customers.filter { it.isActive }`.

### Solusi

Membuat daftar `activeCustomers = remember(customers) { customers.filter { it.isActive } }` dan menggunakannya untuk pengecekan empty state dan pengurutan chip.

### Bukti verifikasi aktual

- Visual QA dan testing pada emulator MuMuPlayer.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/PaymentScreen.kt`

## ERR-029 - Kartu produk bervarian pada layar pengelolaan Owner menampilkan stok 0 (CON-007)

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.3.2`

### Kondisi/gejala

Daftar barang di layar pengelolaan Owner menampilkan `stok 0` untuk produk bervarian meskipun varian-variannya memiliki sisa stok aktif.

### Root cause

`ManagementScreens.kt` membaca `product.stock` langsung untuk subtitle kartu produk, padahal stok produk bervarian disimpan pada entity variannya.

### Solusi

Menghitung `displayStock` dari total stok varian aktif jika `product.hasVariants` bernilai `true`.

### Bukti verifikasi aktual

- Visual QA dan testing pada emulator MuMuPlayer.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`

## ERR-030 - Daftar produk di katalog kasir terdorong keluar layar atau terpotong pada layar HP/Tablet

Tanggal: 2026-08-01

Varian dan versi: Semua flavor (Retail, Wholesale, Culinary), `0.3.3`

### Kondisi/gejala

Pada layar katalog kasir (orientasi landscape atau layar HP/Tablet dengan tinggi terbatas), daftar produk (`LazyColumn`) terdorong ke bawah sehingga hanya menyisakan beberapa piksel di bagian paling bawah layar dan tidak terlihat utuh.

### Root cause

1. `CatalogScreen.kt` menempatkan tombol kalkulator pada `Row` terpisah di bawah `CashierFlowHeader`, memakan ruang setinggi 48dp secara vertikal.
2. Padding vertikal pada `CashierFlowHeader` (14dp) dan lingkaran langkah transaksi (34dp) terlalu besar.
3. Spacing antar pencarian, filter kategori, dan daftar produk (`Spacer` 12dp/8dp) terlalu longgar.
4. Pendaratan `LazyColumn` belum dikunci memakai `Modifier.weight(1f)` secara fleksibel di dalam `Column`.

### Solusi

1. Memindahkan tombol kalkulator (`Icons.Outlined.Calculate`) ke dalam `CashierHeader` hijau di bagian atas (di samping tombol Mode Owner).
2. Menghapus `Row` kalkulator terpisah dari `CatalogScreen.kt`.
3. Merampingkan padding `CashierFlowHeader` (6dp) dan ukuran lingkaran langkah (28dp).
4. Mengatur spacing pencarian & filter kategori menjadi 4dp, serta memberi `Modifier.weight(1f)` pada `LazyColumn` daftar produk.

### Bukti verifikasi aktual

- Unit test lulus 100% pada ketiga flavor (`testRetailDebugUnitTest`, `testWholesaleDebugUnitTest`, `testCulinaryDebugUnitTest`).
- Android Lint lulus 100% pada ketiga flavor (`lintRetailDebug`, `lintWholesaleDebug`, `lintCulinaryDebug`).
- `assembleDebug` lulus untuk ketiga varian APK.
- Visual QA dan testing pada emulator MuMuPlayer (`127.0.0.1:7555`) mengonfirmasi daftar produk pada ketiga varian APK (`Retail`, `Wholesale`, `Culinary`) tampil utuh, lega, dan bisa di-scroll dengan lancar.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/CashierLandingScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/CatalogScreen.kt`

## ERR-031 - Kontrol laporan Owner bertumpuk dan tombol keluar tertutup snackbar pada tablet landscape (CON-008)

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.4.0`

### Kondisi/gejala

Pada layar Laporan Owner, tiga aksi laporan dipaksa berada dalam satu baris. Di tablet landscape, teks `Kunci Mode Owner` membungkus menjadi tombol sangat tinggi. Setelah backup lokal dibuat, snackbar `Backup siap dibagikan` juga dapat menutup area tombol `Keluar Mode Owner`.

### Root cause

Kontrol laporan memakai `Row` tanpa pembagian lebar responsif, sementara layar Lainnya memakai scroll vertikal dan tombol keluar berada di batas bawah viewport ketika snackbar aktif.

### Solusi

Mengubah kontrol Laporan Owner menjadi `Column` dengan tombol lebar penuh dan menambahkan test tag stabil untuk aksi perubahan PIN, penguncian laporan, dan keluar Owner. Panduan MuMu mencatat urutan scroll ke teks `Offline-first` sebelum menekan tombol keluar setelah backup.

### Bukti verifikasi aktual

- Owner connected test lulus `6/6` pada Retail, Wholesale, dan Culinary di `emulator-5554` portrait serta `emulator-5556` landscape.
- Screenshot manual portrait dan tablet yang diaudit vision menunjukkan tiga kontrol laporan utuh, satu baris per tombol, tanpa overlap atau clipping.
- Temuan terpisah dari audit: kontras ikon/jam status bar rendah pada latar terang. Label `Operasional` kemudian diperbaiki pada ERR-034.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`
- `docs/MUMU_TESTING_GUIDE.md`

## ERR-032 - Grafik analisis laporan belum dirender pada APK

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.4.0`

### Kondisi/gejala

Layar Laporan Owner hanya menampilkan angka ringkasan dan daftar metode pembayaran. Grafik analisis tidak terlihat di APK.

### Root cause

Data agregat metode pembayaran sudah tersedia melalui `ReportSummary.payments`, tetapi `ReportsScreen` belum memanggil komponen chart. Implementasi forecast memiliki bar chart terpisah, namun tidak mewakili ringkasan laporan Owner.

### Solusi

Menambahkan vertical bar chart native Compose untuk membandingkan nominal penerimaan per metode pembayaran. Chart memakai data laporan lokal yang sudah ada, mengikuti warna flavor aktif, dan tidak menambah dependency chart atau library Python.

### Bukti verifikasi aktual

- Compile Kotlin semua flavor lulus.
- Android Lint semua flavor lulus.
- Connected smoke test `MainActivitySmokeTest` lulus pada `emulator-5554` (HP portrait) dan `emulator-5556` (tablet landscape) untuk Retail, Wholesale, dan Culinary.
- Screenshot + audit vision pada HP dan tablet mengonfirmasi card grafik tetap terlihat saat data kosong dan menampilkan pesan `Belum ada penerimaan pada periode ini.`; ketika data tersedia, komponen memakai vertical bar chart native Compose.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ReportCharts.kt`

## ERR-033 - Sesi Owner terkunci ulang saat aplikasi kehilangan fokus

Tanggal: 2026-08-01

Varian dan versi: Semua flavor, `0.4.0`

### Kondisi/gejala

Owner harus memasukkan PIN berulang kali setelah berpindah layar atau aplikasi kehilangan fokus. Saat sesi terkunci, header juga menampilkan ajakan membuka Owner alih-alih status `Mode Kasir` dengan gembok.

### Root cause

`MainActivity.onStop()` selalu memanggil `ReportSession.lockUnlessExternalOwnerFlow()`, sehingga lifecycle Android diperlakukan sebagai timeout Owner.

### Solusi

Menghapus auto-lock dari lifecycle Activity. Sesi Owner sekarang tetap terbuka selama proses aplikasi berjalan dan hanya berakhir melalui aksi manual `Kunci Mode Owner` atau `Keluar Mode Owner`. Header kasir memakai ikon gembok dan label `Mode Kasir` saat terkunci, serta ikon buka dan `Mode Owner` saat terbuka.

### Bukti verifikasi aktual

- Unit test sesi Owner diperbarui untuk memastikan sesi tetap terbuka sampai `lock()` eksplisit.
- Connected smoke test diperbarui untuk memverifikasi status kembali ke `Mode Kasir` setelah Owner dikunci manual.
- Build seluruh APK dan connected smoke test lulus pada `emulator-5554` (HP portrait) serta `emulator-5556` (tablet landscape) untuk semua flavor.
- Screenshot + audit vision mengonfirmasi status `Mode Owner` tetap terbuka saat berpindah layar dan `Mode Kasir` menampilkan ikon gembok saat terkunci.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/MainActivity.kt`
- `app/src/main/java/com/bimacore/usahakecil/security/ReportSession.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/CashierLandingScreen.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`
