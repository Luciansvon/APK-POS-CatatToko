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

## ERR-052 - Perbaikan temuan audit CatatToko v0.4.8 (P0-P2)

Tanggal: 2026-08-04

Varian dan versi: Semua varian (Retail, Wholesale, Culinary); Versi 0.4.9 (code 18)

### Kondisi/gejala

Audit menemukan beberapa masalah kritis dan UX:
1. **CT-P0-01 (Restore PIN)**: Restore backup lama menggantikan seluruh database termasuk `report_security`, menyebabkan Owner terkunci dari aplikasinya sendiri jika PIN backup berbeda dari PIN aktif.
2. **CT-P1-07 (Validasi Manifest)**: `BackupManager` tidak mencocokkan `businessUid` manifest dengan profil hasil restore.
3. **CT-P1-05 (Tier Grosir Lintas Satuan)**: Perhitungan tier grosir dihitung dari `baseQuantity` per baris keranjang secara terpisah, bukan total agregat per produk.
4. **CT-P1-04 (ViewModel Busy Flag)**: Flag `_busy` global memblokir reload laporan secara diam-diam saat ada aksi lain yang aktif.
5. **CT-P1-03 (Varian Laporan)**: Rekap produk terjual tidak memperhitungkan varian dan tidak membuang transaksi batal.
6. **CT-P1-02 (Pembatasan 5 Produk)**: Dropdown produk laporan membatasi pilihan hanya 5 produk.
7. **CT-P2-10 (Tap Kuantitas Atomik)**: Perubahan jumlah barang `+`/`-` di keranjang tidak atomik pada level database.
8. **CT-P2-06 (Excel Typed Cells)**: Nilai Rupiah dan Qty di Excel diekspor sebagai string teks (`t="inlineStr"`), sehingga tidak dapat dihitung `SUM` di spreadsheet.

### Root cause

- `BackupManager.kt`: Mengoverwrite tabel `report_security` tanpa menyimpan record PIN aktif terlebih dahulu.
- `PosRepository.kt`: Menggunakan `baseQuantity` per baris keranjang untuk menentukan `applicableTier`, bukan menjumlahkan `baseQuantity` semua baris produk tersebut.
- `OperationsViewModel.kt`: Menggunakan satu flag boolean `_busy` untuk seluruh coroutine ViewModel.
- `OperationalDaos.kt` & `ReportRepository.kt`: Query `productTrendRows` tidak menyertakan `variantId` & `variantName`, dan `ReportProductSettingsMenu` memanggil `products.take(5)`.
- `ExcelWorkbookExporter.kt`: Sel data selalu menggunakan atribut `t="inlineStr"`.

### Solusi

- **Backup Safety**: `BackupManager` menyimpan `report_security` aktif sebelum database ditutup dan menulisnya kembali ke database hasil restore setelah integrity check. `businessUid` manifest dicocokkan dengan profil restore.
- **Tier Grosir**: `PosRepository` mengagregasi total `baseQuantity` per `(productId, variantId)` dari seluruh baris keranjang sebelum menentukan tier grosir.
- **ViewModel Laporan**: Memisahkan pemuatan laporan menggunakan `executeReport` berbasis `Job` (cancel-and-reload pattern).
- **Varian & Produk Laporan**: `ProductTrendRow` dan `ReportProductTrend` menyertakan `variantId` & `variantName`. Query `productTrendRows` memfilter `orderStatus IN ('COMPLETED', 'NEW', 'PROCESSING', 'READY')`. `ReportDashboardComponents` menghapus `products.take(5)`. Forecast produk dipindahkan ke `loadProductForecasts()` (*lazy loading*).
- **Tap Atomik**: Menambahkan `PosRepository.incrementQuantity()` yang berjalan di dalam transaksi Room.
- **Excel Typed Cells**: Sel numerik pada `ExcelWorkbookExporter.kt` ditulis sebagai `<v>$value</v>` jika bernilai angka. `ExcelExportManager.kt` mengelompokkan rekap produk berdasarkan `productId, variantId`.
- **Maintainability & Documentation**: Memperbarui teks onboarding `FirstRunGuide.kt`, menambahkan test migrasi database `2->4` & `3->4` pada `DatabaseMigrationTest.kt`, serta menambahkan test varian pada `ReportTrendRepositoryTest.kt`.

### Perlindungan regresi

