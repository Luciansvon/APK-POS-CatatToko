# Spesifikasi Visual Kasir Retail

## Status

Disetujui user sebagai arah visual pada 30 Juli 2026.

Tujuh gambar referensi menjadi sumber kebenaran visual untuk flow kasir Retail. Implementasi belum disetujui dan belum dikerjakan.

## Referensi visual

Folder:

`docs/design-references/retail-cashier-approved-2026-07-30/`

Urutan layar:

1. `01-cashier-worker-home.png`
2. `02-product-list.png`
3. `03-product-search.png`
4. `04-cart.png`
5. `05-payment-sufficient.png`
6. `06-payment-insufficient.png`
7. `07-transaction-success.png`

Semua referensi berukuran `841 x 1870 px` dengan rasio layar HP yang sama.

## Arah visual yang dikunci

- Latar putih dengan header hijau Jade.
- Informasi utama memakai angka besar dan mudah dibaca.
- Tombol utama berukuran besar dan berada dekat area jangkauan ibu jari.
- Mode Kasir/Pekerja dan jalan masuk ke Mode Owner selalu terlihat jelas.
- Flow transaksi memakai empat tahap: Pilih Produk, Keranjang, Pembayaran, dan Selesai.
- Status stok normal, menipis, dan habis dibedakan dengan teks, ikon, warna, serta kondisi tombol.
- Keranjang dan total selalu mempunyai ringkasan yang mudah ditemukan.
- Kondisi uang cukup dan uang kurang mempunyai tampilan yang berbeda jelas.
- Kembalian menjadi fokus utama setelah transaksi tunai berhasil.
- Gaya ikon, radius, garis, bayangan, jarak, dan tipografi mengikuti referensi sedekat mungkin.

## Behavior tiap layar

### 1. Mode Kasir/Pekerja

- Menampilkan jumlah transaksi aktif hari ini.
- Menampilkan ringkasan stok menipis dan stok habis.
- Aksi utama adalah `Mulai Transaksi`.
- Aksi kedua adalah `Lihat Stok`.
- Area laporan, operasional, backup, dan restore tetap hanya untuk Owner.

### 2. Pilih Produk

- Mendukung daftar produk, pencarian, dan filter kategori.
- Produk dapat ditambahkan, dikurangi, atau dinonaktifkan ketika habis.
- Produk stok menipis mempunyai peringatan tanpa menghalangi penjualan.
- Ringkasan keranjang tetap terlihat di bawah.
- Kontrol scan barcode tidak diaktifkan sampai requirement barcode disetujui.

### 3. Keranjang

- Menampilkan item, harga snapshot, jumlah, hapus item, subtotal, dan total.
- `Hapus Semua` wajib meminta konfirmasi karena bersifat destruktif.
- Catatan pesanan khusus Kuliner tidak ditampilkan pada Retail.
- Aksi utama adalah `Lanjut ke Pembayaran`.

### 4. Pembayaran tunai

- Metode aktif MVP: Tunai, QRIS, Transfer, serta Piutang pada flavor yang mengizinkan.
- E-Wallet dan `Lainnya` tidak ditampilkan sebelum requirement-nya disetujui.
- Uang diterima dapat diketik atau dipilih dari nominal cepat.
- Jika uang cukup, tampilkan kembalian dan aktifkan `Bayar & Selesai`.
- Jika uang kurang, tampilkan nominal `Uang Kurang`, jangan tampilkan kembalian negatif, dan nonaktifkan `Bayar & Selesai`.

### 5. Transaksi berhasil

- Menampilkan status berhasil, kembalian besar, total belanja, uang diterima, dan nomor transaksi.
- Menyediakan `Bagikan Struk` dan `Transaksi Baru`.
- Judul header mengikuti tahap aktif: `Selesai` atau `Transaksi Berhasil`, bukan `Pilih Produk`.

## Penyesuaian fakta tanpa mengubah gaya visual

Referensi adalah hasil visual dari GPT Web, sehingga teks atau kontrol yang belum sesuai produk harus diperbaiki ketika dipindahkan ke Stitch atau Compose:

- Ganti `Akan disinkronkan saat koneksi kembali` menjadi informasi bahwa transaksi disimpan lokal di perangkat. MVP belum mempunyai cloud atau sinkronisasi.
- Jangan tampilkan E-Wallet dan `Lainnya` sebagai metode aktif.
- Jangan tampilkan catatan pesanan umum pada Retail.
- Jangan mengaktifkan scan barcode sebelum requirement dan permission-nya disetujui.
- Rapikan salah ketik seperti `Mode Kasir / Pekerja-`.
- Gunakan copy singkat seperti `Uang cukup. Transaksi bisa diselesaikan.` agar konsisten dengan bahasa aplikasi.

Perubahan di bagian ini hanya menyelaraskan fakta dan behavior. Komposisi, hierarki, warna, serta karakter visual referensi tetap dipertahankan.

## Batas implementasi

- Persetujuan visual ini bukan persetujuan coding.
- Desain Retail menjadi acuan shared component.
- Grosir dan Kuliner memakai struktur visual yang sama, tetapi kemampuan dan warna flavor tetap berbeda.
- Implementasi nanti wajib dibandingkan dengan gambar referensi pada viewport yang sama.
- Perubahan visual tidak boleh melemahkan validasi transaksi, akses Owner, histori stok, atau fungsi offline.
