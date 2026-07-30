# Kebutuhan Desain UI/UX

## Tujuan

Dokumen ini menjadi checklist layar dan komponen yang perlu didesain sebelum style diterapkan ke aplikasi.

Desain pertama memakai varian **Retail dan UMKM** sebagai acuan. Setelah style disetujui, struktur visual yang sama dipakai pada Grosir dan Agen serta Kuliner dan PKL dengan warna dan kebutuhan bisnis masing-masing.

## Status 30 Juli 2026

- Fungsi operasional rilis `0.2.1` sudah dibuat lebih dulu sesuai keputusan user.
- Perombakan visual khusus sengaja ditunda.
- Checklist kosong di dokumen ini berarti desain visualnya belum disetujui, bukan berarti fungsi tersebut selalu belum ada.
- Laporan laba/HPP, pekerja bulanan, jadwal/shift, pajak, dan pengaturan struk tetap di luar scope sampai requirement-nya disetujui.

## Format desain yang diterima

- screenshot, gambar PNG/JPG, file Figma, atau sketsa yang terbaca jelas;
- ukuran dasar HP yang disarankan: `360 x 800 dp`;
- bila ada layout tablet, sertakan contoh minimal `840 x 900 dp`;
- tampilkan warna, font, jarak, radius, ikon, dan gaya komponen;
- sertakan kondisi normal, aktif, nonaktif, kosong, error, dan sukses;
- tidak harus langsung sempurna; satu layar boleh diselesaikan dan diterapkan lebih dahulu.

## Prioritas layar

### P0 - Flow kasir utama

- [ ] Tombol `Buka Mode Owner`
- [ ] Mode Kasir/Pekerja tanpa navigasi area pengelolaan
- [ ] Stok produk dan total transaksi aktif tetap terlihat pekerja
- [ ] Katalog atau kasir
- [ ] Pencarian dan filter kategori
- [ ] Kartu produk normal
- [ ] Kartu produk stok menipis
- [ ] Kartu produk habis
- [ ] Badge jumlah produk dalam keranjang
- [ ] Pemilih varian ukuran, warna, atau satuan
- [ ] Keranjang
- [ ] Quantity stepper dan hapus item
- [ ] Pembayaran tunai
- [ ] Keypad uang diterima
- [ ] Kondisi uang kurang
- [ ] Pembayaran QRIS
- [ ] Pembayaran transfer
- [ ] Konfirmasi pembayaran sudah masuk
- [ ] Struk dan kembalian
- [ ] Bagikan struk
- [ ] Kalkulator

### P1 - Produk dan stok

- [ ] Daftar produk
- [ ] Tambah produk
- [ ] Edit produk
- [ ] Hapus atau nonaktifkan produk
- [ ] Kelola kategori
- [ ] Kelola varian
- [ ] Stok masuk
- [ ] Stok keluar
- [ ] Penyesuaian stok beserta alasan
- [ ] Riwayat pergerakan stok
- [ ] Peringatan stok menipis

### P1 - Pembelian dan supplier

- [ ] Daftar supplier
- [ ] Tambah atau edit supplier
- [ ] Catat pembelian
- [ ] Detail pembelian
- [ ] Status pembayaran pembelian
- [ ] Perubahan harga beli
- [ ] Peringatan dampak margin

### P1 - Kas, laporan, dan keamanan

- [ ] Pengeluaran
- [ ] Kas masuk dan kas keluar
- [ ] Daftar transaksi
- [ ] Detail transaksi
- [ ] Ringkasan penjualan harian
- [ ] Ringkasan metode pembayaran
- [ ] Pengeluaran dan arus kas tanpa klaim laba/HPP
- [ ] Dialog PIN Owner
- [ ] PIN salah, terkunci, dan berhasil
- [ ] Pengaturan, ganti PIN, dan keluar Mode Owner

### P2 - Pegawai

- [ ] Daftar pegawai
- [ ] Tambah atau edit pegawai
- [ ] Pegawai harian
- [ ] Kehadiran
- [ ] Gaji, bonus, kasbon, dan potongan
- [ ] Riwayat pembayaran pegawai

### P2 - Pengaturan dan pemulihan

- [ ] Profil dan nama toko
- [ ] Pengaturan harga
- [ ] Backup data
- [ ] Restore data
- [ ] Konfirmasi sebelum restore
- [ ] Hasil backup atau restore berhasil
- [ ] Error backup atau restore
- [ ] Tentang aplikasi dan nomor versi

### P2 - Kebutuhan khusus flavor

Grosir dan Agen:

- [ ] Satuan pcs, pak, dus, atau karung
- [ ] Harga eceran dan grosir
- [ ] Minimum quantity harga grosir
- [ ] Piutang pelanggan
- [ ] Utang supplier

Kuliner dan PKL:

- [ ] Kartu menu
- [ ] Tambahan atau topping
- [ ] Catatan pesanan
- [ ] Status pesanan
- [ ] Bahan baku sederhana

## Komponen visual yang perlu ditentukan

- [ ] Palet semantic: primary, background, surface, text, border, error, warning, dan success
- [ ] Font, ukuran judul, isi, label, harga, dan angka laporan
- [ ] Tombol primary, secondary, text, destructive, dan disabled
- [ ] Input teks, search, nominal uang, dropdown, dan validation message
- [ ] Kartu produk, kartu laporan, kartu transaksi, dan list row
- [ ] Bottom bar, top bar, tab, chip kategori, dialog, dan bottom sheet
- [ ] Ikon kategori dan fallback produk tanpa foto
- [ ] Empty state, loading state, error state, dan success state
- [ ] Minimum touch target 48 dp dan kontras teks yang terbaca

## Urutan pengerjaan desain

1. Katalog atau kasir Retail.
2. Keranjang.
3. Pembayaran tunai.
4. Struk.
5. Komponen dasar dan semantic color.
6. Produk dan stok.
7. Laporan dan PIN.
8. Modul lanjutan.

## Aturan penerapan oleh Codex

- Style buatan user menjadi acuan visual utama.
- Behavior transaksi dan keamanan tidak diubah hanya demi mengikuti tampilan.
- Jika ada bagian desain yang tidak aman, sulit disentuh, atau tidak jelas, Codex harus menunjukkan masalah dan menawarkan alternatif.
- Implementasi dilakukan per flow supaya bisa diuji sebelum lanjut ke layar berikutnya.
- Perubahan style shared core harus diperiksa pada semua flavor.