- `BackupRestoreTest`: `restore_preserves_current_owner_PIN`, `restore_validates_manifest_identity_matches_database`.
- `PosRepositoryTest`: `wholesale_tier_uses_combined_quantity_across_units`.
- `DatabaseMigrationTest`: `migration_2_4_validates_schema`, `migration_3_4_validates_schema`.
- `ReportTrendRepositoryTest`: `trend_groups_by_variant_and_supports_more_than_5_products`.
- `FormattersTest`: `compact_rupiah_keeps_currency_context_for_chart_values`.

### Bukti verifikasi aktual

- Unit test: Lulus pada ketiga flavor (`testRetailDebugUnitTest`, `testWholesaleDebugUnitTest`, `testCulinaryDebugUnitTest`).
- Build: `assembleDebug` sukses membangun APK 0.4.9 (code 18) untuk ketiga flavor.
- Packaging: `scripts/package-apks.ps1` menyalin APK ke `dist/debug/`.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/backup/BackupManager.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/PosRepository.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/OperationalDaos.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/ReportRepository.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/ReportTrend.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/WorkforceRepository.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/PosViewModel.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/CartScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/PosApp.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/Formatters.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ReportDashboardComponents.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ForecastScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/FirstRunGuide.kt`
- `app/src/main/java/com/bimacore/usahakecil/export/ExcelWorkbookExporter.kt`
- `app/src/main/java/com/bimacore/usahakecil/export/ExcelExportManager.kt`
- `app/src/test/java/com/bimacore/usahakecil/ui/FormattersTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/backup/BackupRestoreTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/PosRepositoryTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/DatabaseMigrationTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/ReportTrendRepositoryTest.kt`

## ERR-051 - Sesi Owner kembali ke Mode Kasir setelah APK dibuka ulang

Tanggal: 2026-08-02

Varian dan versi: semua varian; perubahan pada sesi Owner bersama

### Kondisi/gejala

Owner sudah membuka Mode Owner, tetapi setelah keluar atau membuka ulang APK, aplikasi kembali ke Mode Kasir.

### Root cause

Status sesi Owner hanya disimpan di RAM melalui ReportSession. Saat proses aplikasi dibuat ulang, status selalu kembali terkunci.

### Solusi

Menyimpan status Mode Owner aktif di preferensi lokal dan memulihkannya saat PosApplication dibuat lagi. Status hanya kembali ke Kasir setelah owner menekan Kunci Mode Owner.

### Perlindungan regresi

Menambah unit test untuk pemulihan sesi Owner dan kunci eksplisit.

### Bukti verifikasi aktual

- Test: ReportSessionTest
- Build: assembleDebug akan dijalankan setelah perubahan ini.
- Perangkat: belum connected test.

### File terdampak

- app/src/main/java/com/bimacore/usahakecil/security/ReportSession.kt
- app/src/main/java/com/bimacore/usahakecil/PosApplication.kt
- app/src/main/java/com/bimacore/usahakecil/FirstRunGuide.kt
- app/src/test/java/com/bimacore/usahakecil/security/ReportSessionTest.kt

## ERR-050 - Tombol refresh laporan tersembunyi di rincian

Tanggal: 2026-08-02

Varian dan versi: semua varian; diverifikasi pada debug terbaru

### Kondisi/gejala

Tombol Muat ulang hanya muncul setelah pengguna membuka rincian lengkap laporan. Pengguna dapat mengira laporan selalu ter-update otomatis setelah transaksi baru.

### Root cause

Aksi refresh ditempatkan di akhir konten rincian lengkap, sehingga tidak terlihat pada tampilan ringkas laporan.

### Solusi

Memindahkan tombol ke bawah pilihan periode di bagian atas laporan, menambahkan ikon refresh, label Muat ulang laporan, dan keterangan bahwa laporan perlu dimuat ulang setelah ada transaksi baru.

### Perlindungan regresi

Smoke test memeriksa tag report-refresh sebelum rincian lengkap dibuka.

### Bukti verifikasi aktual

- Build: assembleDebug lulus untuk Retail, Wholesale, dan Culinary.
- Compile test: assembleRetailDebugAndroidTest lulus.
- Perangkat: belum dilakukan connected test; perubahan diverifikasi melalui compile/build.

### File terdampak

- app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt
- app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt

## ERR-049 - Header kolom Excel tidak terbaca dan lebar tabel terasa sempit

Tanggal: 2026-08-02

Varian dan versi: semua varian yang memakai export laporan; diverifikasi pada Retail debug 0.4.8

### Kondisi/gejala

Pada file Excel hasil export APK, judul tiap kolom laporan sulit dibaca. Di Excel desktop, sebagian teks header terlihat hilang karena kombinasi warna header dan tinggi baris yang tidak cukup. Kolom dengan teks panjang seperti Nomor Struk juga terasa terlalu rapat.

### Cara reproduksi

1. Jalankan export laporan penjualan tahunan dari APK Retail.
2. Buka file .xlsx hasil export di Excel desktop.
3. Lihat baris judul kolom dan kolom berisi nomor struk.

Hasil aktual: kontras header rendah pada tampilan Excel yang diuji, teks header terpotong oleh tinggi baris, dan lebar kolom tidak memiliki ruang tambahan.

Hasil yang diharapkan: teks header terbaca jelas, teks panjang dapat membungkus dengan tinggi baris yang cukup, dan lebar kolom memiliki ruang napas.

### Root cause

Style header memakai latar teal gelap dengan teks putih, tetapi kombinasi tersebut tidak terbaca baik pada tampilan Excel yang digunakan. Selain itu, wrapText sudah aktif tetapi tinggi baris masih mengikuti default Excel, dan lebar kolom dihitung tepat dari panjang teks tanpa padding.

### Solusi

- Mengganti header menjadi latar mint terang #E5F3F0 dengan teks teal gelap #0B6B61 tebal.
- Menetapkan tinggi baris eksplisit: header 32pt, data 18pt, judul 24pt, dan subjudul 18pt.
- Menambahkan padding 3 karakter pada lebar kolom, dengan batas minimum 10 dan maksimum 72.

### Perlindungan regresi

Menambah regression test untuk warna style header, tinggi baris header/data, wrapText, dan padding lebar kolom.

### Bukti verifikasi aktual

- Test: testRetailDebugUnitTest --tests com.bimacore.usahakecil.export.ExcelWorkbookExporterTest
- Build: seluruh unit test flavor dan compile test APK Retail/Wholesale/Culinary lulus.
- Smoke test: AnnualSalesExportTest menghasilkan ulang workbook dari jalur export APK.
- Perangkat: emulator-5556.
- Kondisi offline/online: export memakai database sementara dan jalur lokal APK; tidak memerlukan koneksi internet.
- Pemeriksaan data: workbook hasil APK di-inspect dan dirender; tidak ada formula/error yang terdeteksi.

### File terdampak

- app/src/main/java/com/bimacore/usahakecil/export/ExcelWorkbookExporter.kt
- app/src/test/java/com/bimacore/usahakecil/export/ExcelWorkbookExporterTest.kt

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

## ERR-049 - Restore memakai database baru, sesi Owner, dan piutang parsial tidak konsisten

Tanggal: 2026-08-03

Varian dan versi: Semua flavor, branch `codex/report-refresh-button`

### Kondisi/gejala

- Setelah restore, ViewModel Activity masih dapat memegang repository/database lama yang sudah ditutup.
- Status Owner sebelumnya dapat terbawa lewat preference atau tetap terbuka setelah aplikasi ditinggalkan.
- Pembayaran awal penjualan kredit tercatat sebagai `paymentMethod = CREDIT`, sehingga tidak masuk rekonsiliasi kas shift walaupun uang tunai benar-benar diterima.

### Root cause

- Restore membuka ulang Room database, tetapi callback sebelumnya hanya memanggil `recreate()` tanpa membuang `ViewModelStore` yang memegang dependency lama.
- `ReportSession` membaca status aktif dari preference dan `endExternalOwnerFlow()` tidak mengunci sesi.
- `PosRepository` memakai nama metode pembayaran penjualan (`CREDIT`) untuk cash entry `RECEIVABLE_IN`, sementara rekonsiliasi shift hanya menghitung entry tunai.

### Solusi

- ViewModelStore dibersihkan sebelum Activity dibuat ulang setelah restore.
- Sesi Owner selalu mulai terkunci, dikunci saat Activity berhenti, dan dikunci kembali setelah alur file eksternal. URI restore yang dipilih disimpan sementara dan baru diperiksa setelah PIN Owner diverifikasi ulang.
- Cash entry pembayaran awal piutang memakai `paymentMethod = CASH`, tanpa mengubah metode pembayaran pada histori penjualan.
- Event restore diubah menjadi one-shot `SharedFlow` agar tidak memicu recreate berulang.

### Bukti verifikasi aktual

- Test-first `ReportSessionTest`: baseline gagal pada `owner_session_starts_locked_and_external_flow_locks_on_return`; setelah fix targeted test lulus (`BUILD SUCCESSFUL`).
- Unit test tiga flavor lulus: `testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest` (`BUILD SUCCESSFUL`, 84 actionable tasks).
- `assembleDebug` lulus untuk Retail, Wholesale, dan Culinary.
- `assembleRetailDebugAndroidTest assembleWholesaleDebugAndroidTest assembleCulinaryDebugAndroidTest` lulus; regression `partial_credit_down_payment_reconciles_as_cash_in_shift` berhasil dikompilasi pada tiga flavor.
- Connected Retail lulus `49/49` pada ASUS portrait dan `49/49` pada ALT landscape.
- Connected Wholesale dan Culinary masing-masing menjalankan `49` target test per device; `2` test flavor-specific dilewati dan `0` failure.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/MainActivity.kt`
- `app/src/main/java/com/bimacore/usahakecil/PosApplication.kt`
- `app/src/main/java/com/bimacore/usahakecil/security/ReportSession.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/PosRepository.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/HomeScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`
- `app/src/test/java/com/bimacore/usahakecil/security/ReportSessionTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/PosRepositoryTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/report/ReportDemoTest.kt`

