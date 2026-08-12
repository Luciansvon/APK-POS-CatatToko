# Impor Catatan Fisik ke Histori CatatToko

Status: Importer v1 diimplementasikan pada `0.6.0`; koreksi field rinci dan lampiran foto tetap tahap lanjutan.

## Tujuan

Membantu owner memindahkan catatan usaha lama dari buku atau kertas menjadi histori CatatToko tanpa mengetik ulang satu per satu.

CatatToko tidak menyediakan model AI, tidak memanggil API AI, dan tidak menyimpan API key. Pengguna memakai aplikasi AI yang sudah dimiliki, seperti Gemini, ChatGPT, atau layanan lain.

Keputusan yang dikunci: APK hanya menerima data yang sudah diubah oleh alat eksternal menjadi format impor CatatToko. APK tidak menerima foto untuk dibaca AI dan tidak melakukan konversi catatan fisik.

Prompt provider-agnostic versi 1 tersedia di `docs/templates/CATATTOKO_CONVERT_PHYSICAL_NOTES_PROMPT_V1.md` dan dibundel di APK agar dapat disalin dari layar importer.

Prompt juga mewajibkan AI eksternal menjadi pemandu setelah konversi: merangkum hasil, menunjukkan baris yang perlu diperiksa, menjelaskan cara menyimpan/mengimpor JSON, dan memberi tahu secara jujur jika versi APK belum mempunyai menu importer.

Alur yang disetujui:

```text
Foto satu atau beberapa halaman
  -> Pengguna membuka aplikasi AI pilihannya
  -> AI eksternal membaca catatan dan menghasilkan format impor CatatToko
  -> Pengguna menyalin hasil atau menyimpan file JSON
  -> CatatToko memvalidasi dan membuat draf
  -> Owner memeriksa bagian yang pasti dan yang meragukan
  -> Owner mengonfirmasi
  -> Data masuk ke histori dengan penanda sumber
```

## Batas keselamatan data

- Aplikasi AI eksternal tidak pernah mendapat akses langsung ke database CatatToko.
- CatatToko tidak boleh langsung menulis transaksi, kas, stok, utang, atau piutang hanya karena menerima hasil AI.
- Semua hasil wajib menjadi draf dan baru disimpan setelah konfirmasi Owner.
- Kolom yang tidak terbaca tidak boleh ditebak diam-diam. Tampilkan nilai kosong, foto potongan sumber, dan status `Perlu dicek`.
- Tanggal, nominal, jumlah, satuan, nama produk, serta jenis kas harus diperiksa terhadap aturan domain yang sama dengan input manual.
- Setiap impor menyimpan sumber `Impor dari catatan lama`, waktu impor, dan identitas batch. Nama aplikasi AI boleh dicatat sebagai keterangan opsional, bukan syarat.
- Foto tidak dikirim oleh CatatToko. Pengguna sendiri yang memilih foto dan kebijakan privasi aplikasi AI yang dipakai.
- Owner boleh melampirkan foto sumber ke batch impor untuk audit lokal, tetapi fitur impor tetap dapat bekerja tanpa menyimpan foto.
- Impor ulang halaman yang sama harus diperingatkan untuk mencegah histori ganda.
- Histori lama tidak boleh dihitung sebagai transaksi baru hari ini dan tidak boleh membuka shift kasir.
- Koreksi setelah impor harus mempunyai jejak perubahan; data lama tidak ditimpa tanpa catatan.

## Jenis catatan tahap awal

Prioritas rancangan:

1. penjualan harian;
2. kas masuk dan kas keluar;
3. pembelian dari pemasok;
4. utang dan piutang;
5. stok awal atau penyesuaian stok sebagai arsip review; v1 tidak mengubah stok aktif otomatis karena posisi stok sesudah catatan lama tidak dapat dibuktikan hanya dari satu baris.

Satu halaman dapat berisi lebih dari satu jenis catatan. Sistem harus meminta Owner memilih jenis jika hasil klasifikasi tidak yakin.

## Layar review yang dibutuhkan

- pilihan `Tempel hasil AI` dan `Pilih file hasil AI`;
- panduan siap salin yang menjelaskan format keluaran CatatToko kepada aplikasi AI eksternal;
- foto asli dan hasil baca dapat dibandingkan jika Owner memilih melampirkan foto lokal;
- ringkasan jumlah baris, total nominal, rentang tanggal, dan jumlah bagian yang perlu dicek;
- status per baris: `Siap masuk`, `Perlu dicek`, `Tidak diterapkan`, dan `Data ganda`;
- persetujuan Owner untuk record yang strukturnya valid tetapi ditandai ragu oleh AI;
- edit tanggal, produk, jumlah, satuan, harga, metode pembayaran, dan catatan masih tahap berikutnya;
- pemetaan manual nama lama ke produk/pelanggan/pemasok serta pembuatan data master baru masih tahap berikutnya;
- pemeriksaan total halaman terhadap jumlah hasil baca jika catatan fisik memiliki total;
- tombol akhir `Masukkan catatan siap`, terpisah dari tombol pemeriksaan.

