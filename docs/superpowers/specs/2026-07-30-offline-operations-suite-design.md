# Desain Penyelesaian Fitur Offline Usaha Kecil Suite

## Status

Disetujui untuk diimplementasikan pada 30 Juli 2026.

Fokus rilis ini adalah fungsi operasional. Perombakan visual khusus ditunda. Semua layar baru tetap wajib terbaca, mempunyai target sentuh yang layak, menampilkan validasi yang jelas, dan dapat dipakai pada HP.

## Tujuan

Menyelesaikan fungsi offline yang sudah dikunci untuk tiga APK:

1. Retail dan UMKM;
2. Grosir dan Agen;
3. Kuliner dan Pedagang Kaki Lima.

Ketiga APK tetap berasal dari satu source dan satu shared core. Perbedaan fungsi dikendalikan oleh konfigurasi kemampuan flavor, bukan salinan codebase.

## Batas rilis

### Termasuk

- profil usaha lokal;
- produk atau menu, kategori, varian, dan status aktif;
- stok masuk, stok keluar, penyesuaian dengan alasan, serta riwayat pergerakan;
- supplier dan pembelian;
- pelanggan;
- pengeluaran, kas masuk, dan kas keluar;
- utang supplier dan piutang pelanggan beserta pembayaran bertahap;
- daftar dan detail transaksi;
- laporan penjualan, metode pembayaran, pengeluaran, arus kas, serta posisi utang-piutang;
- satu PIN Owner offline;
- pekerja harian dan freelancer/panggilan;
- kehadiran atau pekerjaan, komponen upah, dan pembayaran;
- backup lokal berversi, pemeriksaan integritas, berbagi file, dan restore aman;
- multi-satuan dan harga bertingkat untuk Grosir;
- topping, catatan pesanan, status pesanan, resep sederhana, dan pengurangan bahan untuk Kuliner.

### Tidak termasuk

- cloud, akun online, sinkronisasi, dan multi-device;
- printer Bluetooth, barcode scanner eksternal, marketplace, dan payment gateway;
- pajak otomatis;
- BPJS, PPh 21, payroll formal, login pekerja, GPS, selfie, atau absensi mandiri;
- usaha jasa;
- metode penilaian HPP.

Karena metode penilaian HPP belum disetujui, laporan rilis ini tidak mengklaim laba kotor atau laba bersih. Laporan menampilkan angka yang dapat dibuktikan tanpa memilih metode HPP: omzet, jumlah transaksi, penerimaan per metode, pengeluaran, arus kas tercatat, serta utang-piutang.

## Arsitektur

```text
UI bersama
├─ Kasir
├─ Produk dan stok
├─ Pembelian dan pihak terkait
├─ Keuangan
├─ Laporan terlindungi
├─ Tenaga kerja
└─ Pengaturan dan pemulihan
        │
Shared repositories dan aturan domain
        │
Room database lokal
        │
Konfigurasi kemampuan flavor
├─ Retail
├─ Wholesale
└─ Culinary
```

Room tetap menjadi source of truth. Semua perubahan finansial atau stok yang saling terkait dijalankan dalam satu transaksi database.

Setiap record baru mempunyai identitas stabil, waktu dibuat, dan waktu diubah. Nilai transaksi lama disimpan sebagai snapshot agar perubahan produk, harga, satuan, supplier, pelanggan, atau tarif pekerja tidak mengubah histori.

## Kemampuan tiap APK

| Kemampuan | Retail | Grosir | Kuliner |
|---|---:|---:|---:|
| Kasir dan pembayaran | Ya | Ya | Ya |
| Produk/menu dan stok | Ya | Ya | Ya |
| Supplier dan pembelian | Ya | Ya | Ya |
| Pengeluaran dan kas | Ya | Ya | Ya |
| Pelanggan dan piutang | Ya | Ya | Tidak ditampilkan |
| Utang supplier | Ya | Ya | Ya |
| Laporan dan PIN | Ya | Ya | Ya |
| Tenaga kerja | Ya | Ya | Ya |
| Backup/restore | Ya | Ya | Ya |
| Multi-satuan | Tidak ditampilkan | Ya | Tidak ditampilkan |
| Harga bertingkat | Tidak ditampilkan | Ya | Tidak ditampilkan |
| Topping/catatan/status | Tidak ditampilkan | Tidak ditampilkan | Ya |
| Resep dan bahan | Tidak ditampilkan | Tidak ditampilkan | Ya |