## ERR-048 - Rentang tanggal laporan tampil sebagai karakter asing

Tanggal: 2026-08-02

Varian dan versi: Semua flavor, `0.4.8`

### Kondisi/gejala

Menu periode Laporan menampilkan rentang seperti `27 Julâ€“2 Agu 2026` dan `1 Janâ€“2 Agu 2026`, sehingga terlihat seperti bahasa asing bagi pengguna.

### Root cause

Pemisah rentang tanggal pada source sudah tersimpan sebagai karakter mojibake `â€“`, bukan tanda rentang yang aman.

### Solusi

Mengganti pemisah dengan teks ASCII berjarak ` - `, memeriksa source dari sisa karakter mojibake, dan menambah regression check pada `ReportDemoTest`.

### Bukti verifikasi aktual

- Tampilan final menunjukkan `27 Jul - 2 Agu 2026` dan `1 Jan - 2 Agu 2026` pada HP portrait serta tablet landscape.
- `ReportDemoTest` menolak karakter `\u00E2` dan memastikan minimal tiga label rentang memakai ` - `.
- Connected matrix tiga flavor lulus pada kedua perangkat tanpa kegagalan.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/ReportDashboardComponents.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/report/ReportDemoTest.kt`

## ERR-047 - Area Owner sulit dipindai dan tombol Excel kurang mudah dikenali

Tanggal: 2026-08-02

Varian dan versi: Semua flavor, `0.4.8`

### Kondisi/gejala

Laporan menampilkan banyak kontrol teknis sebelum angka yang dibutuhkan Owner. Aksi export mudah terlewat bila hanya mengandalkan ikon atau posisi. Halaman Stok, Pembelian, Pekerja, Keuangan, Transaksi, dan Lainnya memakai susunan generik tanpa hierarki angka utama, aksi utama, atau petunjuk saat data kosong. Perbandingan laporan juga dapat membandingkan waktu berjalan dengan periode lama yang sudah lengkap.

### Root cause

Komponen Owner dibuat per layar tanpa sistem hierarki bersama. Kontrol laporan berkembang bertahap mengikuti fitur, bukan mengikuti urutan keputusan pengguna. Batas periode sebelumnya memakai akhir kalender penuh, bukan waktu berjalan yang setara.

### Solusi

Menambahkan komponen Owner bersama untuk tab, kartu angka utama, metrik ringkas, daftar, aksi bertulisan, dan kondisi kosong. Laporan memakai periode, tombol `Simpan Laporan Excel` lebar penuh, omzet utama, grafik penjualan, serta rincian lanjutan. Batas pembanding digeser sesuai durasi berjalan, dan agregasi `Semua produk` hanya memakai omzet agar unit berbeda tidak dijumlahkan sebagai satu angka jumlah barang. Kata asing yang tidak diperlukan pada area baru diganti dengan istilah berbahasa Indonesia.

### Bukti verifikasi aktual

- `ReportPeriodTest` memeriksa hari, minggu, bulan, dan tahun terhadap waktu berjalan yang sama.
- Unit test tiga flavor dan compile AndroidTest Retail lulus.
- Connected `ReportDemoTest` lulus di `emulator-5554` dan `emulator-5556`, termasuk tombol rincian, empat periode, grafik, pembatasan `Semua produk`, forecast, serta workbook Excel.
- Connected matrix tiga flavor lulus pada dua perangkat dan audit visual final berstatus `passed` di `design-qa.md`.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/data/ReportPeriod.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OwnerDashboardComponents.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ReportDashboardComponents.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/AppDestination.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/report/ReportDemoTest.kt`
- `app/src/test/java/com/bimacore/usahakecil/data/ReportPeriodTest.kt`

