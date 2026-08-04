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
dist/debug/CatatToko-Retail.apk
dist/debug/CatatToko-Grosir.apk
dist/debug/CatatToko-Kuliner.apk
```

## Versi 0.4.9 - 2026-08-04

Status: Siap dibagikan sebagai APK debug

### Kenapa versi ini dibuat

- Hasil audit menyeluruh CatatToko (AUDIT_CATATTOKO_2026-08-03.md) menemukan masalah prioritas P0-P2 pada pemulihan salinan data (PIN Owner dapat terganti), tier grosir lintas satuan, responsivitas laporan, varian produk pada grafik/rekap, serta format ekspor Excel.

### Perubahan

- **Perbaikan Keamanan & Pemulihan (CT-P0-01, CT-P1-07, CT-P1-06)**:
  - Restore backup kini mempertahankan PIN Owner yang sedang aktif agar pemilik tidak terkunci jika file backup berisi PIN lama.
  - Menambahkan validasi `businessUid` manifest vs profil hasil restore.
  - Menambahkan peringatan privasi sebelum membagikan file salinan data.
- **Perbaikan Tier Grosir (CT-P1-05)**:
  - Mengagregasi total `baseQuantity` per `(productId, variantId)` dari seluruh baris keranjang sebelum menentukan harga tier grosir.
- **Perbaikan Laporan & ViewModel (CT-P1-04, CT-P1-01, CT-P1-02, CT-P1-03, CT-P2-04)**:
  - Menangani reload laporan via `executeReport` berbasis `Job` agar pergantian filter/periode tidak ter-drop secara diam-diam.
  - Memisahkan dan menampilkan varian produk (`variantId` & `variantName`) pada grafik dan rekap tren laporan. Filter `orderStatus` diterapkan untuk membuang transaksi batal.
  - Menghapus batas `products.take(5)` sehingga semua produk dan varian dapat dicari dan dipilih.
  - Pemuatan perkiraan stok (forecast) dipindahkan menjadi *lazy loading*.
  - Menambahkan `formatCompactRupiah()` untuk format label angka grafik singkat ("84 rb", "1,5 jt").
- **Keandalan & Format Excel (CT-P2-10, CT-P2-11, CT-P2-12, CT-P2-06, CT-P2-08)**:
  - Perubahan kuantitas keranjang (`+` / `-`) menggunakan transaksi atomik `incrementQuantity`.
  - Menambahkan formatter bahasa Indonesia untuk status absensi, skema pekerja, status pesanan, jenis pergerakan stok, dan arus kas.
  - Memblokir pencatatan kehadiran atau pekerjaan panggilan baru untuk pekerja nonaktif.
  - Sel numerik Excel (Rupiah & Qty) diekspor sebagai nilai angka `<v>` agar dapat dihitung `SUM` di spreadsheet.
  - Rekap produk Excel dikelompokkan berdasarkan `productId, variantId`.
- **Maintainability & Test (Tahap 3)**:
  - Memperbarui teks onboarding agar sesuai dengan perilaku keamanan (aplikasi selalu mulai dalam Mode Kasir).
  - Menambahkan test migrasi database `2->4` dan `3->4`.
  - Menambahkan test varian produk & >5 produk pada `ReportTrendRepositoryTest`.
- Menaikkan `versionCode` ke `18` dan `versionName` ke `0.4.9`.

### Bukti pengujian

- Unit test lulus pada seluruh flavor (`testRetailDebugUnitTest`, `testWholesaleDebugUnitTest`, `testCulinaryDebugUnitTest`).
- Build `assembleDebug` sukses tanpa error.

## Versi 0.4.8 - 2026-08-02

Status: Siap dibagikan sebagai APK debug

### Kenapa versi ini dibuat

- Laporan masih terasa seperti panel teknis dan aksi menyimpan laporan Excel belum cukup menonjol untuk Owner UMKM yang jarang memakai aplikasi bisnis.
- Halaman Owner lain masih memakai susunan generik sehingga angka penting, aksi utama, dan kondisi kosong sulit dipindai.

### Perubahan

- Menerapkan arah Laporan opsi 2: periode yang jelas, omzet sebagai angka utama, metrik ringkas, grafik pergerakan penjualan, serta rincian lanjutan yang dapat dibuka saat dibutuhkan.
- Membuat tombol `Simpan Laporan Excel` berikon dan bertulisan lengkap, lebar penuh, dengan penjelasan singkat; setelah berkas siap tersedia tombol `Bagikan Excel`.
- Mengganti kata asing yang tidak perlu pada area baru: `backup`, `restore`, `supplier`, `freelancer`, `model`, dan `MAE` menjadi istilah usaha berbahasa Indonesia.
- Memperbaiki teks rentang tanggal yang sempat tampil rusak sebagai `â€“`; format final memakai tanda aman seperti `27 Jul - 2 Agu 2026`.
- Menampilkan nominal panjang dalam susunan adaptif agar angka seperti `Rp165.000` tidak terpotong.
- Membandingkan periode aktif dengan durasi berjalan yang sama pada periode sebelumnya.
- Menyembunyikan pilihan `Terjual` ketika grafik masih memakai `Semua produk` agar jumlah berbagai produk tidak dijumlahkan secara menyesatkan.
- Menerapkan sistem visual Owner yang konsisten pada Stok, Pembelian, Pekerja, Kas, Utang & Piutang, Transaksi, dan Lainnya.
- Mengganti label navigasi Retail `Piutang` menjadi `Keuangan`; Kasir dan isi halaman Produk tidak didesain ulang.
- Memindahkan penggantian PIN dan keluar Mode Owner ke `Lainnya > Keamanan Owner`.
- Menaikkan `versionCode` ke `17` dan `versionName` ke `0.4.8`.

### Kekurangan yang masih ada

- HPP dan laba belum dihitung karena metode HPP belum dikunci.
- Chart dan forecast belum menjadi sheet khusus di Excel.
- Visual form tambah/edit rinci masih memakai komponen dasar lama.

### Masalah yang diketahui

- Provider share Android dapat menampilkan aplikasi tujuan berbeda-beda; file Excel tetap dibuat lokal sebelum dibagikan.

### Verifikasi

- Unit test, lint, build debug, dan kompilasi AndroidTest tiga flavor lulus (`259` task, `BUILD SUCCESSFUL`).
- Connected Retail lulus `47/47` per perangkat pada `emulator-5554` dan `emulator-5556`.
- Connected Grosir dan Kuliner masing-masing menyelesaikan `49` run per perangkat dengan `2` test khusus Retail dilewati dan tanpa kegagalan.
- `ReportDemoTest` lulus pada dua perangkat, termasuk regression check bahwa karakter rusak tidak tampil dan tiga rentang tanggal memakai ` - `.
- Visual QA HP portrait dan tablet landscape mencakup Laporan, Pembelian, Pekerja, Keuangan, dan Lainnya; hasil akhir tercatat `passed` di `design-qa.md`.

### APK yang ditimpa

- Retail: `dist/debug/CatatToko-Retail.apk` | SHA256 `DD875C4CB7024E81909A464BEB784756BC26084370B0A0438C6DBEA267C5BEB8`
- Grosir: `dist/debug/CatatToko-Grosir.apk` | SHA256 `00B8D129CC086C1C79F979743826C33FED2EC583873B5D4B818F35739A28D874`
- Kuliner: `dist/debug/CatatToko-Kuliner.apk` | SHA256 `CB6A9521A8A275B039FCE831B800B49A5B24638CA884388FE926600EAAC9CA0B`

## Versi 0.4.7 - 2026-08-02

Status: Siap dibagikan sebagai APK debug

### Kenapa versi ini dibuat

- Ringkasan laporan terlalu panjang dan grafik penerimaan per metode tidak membantu Owner memantau perubahan penjualan serta arus kas.

### Perubahan

- Ringkasan laporan diubah menjadi grid KPI dua kolom agar lebih hemat ruang dan mudah dipindai.
- KPI membandingkan periode aktif dengan periode sebelumnya yang setara, memakai panah, teks, dan warna semantik; metrik arus keluar memakai arah penilaian yang benar.
- Grafik utama memiliki mode `Arus kas`, `Penjualan`, dan `Produk`.
- Grafik menyediakan granularitas `Harian`, `Mingguan`, `Bulanan`, dan `Tahunan`, dengan bucket nol tetap dirender saat belum ada data.
- Grafik Produk menyediakan pilihan `Semua produk`/produk teratas serta metrik `Omzet` atau `Terjual`.
- Grafik pembayaran tetap tersedia sebagai rincian sekunder, sementara forecast tetap berada di bagian analisis stok.
- Menambahkan repository trend query, test data kosong, dan demo connected untuk seluruh mode grafik serta periode laporan.
- Menaikkan `versionCode` ke `16` dan `versionName` ke `0.4.7`.

### Kekurangan yang masih ada

- Chart dan forecast belum menjadi sheet khusus di Excel.
- HPP dan laba belum dihitung otomatis karena metode HPP belum dikunci.
- Grafik belum menyediakan zoom atau ekspor gambar; detail bucket tersedia melalui pemilihan batang.

### Masalah yang diketahui

- Provider share Android dapat menampilkan aplikasi tujuan berbeda-beda; file tetap dibuat lokal dan dapat dibagikan dari tombol `Bagikan Excel`.

### Verifikasi

- Unit test tiga flavor dan compile AndroidTest Retail lulus.
- Connected `ReportDemoTest` Retail lulus pada `emulator-5554` dan `emulator-5556` setelah kontrol periode diuji dengan sinkronisasi pemuatan data.
- Repository trend test memeriksa 14 bucket nol pada database kosong.
- Full flavor build dan compile AndroidTest tiga flavor lulus setelah patch label sumbu.
- Connected matrix tiga flavor lulus pada dua emulator: Retail `47/47` per device; Wholesale dan Culinary `49` run per device dengan 2 test Retail-only dilewati.
- Setelah patch label sumbu, targeted `ReportDemoTest` dan `ReportTrendRepositoryTest` lulus pada `emulator-5554` dan `emulator-5556`.
- Screenshot final data kosong memperlihatkan KPI grid dua kolom, selector grafik, 14 bucket harian nol, angka `0`, dan label tanggal tanpa ellipsis.

### APK yang ditimpa

- Retail: `dist/debug/CatatToko-Retail.apk` | SHA256 `4C4CB2BEBFFFFF5794A1972F3902CC2269DDB9A76BD6516F18322F23ADBBE110`
- Grosir: `dist/debug/CatatToko-Grosir.apk` | SHA256 `F50BAC681756478FF6A0BDE6DADC03C67CA13BA71C444A0F389AB61701560A31`
- Kuliner: `dist/debug/CatatToko-Kuliner.apk` | SHA256 `C43F00F1361532205CF73791D47E1701F648D8F40500E75B2F68B8A0FAE84323`

## Versi 0.4.6 - 2026-08-02

Status: Siap dibagikan sebagai APK debug

### Kenapa versi ini dibuat

- Memindahkan export Excel ke konteks yang benar, yaitu halaman `Laporan`.
- Menyediakan pilihan periode yang jelas sebelum Owner membuat export.
- Membuat grafik perkiraan dapat dibaca meskipun nilai antarhari berdekatan.

### Perubahan

- `Laporan` sekarang memiliki pilihan `Hari ini`, `Minggu ini`, `Bulan ini`, dan `Tahun ini`.
- Ringkasan laporan dan workbook Excel mengikuti periode yang dipilih.
- Event transaksi, pembelian, kas, utang, pembayaran utang, shift, stok, kehadiran, pekerjaan panggilan, dan pembayaran pekerja difilter berdasarkan periode export.
- Data katalog/master tetap menjadi snapshot kondisi saat export agar konteks usaha tidak hilang.
- `Export Excel` dan `Bagikan Excel` dipindahkan dari `Lainnya > Backup & restore` ke bagian atas `Laporan`.
- Grafik forecast menampilkan nilai setiap hari, satuan, dan rentang skala sehingga perbedaan kecil tetap terbaca.
- Menaikkan `versionCode` ke `15` dan `versionName` ke `0.4.6`.

### Kekurangan yang masih ada

- Chart dan forecast belum menjadi sheet khusus di Excel.
- HPP dan laba belum dihitung otomatis karena metode HPP belum dikunci.
- Filter periode memakai waktu lokal perangkat; perubahan jam perangkat dapat memengaruhi batas periode.

### Masalah yang diketahui

- Provider share Android dapat menampilkan aplikasi tujuan berbeda-beda; file tetap dibuat lokal dan dapat dibagikan dari tombol `Bagikan Excel`.

### Verifikasi

- Unit test: tiga flavor lulus.
- Lint/build: lint tiga flavor, `assembleDebug`, dan AndroidTest APK tiga flavor lulus; `259 actionable tasks`.
- Smoke test: Retail `46/46` per emulator; Wholesale dan Culinary `48` run per emulator dengan `2` test Retail-only dilewati, tanpa failure.
- Demo/export: `ReportDemoTest` serta filter workbook periode lulus pada dua emulator; UI export protected dan Owner tanpa shift lulus.
- HP/emulator: `emulator-5554` dan `emulator-5556`; portrait dan landscape.
- Kondisi offline: query laporan/export memakai database lokal dan tidak menambah syarat internet atau shift Owner.

### APK yang ditimpa

- Retail: `dist/debug/CatatToko-Retail.apk` | SHA256 `1A71CCE4A56E548E2DC8528666FF31FCCD5C5E8E443EE2AAEEDDD0ED17C19DEC`
- Grosir: `dist/debug/CatatToko-Grosir.apk` | SHA256 `4B36F1DA7C736FDB77BE3E43A0ED5B19F2C46FA83919E09DAEEAC8374DB18EEE`
- Kuliner: `dist/debug/CatatToko-Kuliner.apk` | SHA256 `A206F4014F3893FBDD0606385950A57AEB9120B944A0FE6068180F2E8AD426BA`

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

## Versi 0.4.5 - 2026-08-02

Status: Siap dibagikan sebagai APK debug

### Kenapa versi ini dibuat

- Memastikan grafik laporan tetap terbaca ketika periode belum mempunyai transaksi.
- Menambahkan jalur foto untuk produk/menu.
- Menambah demo test laporan yang mengisi data fiktif dan memeriksa seluruh kartu, grafik, analisis, periode, serta export Excel.

### Perubahan

- Grafik penerimaan selalu menampilkan Tunai, QRIS, Transfer, dan Piutang; metode tanpa data tampil `Rp0`.
- Export Excel menganggap angka histori shift yang kosong sebagai `Rp0`, sehingga satu shift lama yang belum lengkap tidak menggagalkan export.
- Form tambah/edit produk memiliki tombol `Pilih foto menu`/`Ganti foto menu`, menyimpan URI foto secara lokal, dan mempertahankan foto saat edit tanpa memilih foto baru.
- `ReportDemoTest` memeriksa kartu laporan, agregasi harian/mingguan/bulanan/tahunan, forecast 7 hari, ranking produk, dan workbook Excel dengan data demo.
- Menambahkan regression test UI untuk keberadaan pemilih foto menu.
- Menaikkan `versionCode` ke `14` dan `versionName` ke `0.4.5`.

### Kekurangan yang masih ada

- Layar laporan saat ini tetap menampilkan ringkasan hari ini; agregasi periode lain sudah diuji di repository/export fixture, tetapi selector periode di UI belum ditambahkan.
- Export Excel masih mengekspor seluruh data offline; pemilihan periode export belum tersedia.
- Shift pekerja tetap harus dibuka sebelum pekerja menerima pembayaran.
- HPP dan laba belum dihitung otomatis karena metode HPP belum diputuskan.

### Masalah yang diketahui

- Foto dari provider yang tidak memberi izin baca permanen dapat menampilkan fallback ikon setelah provider menghapus aksesnya.
- Kontras jam/status bar masih bergantung pada konfigurasi emulator/perangkat.

### Verifikasi

- Unit test: tiga flavor lulus.
- Lint/build: matriks lint, `assembleDebug`, dan tiga target AndroidTest lulus.
- Demo laporan: semua kartu, empat metode grafik, empat periode agregasi, forecast/analisis, dan workbook Excel lulus pada Retail di dua emulator.
- UI smoke: form produk menampilkan `Pilih foto menu` pada semua flavor.
- Connected smoke final: Retail `45/45` per emulator; Grosir dan Kuliner `47` run per emulator dengan `2` test Retail-only dilewati dan tanpa failure.
- Kondisi offline: URI foto disimpan di database lokal dan Owner tetap dapat memakai laporan/export tanpa shift pekerja.

### APK yang ditimpa

- Retail: `dist/debug/CatatToko-Retail.apk` — SHA256 dicatat setelah packaging.
- Grosir: `dist/debug/CatatToko-Grosir.apk` — SHA256 dicatat setelah packaging.
- Kuliner: `dist/debug/CatatToko-Kuliner.apk` — SHA256 dicatat setelah packaging.

### Hash final APK

- Retail: `754D99F37E5310E391583C2A421191DB7F4892B3C24F17DD56A0B8026896F6A2`
- Grosir: `C8E632EF33CFDF35B67D701048A394CEDB64F9837B9BF59BD94F729D1EBEA2AF`
- Kuliner: `C1EA3177CBF5F699A5A9BBA762140920F9175ECA3BB966212563D6376DD1F140`

## Versi 0.4.4 - 2026-08-02

Status: Siap dibagikan sebagai APK debug

### Kenapa versi ini dibuat

- Menghilangkan kewajiban shift untuk Owner saat memakai kasir.
- Merapikan ruang layar Operasional dan Keuangan agar pilihan tidak saling bertabrakan.

### Perubahan

- Owner dapat membuka katalog dan menyelesaikan transaksi tanpa membuka shift pekerja.
- Transaksi Owner tanpa shift tetap tersimpan, tetapi tidak ditautkan ke shift pekerja.
- Header Owner di Operasional, Keuangan, Laporan, dan Lainnya memakai bar putih compact.
- Tab Operasional/Keuangan, tombol aksi, dan kategori memakai grid dua kolom dengan teks maksimal dua baris.
- Menaikkan `versionCode` ke `13` dan `versionName` ke `0.4.4`.

### Kekurangan yang masih ada

- Shift pekerja tetap harus dibuka sebelum pekerja menerima pembayaran.
- HPP dan laba belum dihitung otomatis karena metode HPP belum diputuskan.

### Masalah yang diketahui

- Kontras jam/status bar masih bergantung pada konfigurasi emulator/perangkat.

### Verifikasi

- Unit test: `testRetailDebugUnitTest`, `testWholesaleDebugUnitTest`, dan `testCulinaryDebugUnitTest` lulus.
- Lint/build: matriks lint, `assembleDebug`, dan tiga target AndroidTest lulus; `259 actionable tasks`, `9 executed`, `250 up-to-date`.
- Smoke test Android: Retail `42/42` lulus di `emulator-5554` dan `emulator-5556`; Grosir dan Kuliner masing-masing `44` run per emulator, `2` test Retail-only skipped, tanpa failure.
- HP/emulator: visual QA portrait dan landscape mencakup Owner, Operasional, Laporan, dan Lainnya; `Pembelian` tampil utuh dan grid dua kolom rata.
- Kondisi offline: alur Owner tanpa shift memakai database lokal dan regression test memastikan `shiftId = null`.

### APK yang ditimpa

- Retail: `dist/debug/CatatToko-Retail.apk` — SHA256 `443ED6FAC8B134478027733912482B968E2C825191484D933B56BE697A5CA005`
- Grosir: `dist/debug/CatatToko-Grosir.apk` — SHA256 `793621036D1FCB33D63C84270DB5F520BE212C2312085731431F766F1AB89E21`
- Kuliner: `dist/debug/CatatToko-Kuliner.apk` — SHA256 `0FC6F2BE843E96696AA1A72E26EAD8EF555B3DD40D10DB4843BA0664DC7D164D`

## Versi 0.4.3 - 2026-08-02

Status: Siap dibagikan sebagai APK debug

### Kenapa versi ini dibuat

- Membuat alur pekerja yang belum membuka shift lebih mudah ditemukan sebelum mulai menerima pesanan.

### Perubahan

- Tombol `Mulai Transaksi` sekarang membuka form `Buka Shift` ketika belum ada shift aktif.
- Jika shift sudah aktif, alur tombol tetap langsung menuju katalog produk/menu.
- Menambahkan regresi AndroidTest untuk memastikan katalog tidak dibuka sebelum shift diisi.
- Menaikkan `versionCode` ke `12` dan `versionName` ke `0.4.3`.

### Kekurangan yang masih ada

- Shift tetap dibuka dari popup; halaman pengelolaan shift tetap berada di area Owner.

### Masalah yang diketahui

- Kontras ikon/jam status bar masih rendah pada latar terang di sebagian emulator.

### Verifikasi

- Unit test, lint, build, dan compile AndroidTest tiga flavor lulus (`259 actionable tasks`).
- Connected `MainActivitySmokeTest`: Retail 8/8 per device; Wholesale dan Culinary 8 lulus + 1 test Retail-only dilewati per device, tanpa failure.
- Regression tanpa shift lulus di `emulator-5554` portrait dan `emulator-5556` landscape; form `Buka Shift` muncul dan katalog tidak terbuka.
- Kondisi offline tetap lulus karena shift dibuka melalui database lokal tanpa PIN Owner.

### APK yang ditimpa

- Retail: `dist/debug/CatatToko-Retail.apk` | SHA256 `BE4D77121CEBE57CF483441DD7D0C0995CCD90FD5ED62E3F96CE5C79F1EB06F5`
- Grosir: `dist/debug/CatatToko-Grosir.apk` | SHA256 `F5331CD6CF7A4D3BB971C6319EB9D219C141AE5759BB71F586C8B5E0D47D0AC3`
- Kuliner: `dist/debug/CatatToko-Kuliner.apk` | SHA256 `A1DAADC9DF52E5B1C5EEFA7118B4E3424DBDCBBF1BA455C03BE2FF9F50AE4ECC`

## Versi 0.4.1 - 2026-08-01

Status: Siap dibagikan sebagai APK debug

### Kenapa versi ini dibuat

- Memperbaiki identitas CatatToko pada layar awal agar tidak memakai ikon Material generik tanpa nama brand.

### Perubahan

- Layar awal kasir menampilkan logo launcher HD dari flavor aktif beserta teks `CatatToko`.
- Mapping asset tetap mengikuti flavor: Retail jade, Grosir biru, dan Kuliner orange.
- Nama launcher/APK memakai brand `CatatToko Retail`, `CatatToko Grosir`, dan `CatatToko Kuliner`.
- Database instalasi baru tidak memiliki PIN Owner bawaan; PIN dibuat sendiri oleh owner. `2468` hanya fixture AndroidTest.
- Menambahkan regresi AndroidTest untuk keberadaan logo dan teks brand.
- Menaikkan `versionCode` ke `10` dan `versionName` ke `0.4.1`.

### Kekurangan yang masih ada

- Logo ditampilkan pada layar awal Compose; system splash Android tetap memakai adaptive launcher icon bawaan Android.

### Masalah yang diketahui

- Kontras ikon/jam status bar masih rendah pada latar terang di sebagian emulator.

### Verifikasi

- Unit test tiga flavor: `testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest` - BUILD SUCCESSFUL.
- Lint tiga flavor: `lintRetailDebug lintWholesaleDebug lintCulinaryDebug` - BUILD SUCCESSFUL.
- Build dan compile AndroidTest tiga flavor - BUILD SUCCESSFUL.
- Connected smoke: Retail 5/5 lulus pada `emulator-5554` dan `emulator-5556`; Wholesale dan Culinary 5 lulus dengan 1 test Retail-only dilewati per device, tanpa failure.
- Visual QA: 6 screenshot layar awal (3 flavor x 2 device) diaudit vision; logo dan warna flavor tidak tertukar, teks `CatatToko` tampil, dan fresh install menampilkan `Buat PIN Owner`.
- Kondisi offline: tidak ada perubahan; fungsi inti tetap memakai Room lokal.

### APK yang ditimpa

- Retail: `dist/debug/CatatToko-Retail.apk` | SHA256 `E2F05D2C4E020CCCEC1DD6759B304F2C4C1010A1B60EDCCD09A1A4CC7EA7F36A`
- Grosir: `dist/debug/CatatToko-Grosir.apk` | SHA256 `A41F3A37823389409D5C2F0B40F34C8E89D6BCC22A53272046A2CFCAD4CBBE32`
- Kuliner: `dist/debug/CatatToko-Kuliner.apk` | SHA256 `7641E6B3AB481B63BC7D28851A586A721056EA1D433EB205AE869993F6710A59`

## Versi 0.4.2 - 2026-08-01

Status: Siap dibagikan sebagai APK debug

### Kenapa versi ini dibuat

- Menyediakan export data operasional offline yang bisa dipakai Owner dalam format `.xlsx`.
- Memperbaiki waktu export yang sebelumnya tampil sebagai timestamp mentah.
- Menambahkan panduan wajib first-run agar perbedaan Mode Kasir/Pekerja dan Mode Owner tidak terlewat.

### Perubahan

- Menambahkan workbook `.xlsx` OpenXML dengan sheet informasi dan tabel operasional.
- Menambahkan tombol `Export Excel` dan `Bagikan Excel` pada area Owner.
- Mengubah `Info Export!B4` menjadi format tanggal/jam Indonesia yang terbaca manusia.
- Mengatur lebar setiap kolom berdasarkan teks terpanjang dengan batas 10–72 karakter agar tidak ada teks penting yang kepotong.
- Menambahkan ringkasan angka deterministik; filter periode dan chart Excel belum termasuk.
- Memperbaiki posisi tombol Excel dan busy guard agar aksi tidak tertutup snackbar atau tertekan saat proses lain berjalan.
- Menaikkan `versionCode` ke `11` dan `versionName` ke `0.4.2`.

### Kekurangan yang masih ada

- Export masih berupa snapshot seluruh data offline; filter harian/mingguan/bulanan/tahunan, chart Excel, forecasting Excel, dan analisis laba/HPP belum termasuk.

### Masalah yang diketahui

- Kontras ikon/jam status bar masih rendah pada latar terang di sebagian emulator.

### Verifikasi

- Unit test, lint, build, dan compile AndroidTest tiga flavor lulus: `259 actionable tasks`.
- Full connected smoke UI: Retail 38/38 per device; Wholesale dan Culinary 38/38 per device dengan satu test Retail-only dilewati.
- Connected `ExcelExportTest` 2/2 lulus pada Retail, Wholesale, dan Culinary di `emulator-5554` portrait serta `emulator-5556` landscape; termasuk 500 order.
- File runtime final tervalidasi artifact tool sebagai ZIP OpenXML dengan `Info Export`, `Ringkasan`, `Penjualan`, dan tabel `Katalog & Stok`; lebar kolom Catatan tidak terpotong.

### APK yang ditimpa

- Retail: `dist/debug/CatatToko-Retail.apk` | SHA256 `69A74F403049FFF5C62076DACBAE7A6927B625338BE3D588BF2112C13096987B`
- Grosir: `dist/debug/CatatToko-Grosir.apk` | SHA256 `73751F81598C831AADAB747D4900997F6D18F5FF0BCEAFC206FF579A4A6DD6CA`
- Kuliner: `dist/debug/CatatToko-Kuliner.apk` | SHA256 `8E4989C76DC63EAC305BD303B51308C1AB6878682E51F16F093450560121C892`

## Versi 0.4.0 - 2026-08-01

Status: Draft, siap dites sebagai APK debug

### Kenapa versi ini dibuat

- Menghubungkan forecasting penjualan offline ke histori transaksi nyata dan layar Laporan Owner.

### Perubahan

- Menambahkan migrasi Room skema 2 ke 3 untuk menyimpan snapshot `baseQuantity` pada item transaksi.
- Menambahkan migrasi Room skema 3 ke 4 untuk shift, `shiftId` transaksi/kas, dan unique guard satu shift aktif.
- Forecasting membaca transaksi lokal, memakai kuantitas dasar untuk Grosir, dan berjalan di background thread.
- Menambahkan kartu forecasting 7 hari pada Laporan Owner untuk maksimal lima produk dengan histori yang cukup.
- Menambahkan Buka Shift, Tutup Shift, perhitungan kas seharusnya, selisih kas, dan riwayat shift pada layar Keuangan Owner.
- Transaksi kasir sekarang ditolak jika belum ada shift aktif; pembayaran tunai dikaitkan ke shift secara atomik.
- Menambahkan regresi AndroidTest untuk migrasi, kuantitas dasar Grosir, dan penguncian laporan tanpa PIN.
- Menambahkan katalog grid adaptif HP/tablet, chart batang penerimaan, sesi Owner tanpa auto-lock, serta label navigasi compact satu baris.
- Menaikkan `versionCode` ke `9` dan `versionName` ke `0.4.0`.

### Kekurangan yang masih ada

- Forecasting restock, retur/refund, void, HPP/laba, Excel, notifikasi, dan sinkronisasi cloud belum diimplementasikan.

### Masalah yang diketahui

- Kontras ikon/jam status bar masih rendah pada latar terang di sebagian emulator.
- Uji offline manual belum menjadi bagian dari smoke run ini; fungsi inti tetap diverifikasi lewat unit test dan connected flow.

### Verifikasi

- Unit test tiga flavor: `testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest` - BUILD SUCCESSFUL.
- Lint tiga flavor: `lintRetailDebug lintWholesaleDebug lintCulinaryDebug` - BUILD SUCCESSFUL.
- Build tiga flavor: `assembleDebug` - BUILD SUCCESSFUL.
- Compile AndroidTest tiga flavor: `assembleRetailDebugAndroidTest assembleWholesaleDebugAndroidTest assembleCulinaryDebugAndroidTest` - BUILD SUCCESSFUL.
- Connected smoke test: Retail, Wholesale, dan Culinary lulus pada `emulator-5554` HP portrait serta `emulator-5556` tablet landscape.
- Visual QA: screenshot katalog, Owner, laporan chart, dan navigasi diaudit vision pada HP dan tablet.

### APK yang ditimpa

- Belum dipaketkan; `scripts/package-apks.ps1` belum dijalankan.

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