Fitur yang tidak relevan tidak muncul di navigasi flavor tersebut, tetapi tetap memakai komponen inti yang sama ketika domainnya sama.

## Model dan aturan data

### Profil usaha

Profil menyimpan ID usaha stabil, nama usaha, jenis flavor, waktu dibuat, dan waktu diubah. Identitas ini ikut masuk ke backup dan ditampilkan sebelum restore.

### Produk, satuan, dan harga

- Produk dapat dibuat, diedit, dinonaktifkan, dan dicari.
- Produk yang pernah dipakai transaksi tidak dihapus permanen.
- Kategori dan varian dapat dikelola.
- Harga dan kuantitas memakai batas aman domain.
- Grosir menyimpan satuan dasar, faktor konversi bilangan bulat, minimum kuantitas, dan harga per tingkat.
- Konversi selalu kembali ke satuan dasar agar stok konsisten.
- Harga baru hanya dipakai transaksi baru.

### Stok

Stok dihitung dari pergerakan yang mempunyai jenis, jumlah bertanda, alasan, waktu, dan referensi sumber.

Jenis minimal:

- stok awal;
- pembelian;
- penjualan;
- penyesuaian masuk;
- penyesuaian keluar;
- rusak;
- hilang;
- konsumsi bahan.

Stok tidak boleh negatif diam-diam. Penyesuaian manual wajib mempunyai alasan.

### Pembelian, utang, dan kas

- Pembelian menyimpan supplier, item snapshot, jumlah, satuan, harga beli, total, jumlah dibayar, status, dan waktu.
- Pembelian menambah stok dalam operasi yang sama.
- Pembayaran kurang dari total membuat utang supplier.
- Pelunasan berikutnya menambah histori pembayaran dan mengurangi sisa tanpa menghapus histori.
- Pengeluaran, kas masuk, dan kas keluar menyimpan kategori, nominal, waktu, serta catatan.
- Penjualan tunai/QRIS/transfer mencatat penerimaan kas sesuai metode pembayaran.

### Piutang pelanggan

Penjualan dapat diselesaikan sebagai piutang hanya pada flavor yang mengaktifkannya. Transaksi menyimpan pelanggan, nilai awal, pembayaran awal, sisa, status, dan histori cicilan. Sisa tidak boleh kurang dari nol.

### Mode Owner, laporan, dan PIN

- Aplikasi selalu mulai dalam Mode Kasir/Pekerja.
- Pekerja hanya dapat membuka kasir serta melihat total transaksi aktif dan stok produk pada layar kasir.
- Operasional, keuangan, laporan, profil, backup, dan restore baru muncul setelah PIN Owner benar.
- Laporan hanya membaca data setelah sesi Owner dibuka dengan PIN.
- Satu PIN Owner dipakai pada MVP.
- PIN disimpan sebagai hash dengan salt, bukan plaintext.
- Aplikasi meminta pembuatan PIN saat Mode Owner pertama kali dibuka.
- Sesi Owner dikunci kembali ketika aplikasi masuk background atau Owner menekan `Keluar Mode Owner`.
- Kesalahan PIN tidak membocorkan data laporan.
- Domain laporan juga memeriksa sesi, bukan hanya menyembunyikan tombol.

### Tenaga kerja

Pekerja mempunyai skema `HARIAN` atau `FREELANCE`.

Pekerja harian:

- owner mencatat hadir, setengah hari, izin, atau tidak hadir;
- tarif harian mempunyai tanggal berlaku;
- lembur, bonus, kasbon, dan potongan disimpan terpisah;
- pembayaran menyimpan snapshot perhitungan.

Freelancer:

- owner mencatat pekerjaan, nilai kesepakatan, tanggal, dan status;
- absensi tidak wajib;
- pembayaran dapat bertahap dan menyimpan histori.

### Kuliner