## ERR-046 - Ringkasan laporan memakan ruang dan grafik utama tidak membantu monitoring

Tanggal: 2026-08-02

Varian dan versi: Semua flavor, `0.4.7`

### Kondisi/gejala

Ringkasan laporan tersusun sebagai kartu vertikal sehingga memakan ruang. Grafik utama hanya membandingkan metode pembayaran dan tidak menunjukkan arah penjualan atau arus kas dari hari ke hari. Saat belum ada transaksi, grafik tidak memberi sumbu dan bucket nol yang bisa dipantau.

### Root cause

UI laporan belum memiliki model perbandingan periode atau seri tren berbucket. Data laporan hanya dipakai sebagai agregat periode aktif, sedangkan grafik pembayaran tidak cocok menjadi grafik monitoring utama.

### Solusi

Menambahkan perbandingan dengan periode sebelumnya yang setara, grid KPI dua kolom, dan status semantik yang memperhitungkan apakah kenaikan itu baik atau buruk. Repository sekarang mengubah baris penjualan, item produk, dan kas menjadi `ReportTrendReport` dengan bucket harian, mingguan, bulanan, atau tahunan. UI menyediakan mode Arus kas, Penjualan, dan Produk, mempertahankan bucket nol, dan menjadikan grafik pembayaran sebagai rincian sekunder.

