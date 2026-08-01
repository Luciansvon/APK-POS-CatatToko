# Spesifikasi Fondasi Forecasting Penjualan CatatToko

Tanggal: 2026-08-01
Status: Fondasi domain diimplementasikan, integrasi data dan UI belum dilakukan
Varian: Retail, Wholesale, dan Culinary melalui shared core

## Tujuan

Menambahkan mesin prediksi permintaan harian yang berjalan sepenuhnya offline dan memilih model berdasarkan backtesting data transaksi nyata, bukan memakai satu rumus tetap untuk semua produk.

Fondasi ini tidak mengubah transaksi, stok, harga, HPP, kas, laporan aktif, schema Room, atau UI. Output masih berupa API domain pure Kotlin untuk dipakai tahap integrasi berikutnya.

## Keputusan yang diterapkan

- Satuan waktu internal adalah hari dengan `epochDay`.
- Tanggal tanpa penjualan di antara awal dan akhir histori diisi kuantitas `0`.
- Beberapa observasi pada tanggal sama dijumlahkan dengan operasi overflow-safe.
- Kuantitas negatif ditolak karena retur/void belum mempunyai kontrak domain yang disetujui.
- Histori default dibatasi ke 730 hari terakhir agar komputasi di HP tetap terkendali.
- Horizon default adalah 7 hari dan dapat dikonfigurasi sampai 365 hari.
- Musiman default adalah 7 hari, tetapi panjang musim diberikan melalui konfigurasi, bukan ditanam permanen dalam model.
- Forecast tidak pernah mengembalikan nilai negatif.
- Nilai forecast tetap `Double` karena permintaan harapan dapat berupa pecahan. Pembulatan adalah keputusan lapisan presentasi atau perencanaan stok.

## Model kandidat

Mesin mengevaluasi kombinasi parameter berikut:

1. Moving average dengan beberapa ukuran window.
2. Simple exponential smoothing dengan beberapa nilai alpha.
3. Holt linear untuk data yang memiliki tren.
4. Holt-Winters additive untuk tren dan pola musiman.
5. Croston-SBA untuk penjualan jarang atau intermittent demand.

Model tidak dipilih berdasarkan nama atau asumsi jenis produk. Semua kandidat yang memenuhi syarat data mengikuti rolling-origin one-step backtesting pada rentang validasi yang sama.

## Metrik evaluasi

Setiap kandidat mencatat:

- MAE sebagai peringkat utama;
- RMSE sebagai tie-break pertama;
- sMAPE untuk error persentase yang aman saat nilai aktual nol;
- WAPE untuk melihat error terhadap total permintaan;
- bias untuk mendeteksi kecenderungan over-forecast atau under-forecast.

Bila beberapa kandidat mempunyai hasil praktis sama, mesin memilih model yang lebih sederhana agar perilaku lebih stabil dan lebih mudah dijelaskan.

## Kontrak API

File utama:

`app/src/main/java/com/bimacore/usahakecil/domain/forecast/SalesForecastEngine.kt`

Input utama:

```kotlin
List<DailySales>
```

Output utama:

```kotlin
SalesForecastResult
```

Output mencakup:

- kandidat terpilih;
- seluruh kandidat yang sudah diranking;
- metrik backtesting;
- parameter model;
- forecast per hari;
- rentang dan jumlah histori yang benar-benar dipakai.

## Validasi dan batas

- Histori kosong ditolak.
- Kuantitas negatif ditolak.
- Histori default minimal 19 hari setelah tanggal kosong diisi nol, yaitu 14 hari training dan 5 titik evaluasi.
- Model musiman hanya dibuat bila data minimal mencakup dua musim ditambah titik evaluasi.
- Croston hanya dibuat bila terdapat minimal dua hari dengan permintaan positif.
- Backtesting default memakai maksimal 60 titik terbaru untuk membatasi biaya komputasi.
- Kandidat dengan hasil non-finite dikeluarkan.
- Jika seluruh kandidat gagal, operasi ditolak dengan pesan domain dan tidak menghasilkan angka palsu.

## Integrasi data tahap berikutnya

Antigravity perlu menelusuri entity dan DAO transaksi aktif sebelum menulis query. Jangan menebak nama tabel atau field.