- Topping adalah tambahan berharga yang disnapshot pada item transaksi.
- Catatan pesanan disimpan pada item.
- Transaksi kuliner mempunyai status `BARU`, `DIPROSES`, `SIAP`, atau `SELESAI`.
- Resep memetakan menu ke bahan dan jumlah konsumsi.
- Penjualan mengurangi bahan secara atomik.
- HPP resep tidak dihitung pada rilis ini.

## Navigasi

Saat sesi Owner aktif, navigasi utama memakai tujuan berikut:

- `Kasir`;
- `Operasional`;
- `Keuangan`;
- `Laporan`;
- `Lainnya`.

Saat sesi Owner terkunci, navigasi pengelolaan tidak ditampilkan dan pekerja tetap di `Kasir`. `Operasional` memuat produk/menu, stok, supplier, pembelian, serta tenaga kerja. `Keuangan` memuat pengeluaran, kas, utang, dan piutang yang relevan. `Lainnya` memuat profil usaha, PIN, backup/restore, dan informasi aplikasi.

Flow kasir yang sudah ada tetap dipertahankan. Fitur baru tidak boleh membuat kasir bergantung pada internet atau login.

## Backup dan restore

Backup memakai paket file lokal yang berisi:

- versi format;
- identitas dan jenis usaha;
- waktu dibuat;
- versi skema database;
- salinan database yang konsisten;
- hash SHA-256 untuk pemeriksaan integritas.

Sebelum membuat salinan, database melakukan checkpoint. Restore:

1. membaca metadata dan memeriksa hash;
2. menampilkan identitas usaha dan tanggal;
3. meminta konfirmasi;
4. membuat backup pengaman data aktif;
5. mengganti data aktif;
6. membuka ulang database;
7. menjalankan pemeriksaan integritas;
8. memulihkan backup pengaman jika proses gagal.

File dibagikan melalui mekanisme berbagi Android. Aplikasi tidak meminta izin internet atau akses seluruh penyimpanan.

## Penanganan error

- Input tidak valid ditolak di domain dan dijelaskan dengan bahasa usaha sehari-hari.
- Tombol simpan dinonaktifkan selama operasi berlangsung untuk mencegah double submit.
- Konflik stok membuat seluruh transaksi batal.
- Kegagalan pembelian tidak boleh menambah stok sebagian.
- Pembayaran utang/piutang tidak boleh melewati sisa.
- Laporan gagal tertutup jika sesi PIN tidak valid.
- Backup rusak ditolak sebelum data aktif disentuh.
- Restore gagal mengembalikan data aktif dari backup pengaman.
- Error sensitif tidak menampilkan PIN, isi backup, atau data pribadi lengkap.

## Strategi implementasi

Pengerjaan dilakukan sebagai irisan vertikal:

1. migrasi database, profil usaha, konfigurasi kemampuan, dan navigasi;
2. produk serta stok;
3. supplier, pembelian, kas, utang, dan piutang;
4. laporan serta PIN;
5. tenaga kerja;
6. backup/restore;
7. multi-satuan Grosir;
8. fitur pesanan dan bahan Kuliner.

Setiap irisan dimulai dengan test gagal, implementasi minimum, test lulus, lalu pemeriksaan regresi pada tiga flavor.

## Verifikasi

Sebelum rilis:

- seluruh unit test tiga flavor lulus;
- tiga APK debug berhasil dibangun;
- tiga APK Android test berhasil dikompilasi;
- smoke test flow baru dilakukan pada runtime yang tersedia;
- mode offline dibuktikan dari manifest dan flow lokal;
- migrasi menjaga data transaksi versi sebelumnya;
- backup dibuat, dirusakkan untuk membuktikan penolakan, lalu restore diuji;
- flavor lain diperiksa agar fitur khusus tidak bocor;
- `ARCHITECTURE.md`, `WORKLOG.md`, `ERROR_SOLUTIONS.md`, `RELEASE_NOTES.md`, dan README diperbarui;
- `versionCode` dan `versionName` dinaikkan sebelum APK distribusi ditimpa.

Connected test hanya dijalankan jika target emulator atau HP sudah dikonfirmasi user.