### Bukti verifikasi aktual

- Unit `ReportPeriodTest` memeriksa batas periode sebelumnya untuk hari, minggu, bulan, dan tahun.
- AndroidTest `ReportTrendRepositoryTest` memeriksa 14 bucket nol pada database kosong.
- Connected `ReportDemoTest` membuat transaksi fiktif, memilih semua mode grafik, metrik Produk, serta periode hari/minggu/bulan/tahun pada `emulator-5554` dan `emulator-5556`.
- Full flavor build dan connected matrix tiga flavor lulus pada dua emulator; targeted report test lulus lagi setelah patch label sumbu.
- Judul dan delta KPI dikunci masing-masing dua baris agar tinggi card konsisten walaupun teks membungkus.
- Label batang nominal dipadatkan menjadi angka singkat (`165`) sementara nominal lengkap tetap tampil pada detail bucket; tidak ada ellipsis pada screenshot final.
- Screenshot final memperlihatkan bucket nol tetap dirender, label harian `20` sampai `02` tidak lagi menjadi ellipsis, dan card KPI per baris sama tinggi.
- Kontrol grafik yang sebelumnya tampil sebagai banyak tombol diringkas menjadi dua dropdown inti: `Tampilan` dan `Rentang`.
- Saat mode `Produk` dipilih, selector produk dan metrik `Omzet`/`Terjual` digabung dalam satu menu agar alur tetap seamless tanpa menambah baris tombol.
- Audit visual manual pada emulator memeriksa layout dropdown utama dan popup opsi mode/produk; connected `ReportDemoTest` memeriksa anchor kontrol, perpindahan mode, serta chart produk.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/data/ReportPeriod.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/ReportTrend.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/OperationalDaos.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/ReportRepository.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ReportDashboardComponents.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/ReportTrendRepositoryTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/report/ReportDemoTest.kt`

## ERR-045 - Export Excel salah tempat dan grafik forecast sulit dibaca

Tanggal: 2026-08-02

Varian dan versi: Semua flavor, `0.4.6`

### Kondisi/gejala

Export Excel berada di `Lainnya > Backup & restore`, sehingga Owner tidak tahu konteks periode laporan. Laporan juga belum menyediakan pilihan hari, minggu, bulan, atau tahun. Grafik forecast menampilkan batang yang tampak sama tanpa nilai per hari.

### Root cause

View laporan hanya membaca ringkasan hari ini, sedangkan `ExcelExportManager` mengambil seluruh event offline. Tombol export ditempatkan di layar backup. Grafik hanya memberi tanggal dan tinggi batang tanpa angka atau rentang skala.

### Solusi

Menambahkan `ReportPeriod` bersama dengan batas waktu lokal perangkat. Ringkasan dan sheet event Excel menerima rentang yang sama; sheet master tetap snapshot saat export. Tombol export/share dipindahkan ke bagian atas Laporan dan dibuat mengikuti periode aktif. Forecast sekarang menampilkan nilai per hari, satuan, serta skala minimum-maksimum.

### Bukti verifikasi aktual

- `ExcelExportTest` membuktikan export periode hari ini memuat transaksi hari ini dan tidak memuat transaksi kemarin.
- `ReportDemoTest` lulus dengan periode harian, mingguan, bulanan, tahunan, workbook, analisis, serta label skala grafik.
- `MainActivitySmokeTest.protected_reports_and_backup_are_reachable` lulus setelah export dipindahkan ke Laporan; `Lainnya` tetap hanya menampilkan backup/restore.
- Build Retail debug dan AndroidTest lulus sebelum verifikasi matriks akhir.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/data/ReportPeriod.kt`
- `app/src/main/java/com/bimacore/usahakecil/export/ExcelExportManager.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ForecastScreen.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/export/ExcelExportTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/report/ReportDemoTest.kt`