## Pertukaran data tanpa API key

- CatatToko menyediakan panduan/prompt dan contoh struktur, bukan layanan AI.
- Aplikasi AI eksternal menghasilkan JSON berformat versi, misalnya file `CatatToko-Impor-Catatan.json`.
- Hasil dapat masuk melalui clipboard atau pemilih file Android.
- Android share dapat membantu membuka aplikasi AI dengan foto, tetapi hasil AI tidak boleh diasumsikan dapat kembali otomatis ke CatatToko. Jalur balik resmi tetap tempel teks atau pilih file.
- Parser hanya menerima field yang dikenal. Teks bebas, field tambahan, angka di luar batas, dan tanggal tidak valid ditolak atau ditandai `Perlu dicek`.
- File impor bukan backup database dan tidak boleh diproses oleh menu pemulihan backup.
- APK tidak membutuhkan API key, SDK Gemini/OpenAI, akun AI, atau internet untuk memvalidasi file yang sudah dihasilkan.

## Format minimum versi 1

- versi skema impor;
- jenis catatan per baris;
- tanggal transaksi asli;
- nama produk/pihak dan catatan sumber;
- jumlah, satuan, harga, nominal, dan metode pembayaran bila tersedia;
- status keyakinan per field: `pasti`, `perlu_dicek`, atau `kosong`;
- total halaman/batch jika tertulis pada sumber;
- identitas baris sumber untuk deteksi duplikasi.

Nama skema awal dikunci sebagai `catattoko.history-import.v1`. Record prompt: `SALE`, `PURCHASE`, `CASH_IN`, `CASH_OUT`, `EXPENSE`, `RECEIVABLE`, `PAYABLE`, `STOCK_ADJUSTMENT`, dan `UNRESOLVED`.

AI eksternal tidak boleh mengarang field yang tidak terlihat. Nilai yang ragu harus memakai `null` atau ditandai pada `uncertainFields`.

## Kontrak importer v1 yang diterapkan

- ukuran input maksimal 1 MB;
- maksimal 200 record per batch dan 50 item per record;
- JSON wajib UTF-8, memakai seluruh field yang ditentukan, dan field asing ditolak;
- zona waktu v1 dikunci `Asia/Jakarta`; tanggal tanpa jam disimpan pada pukul 12.00 sebagai timestamp teknis dengan penanda presisi `DATE_ONLY`;
- file yang sama ditolak memakai hash isi; record yang sudah pernah masuk ditandai `Data ganda` memakai fingerprint;
- `SALE`, `PURCHASE`, `CASH_IN`, `CASH_OUT`, `EXPENSE`, `RECEIVABLE`, dan `PAYABLE` dapat masuk jika seluruh aturan finansial valid;
- `STOCK_ADJUSTMENT` dan `UNRESOLVED` disimpan sebagai jejak batch tetapi tidak diterapkan ke data aktif;
- transaksi lama memakai tanggal asli, tidak membuka shift, dan tidak mengubah stok aktif;
- commit batch dilakukan dalam satu transaksi database agar kegagalan tidak meninggalkan data setengah masuk;
- seluruh akses pemeriksaan dan commit hanya tersedia setelah sesi Owner aktif; kembali dari pemilih file meminta PIN Owner lagi.

## Keputusan tahap berikutnya

- CatatToko bersifat provider-agnostic; Gemini, ChatGPT, dan layanan lain boleh dipakai tanpa integrasi khusus.
- penyimpanan foto sumber di APK;
- editor field rinci dan pemetaan manual data lama ke master aktif;
- aturan penerapan penyesuaian stok lama terhadap stok aktif;
- perluasan batas batch setelah diuji memakai contoh buku nyata yang sudah disamarkan.

## Verifikasi yang tetap diperlukan dengan data nyata

- kumpulkan contoh nyata catatan pedagang yang sudah disamarkan;
- uji panduan konversi yang sama pada beberapa aplikasi AI dengan tulisan tangan Indonesia dan tabel tidak rapi;
- uji fixture tersamarkan yang mencakup tulisan ambigu, utang sebagian, dan pembelian multi-item;
- evaluasi kebutuhan editor koreksi setelah melihat pola kesalahan nyata.

## Di luar tahap pertama

- impor otomatis tanpa review Owner;
- SDK, API, API key, atau langganan AI di dalam CatatToko;
- sinkronisasi cloud permanen;
- klaim bahwa AI selalu membaca tulisan tangan dengan benar;
- penghapusan foto atau catatan sumber secara otomatis tanpa pilihan Owner.