Data harian yang diberikan ke engine harus:

1. berasal dari transaksi penjualan yang sah;
2. dikelompokkan per target forecast, minimal produk dan opsional varian;
3. memakai kuantitas stok dasar untuk Grosir agar pcs, pak, dan dus tidak dicampur;
4. memakai tanggal bisnis yang konsisten;
5. tidak memasukkan transaksi yang kelak dibatalkan, diretur, atau di-refund setelah kontrak koreksi transaksi tersedia;
6. tidak memasukkan perubahan stok manual, pembelian, rusak, atau hilang sebagai penjualan.

Karena void/refund/return belum diimplementasikan, tahap integrasi awal harus mencatat batas ini secara eksplisit. Jangan menyamarkan data transaksi yang belum mempunyai mekanisme koreksi sebagai histori sempurna.

## Integrasi UI tahap berikutnya

UI awal yang disarankan berada di area Owner dan tidak muncul pada Mode Kasir/Pekerja.

Informasi minimum:

- nama produk atau varian;
- periode histori;
- prediksi 7 hari;
- model terpilih dalam istilah sederhana;
- MAE atau indikator akurasi;
- pesan `Data belum cukup` bila syarat minimum tidak terpenuhi;
- penjelasan bahwa hasil adalah perkiraan, bukan janji penjualan.

Grafik harus membantu keputusan. Gunakan diagram garis atau batang untuk aktual versus forecast. Jangan memakai diagram lingkaran untuk deret waktu hanya karena manusia menemukan bentuk bulat dan langsung kehilangan kendali.

## Keamanan, performa, dan lifecycle

- Forecast hanya boleh dihitung untuk area Owner.
- Perhitungan harus dijalankan di luar main thread.
- Tidak ada permission internet atau dependency network baru.
- Jangan menyimpan data pelanggan pada cache forecast.
- Cache hasil harus mempunyai kunci target, rentang histori, konfigurasi, dan waktu pembuatan.
- Cache harus invalid ketika transaksi terkait berubah.
- Jangan menjalankan grid search seluruh katalog setiap recomposition.

## HPP dan laba

Patch ini tidak mengimplementasikan HPP atau laba.

Repo saat ini menyatakan metode penilaian HPP belum diputuskan. Forecasting kuantitas dapat berdiri sendiri tanpa mengarang FIFO, average cost, atau metode lain. Integrasi anggaran omzet dapat memakai harga jual snapshot atau harga rencana hanya setelah kontraknya ditulis. Forecast laba tetap diblokir sampai metode HPP dan perlakuan retur disetujui.

## Verifikasi fondasi

Verifikasi lokal yang dilakukan pada file pure Kotlin:

- kompilasi `SalesForecastEngine.kt`: PASS;
- kompilasi source dan test: PASS;
- 8 skenario unit melalui runner lokal: PASS;
- demand stabil: PASS;
- tren meningkat: PASS;
- musiman mingguan: PASS;
- intermittent demand dan kandidat Croston: PASS;
- tanggal duplikat dan tanggal kosong: PASS;
- seluruh nilai nol: PASS;
- kuantitas negatif ditolak: PASS;
- histori kurang ditolak: PASS.

Belum dilakukan:

- Gradle unit test tiga flavor;
- Android lint;
- `assembleDebug`;
- kompilasi androidTest;
- connected test;
- smoke test emulator;
- visual QA.

Alasannya: GitHub App hanya memberikan akses baca dan environment tidak dapat mengunduh repository penuh dari GitHub. Status ini tidak boleh diubah menjadi `BUILD SUCCESSFUL` sampai command repo benar-benar dijalankan.

## Definition of done tahap integrasi

Tahap integrasi berikutnya selesai hanya jika:

- query histori dibuktikan dari schema repo aktual;
- unit kuantitas Grosir dinormalisasi ke stok dasar;
- akses Owner diterapkan;
- komputasi tidak berjalan pada main thread;
- empty/error/insufficient-data state tersedia;
- actual versus forecast dapat diaudit;
- unit test tiga flavor lulus;
- lint tiga flavor lulus;
- build tiga flavor lulus;
- dokumentasi arsitektur dan worklog diperbarui;
- klaim hasil sesuai bukti aktual.