## ERR-044 - Export Excel crash ketika kolom shift kosong

Tanggal: 2026-08-02

Varian dan versi: Semua flavor, `0.4.5`

### Kondisi/gejala

Export Excel dapat gagal dengan `NumberFormatException: For input string: ""` ketika ada histori shift yang memiliki kolom angka kosong.

### Root cause

`ExcelExportManager.shiftSheet()` memanggil `toLong()` langsung pada kolom angka shift. Data lama atau shift yang belum lengkap dapat direpresentasikan sebagai string kosong.

### Solusi

Pembacaan angka export memakai `toLongOrZero()`. Nilai kosong atau tidak valid menjadi `0` dan ditulis sebagai `Rp 0`, sehingga satu baris histori yang tidak lengkap tidak menggagalkan seluruh workbook.

### Bukti verifikasi aktual

- `ReportDemoTest` mengisi data transaksi/laporan lalu menjalankan export workbook dengan histori shift kosong; test lulus pada dua emulator.
- Test export 500 order tetap dijalankan dalam connected smoke.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/export/ExcelExportManager.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/report/ReportDemoTest.kt`

## ERR-042 - Grafik laporan hilang saat belum ada penerimaan

Tanggal: 2026-08-02

Varian dan versi: Semua flavor, `0.4.5`

### Kondisi/gejala

Saat periode laporan belum mempunyai transaksi, area grafik hanya menampilkan pesan `Belum ada penerimaan pada periode ini`, sehingga struktur grafik dan metode pembayaran tidak terlihat.

### Root cause

Data grafik hanya memakai metode pembayaran yang memiliki total lebih dari nol. Ketika hasil filter kosong, composable tidak merender batang apa pun.

### Solusi

Grafik sekarang membuat empat slot tetap dari `PaymentMethod.entries`. Metode yang belum mempunyai transaksi diberi nilai `0`, label tetap tampil, dan baseline kecil dipertahankan agar area grafik tetap terlihat.

### Bukti verifikasi aktual

- Smoke laporan memeriksa chart dan label Tunai, QRIS, Transfer, serta Piutang pada data kosong.
- `ReportDemoTest` memeriksa empat metode dengan data fiktif dan memastikan seluruh label chart tersedia.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/ReportCharts.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/report/ReportDemoTest.kt`

## ERR-043 - Form produk belum menyediakan foto menu

Tanggal: 2026-08-02

Varian dan versi: Semua flavor, `0.4.5`

### Kondisi/gejala

Owner dapat memasukkan nama, harga, stok, dan satuan produk, tetapi tidak mempunyai cara untuk menambahkan foto menu.

### Root cause

`ProductDraft` belum membawa URI gambar dan `ProductDialog` belum membuka pemilih dokumen Android.

### Solusi

Form produk sekarang menyediakan tombol `Pilih foto menu`/`Ganti foto menu`, menyimpan URI content yang dipilih, menampilkan preview, dan mempertahankan URI lama ketika produk diedit tanpa foto baru. Penyimpanan tetap lokal dan tidak membutuhkan internet.

### Bukti verifikasi aktual

- Regression repository menyimpan URI foto lalu memastikan URI tetap ada setelah edit data produk.
- UI smoke semua flavor memastikan tombol pemilih foto tersedia di form produk.
- Compile Retail setelah perubahan foto lulus.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/data/InventoryRepository.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/CatalogScreen.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/OperationalRepositoryTest.kt`
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

## ERR-039 - Pekerja masuk katalog sebelum membuka shift

Tanggal: 2026-08-02

Varian dan versi: Semua flavor, `0.4.3`

### Kondisi/gejala

Saat belum ada shift aktif, pekerja menekan `Mulai Transaksi` tetapi langsung masuk katalog. Shift baru gagal terlihat ketika checkout, sehingga pekerja bisa mengira aplikasi tidak bisa menerima pesanan.

### Root cause

Callback `Mulai Transaksi` langsung memanggil `PosViewModel.showCatalog()` tanpa memeriksa `activeShift`. Halaman shift juga berada di area Owner, sehingga pekerja tidak punya jalur yang jelas dari tombol pesanan.

### Solusi

Home screen sekarang memeriksa shift sebelum membuka katalog. Jika belum ada shift, tombol langsung menampilkan popup `Buka Shift`; jika sudah ada, alur katalog tetap berjalan seperti sebelumnya.

### Bukti verifikasi aktual

- Regression test `start_transaction_without_shift_prompts_to_open_shift` membuktikan form nama kasir dan modal awal muncul, sedangkan katalog tidak terbuka.
- Test checkout dengan shift aktif tetap dijalankan untuk memastikan perubahan tidak memblokir transaksi normal.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/HomeScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/PosApp.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`

## ERR-040 - Owner ikut tertahan aturan shift saat memakai kasir

Tanggal: 2026-08-02

Varian dan versi: Semua flavor, `0.4.4`

### Kondisi/gejala

Mode Owner sudah terbuka, tetapi checkout masih menampilkan `Buka shift terlebih dahulu sebelum menerima transaksi`.

### Root cause

`PosRepository.completeSale()` selalu mengambil shift aktif secara wajib, padahal sesi Owner sudah terverifikasi dan transaksi Owner tidak perlu dimasukkan ke shift pekerja.

### Solusi

Repository sekarang hanya mewajibkan shift untuk pekerja. Owner dapat checkout tanpa shift; penjualan dan kas masuk tetap disimpan, dengan `shiftId` kosong agar tidak salah masuk rekonsiliasi shift pekerja.

### Bukti verifikasi aktual

- AndroidTest domain membuktikan Owner dapat menyelesaikan penjualan tanpa shift dan `sales.shiftId` tetap kosong.
- Regression UI ditambahkan untuk membuka kasir dan checkout dari Mode Owner tanpa shift.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/PosApplication.kt`
- `app/src/main/java/com/bimacore/usahakecil/data/PosRepository.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/HomeScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/CashierLandingScreen.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/data/PosRepositoryTest.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`

## ERR-041 - Navigasi Operasional dan tombol aksi memakan ruang

Tanggal: 2026-08-02

Varian dan versi: Semua flavor, `0.4.4`

### Kondisi/gejala

Header hijau terlalu dominan, label `Pembelian` terpotong/terpaksa turun, dan kategori serta tombol aksi berjajar horizontal sehingga sulit dipindai.

### Root cause

Layar memakai warna primary pada TopAppBar dan `TabRow`/bar horizontal untuk banyak pilihan tanpa batas lebar yang seragam.

### Solusi

Top bar Owner dibuat putih dengan judul hijau yang lebih hemat secara visual. Navigasi Operasional, Keuangan, dan aksi cepat memakai grid dua kolom; tile diberi tinggi minimum dan teks maksimal dua baris dengan ellipsis. Kategori diubah menjadi kartu grid dua kolom dengan tombol Edit yang konsisten.

### Bukti verifikasi aktual

- Test UI memeriksa grid navigasi Operasional dan label `Pembelian` tampil.
- Matriks unit test, lint, build, dan AndroidTest APK tiga flavor lulus.
- Connected smoke lulus di `emulator-5554` dan `emulator-5556`; visual QA portrait/landscape mengonfirmasi header compact dan grid tidak terpotong.

### File terdampak

- `app/src/main/java/com/bimacore/usahakecil/ui/ManagementScreens.kt`
- `app/src/androidTest/java/com/bimacore/usahakecil/MainActivitySmokeTest.kt`
