# Kebutuhan Desain UI/UX

## Tujuan

Dokumen ini menjadi checklist layar dan komponen yang perlu didesain sebelum style diterapkan ke aplikasi.

Desain pertama memakai varian **Retail dan UMKM** sebagai acuan. Setelah style disetujui, struktur visual yang sama dipakai pada Grosir dan Agen serta Kuliner dan PKL dengan warna dan kebutuhan bisnis masing-masing.

## Status 30 Juli 2026

- Fungsi operasional rilis `0.2.1` sudah dibuat lebih dulu sesuai keputusan user.
- Perombakan visual khusus sengaja ditunda.
- Arah visual flow kasir Retail sudah disetujui dari tujuh gambar GPT Web.
- Spesifikasi visual tersimpan di `docs/superpowers/specs/2026-07-30-retail-cashier-visual-design.md`.
- Checklist kosong di dokumen ini berarti desain visualnya belum disetujui, bukan berarti fungsi tersebut selalu belum ada.
- Laporan laba/HPP, pekerja bulanan, jadwal/shift, pajak, dan pengaturan struk tetap di luar scope sampai requirement-nya disetujui.

## Status 2 Agustus 2026 - Area Owner

- [x] Arah `Laporan` opsi 2 disetujui: pemilih periode, omzet utama, metrik ringkas, grafik pergerakan penjualan, dan rincian lanjutan.
- [x] Tombol `Simpan Laporan Excel` memakai ikon dan tulisan lengkap, lebar penuh, serta ditempatkan di bagian atas `Laporan`.
- [x] Sistem visual Owner dipakai pada Stok, Pembelian, Pekerja, Kas, Utang & Piutang, Transaksi, dan Lainnya: tab ringkas, angka utama, aksi bertulisan jelas, daftar, dan kondisi kosong.
- [x] Label navigasi Retail `Piutang` diganti menjadi `Keuangan` karena halaman tersebut juga berisi kas dan transaksi.
- [x] Kasir dan isi halaman Produk tetap dipertahankan sesuai keputusan user.
- [ ] Visual form tambah/edit rinci masih mengikuti komponen dasar lama dan dapat diaudit pada tahap berikutnya.

## Status 11 Agustus 2026 - Stitch Owner UI

- UI kasir/pekerja yang sudah disetujui tetap dipertahankan dan tidak menjadi target redesign ini.
- Referensi Owner `Laporan`, `Operasional/Stok`, `Keuangan/Kas`, dan `Lainnya/Backup` sudah digenerate di Stitch pada project baru karena project Stitch lama hanya memberi akses baca.
- Struktur `Laporan`: periode -> ringkasan omzet -> metrik -> `Simpan Laporan Excel` -> grafik pergerakan penjualan -> rincian transaksi.
- Patch Compose saat ini menyentuh shell Owner, komponen ringkasan/daftar Owner, `Operasional`, `Keuangan`, `Laporan`, dan `Lainnya`; layar kasir tetap tidak disentuh.
- Bukti build/unit test dan visual runtime dicatat setelah verifikasi.
- Data tetap offline-first: copy layar wajib menyebut data tersimpan di perangkat dan tidak boleh menyiratkan cloud sync.

## Status 12 Agustus 2026 - Laporan, branding, dan pengarsipan

- [x] Mode grafik memakai tab teks `Arus kas`, `Penjualan`, dan `Produk` dengan underline pilihan aktif.
- [x] Kontrol waktu memakai istilah `Dikelompokkan` dengan nilai `Per hari`, `Per minggu`, `Per bulan`, atau `Per tahun`; tidak lagi menyebut agregasi harian sebagai rentang.
- [x] Grafik bernilai nol tetap terlihat sebagai baseline `Rp0` dan pesan kosong, tanpa batang palsu.
- [x] Tanggal sumbu memuat nama bulan, pilihan periode mendapat highlight, dan seluruh kolom periode dapat disentuh.
- [x] Mode Produk memisahkan `Produk yang dilihat` dari `Angka yang ditampilkan`; seluruh produk tampil sebagai ranking dan satu produk tampil sebagai tren sesuai periode aktif.
- [x] Pintu rincian transaksi dan arus kas memakai label `Buka`/`Tutup` serta menampilkan isi pada layar yang sama.
- [x] Ekspor Excel dipisah menjadi `Ekspor Ringkasan` (satu sheet Ringkasan) dan `Ekspor Lengkap` (Ringkasan + seluruh sheet detail).
- [x] Sheet Ringkasan memiliki pengaturan fit satu halaman cetak; hasil fisik tetap bergantung pada aplikasi printer yang dipakai.
- [x] Produk, varian, dan kategori memakai istilah `Arsipkan`/`Aktifkan kembali` dengan konfirmasi dampak. Data lama dan histori tidak dihapus.
- [x] Aksi stok dinamai `Penyesuaian stok` dan menjelaskan bahwa alasan wajib dicatat.
- [x] Launcher memakai logo tanpa teks per flavor; pemuatan awal memakai logo bertulisan per flavor tanpa delay palsu.
- [x] Setelah backup selesai, aksi utama berubah menjadi `Bagikan salinan data`; berkas baru dikenali sebagai ZIP dan backup `.ukbackup` lama tetap dapat dipilih.
- [ ] Printer struk pembayaran menjadi tahap berikutnya. Tipe koneksi (Bluetooth/USB/LAN), protokol, lebar kertas, dan model printer belum dipilih; hardware printer belum diimplementasikan.
- Catatan arsitektur kandidat printer disimpan di `docs/PRINTER_REQUIREMENTS.md`: ESC/POS universal, transaksi wajib tersimpan sebelum cetak, dan kegagalan printer tidak boleh membatalkan transaksi.
- Output struk direncanakan memakai logo CatatToko sesuai flavor; printer thermal memakai versi monokrom berskala dengan fallback nama teks bila bitmap tidak didukung.
- [ ] Hapus permanen data master belum disetujui. Rancangan nanti hanya boleh mempertimbangkan data yang belum pernah direferensikan dan stoknya nol.
- [ ] Impor catatan fisik menjadi histori CatatToko adalah tahap berikutnya: foto -> aplikasi AI pilihan pengguna -> tempel/pilih JSON -> validasi CatatToko -> review Owner -> impor.
- CatatToko tidak menyediakan model AI, API key, atau integrasi provider. Requirement awal disimpan di `docs/PHYSICAL_RECORD_IMPORT_REQUIREMENTS.md`; format JSON, jenis catatan pertama, dan batas batch belum dipilih.
- [x] Keputusan integrasi dikunci: APK hanya menerima data yang sudah dikonversi alat eksternal ke format CatatToko; foto catatan tidak diproses oleh APK.
- [x] Prompt konversi provider-agnostic versi 1 disiapkan dengan skema `catattoko.history-import.v1`, keluaran JSON murni, penanda field ragu, dan larangan mengarang data.

### Audit aksi entitas 12 Agustus 2026

- Diterapkan: arsip/aktifkan kembali produk, varian, dan kategori; kategori aktif tidak dapat diarsipkan sebelum seluruh produk aktif di dalamnya diarsipkan.
- Sudah tersedia tetapi perlu tahap UI berikutnya: repository mendukung status aktif pelanggan, pemasok, pekerja, dan topping, namun aksi kelolanya belum konsisten di semua layar.
- Sudah tersedia: penyesuaian stok dengan jenis pergerakan dan alasan wajib; harga bertingkat dapat dihapus karena merupakan aturan harga, bukan histori transaksi.
- Pending spesifikasi: arsip satuan grosir, pelanggan, pemasok, pekerja, topping, serta aturan aman untuk entitas lain. Tidak ada hard delete baru pada patch ini.

## Format desain yang diterima

- screenshot, gambar PNG/JPG, file Figma, atau sketsa yang terbaca jelas;
- ukuran dasar HP yang disarankan: `360 x 800 dp`;
- bila ada layout tablet, sertakan contoh minimal `840 x 900 dp`;
- tampilkan warna, font, jarak, radius, ikon, dan gaya komponen;
- sertakan kondisi normal, aktif, nonaktif, kosong, error, dan sukses;
- tidak harus langsung sempurna; satu layar boleh diselesaikan dan diterapkan lebih dahulu.

## Prioritas layar

### P0 - Flow kasir utama

- [x] Tombol `Buka Mode Owner`
- [x] Mode Kasir/Pekerja tanpa navigasi area pengelolaan
- [x] Stok produk dan total transaksi aktif tetap terlihat pekerja
- [x] Katalog atau kasir
- [x] Pencarian dan filter kategori
- [x] Kartu produk normal
- [x] Kartu produk stok menipis
- [x] Kartu produk habis
- [x] Badge jumlah produk dalam keranjang
- [ ] Pemilih varian ukuran, warna, atau satuan
- [x] Keranjang
- [x] Quantity stepper dan hapus item
- [x] Pembayaran tunai
- [x] Keypad uang diterima
- [x] Kondisi uang kurang
- [ ] Pembayaran QRIS
- [ ] Pembayaran transfer
- [ ] Konfirmasi pembayaran sudah masuk
- [x] Struk dan kembalian
- [x] Bagikan struk
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

## Rencana kerja di Stitch

Status: Flow kasir tetap memakai referensi dan implementasi yang sudah disetujui. Arah Owner UI sudah dipindahkan ke project Stitch baru; patch pertama diterapkan pada Compose shared source tanpa mengubah layar kasir.

### Paket pertama

- Varian acuan: Retail dan UMKM.
- Ukuran frame utama: `360 x 800 dp`.
- Flow: `Kasir -> Keranjang -> Pembayaran Tunai -> Struk`.
- Layar harus menunjukkan Mode Kasir/Pekerja tanpa akses ke area pengelolaan.
- Sertakan kondisi stok normal, stok menipis, stok habis, keranjang kosong, uang kurang, transaksi berhasil, dan error penyimpanan.
- Gunakan tujuh gambar di `docs/design-references/retail-cashier-approved-2026-07-30/` sebagai acuan visual utama.

### Paket Owner flat operational surfaces

- Project Stitch: `5839994889813616259`.
- Final report screen: `Laporan Owner (Revised Final)` (`203fd8ae97dd49199cb395c205e33e7a`).
- Prompt tersimpan di `docs/stitch/owner-reports-overview-2026-08-11-prompt.md`.
- Implementasi tidak menyalin HTML Stitch; hanya keputusan hierarchy, warna, spacing, dan state yang dipindahkan ke Compose.
- Final references: `Operasional - Stok Owner Final` (`27a28e73ec6845d5bb04bbaa5649cf5b`), `Keuangan - Kas Owner (Empty State)` (`315cbf64dad04a12b093f39c116e228e`), dan `Lainnya - Backup & Keamanan (Final)` (`60f6e98df53a4fc9b858c3f0233063a8`).
- Arah visual final: app bar putih ringkas, tab teks dengan underline, satu angka utama, daftar tanpa kartu bertumpuk, divider tipis, dan CTA hanya pada konteks yang jelas.

Eksplorasi Owner awal yang memakai frame ungu, hero hijau besar, kartu bertumpuk, data contoh, atau navigasi terpotong ditolak dan bukan acuan implementasi.

### Bahan yang perlu disiapkan sebelum mulai

- screenshot runtime terbaru dari setiap langkah flow;
- aset ikon atau logo yang memang sudah disetujui;
- keputusan gaya visual, referensi, atau mood yang ingin dikejar user;
- teks, nominal, dan contoh produk yang realistis agar layout tidak diuji dengan data palsu yang terlalu pendek;
- batas behavior dari aplikasi saat ini supaya hasil Stitch tidak mengubah aturan transaksi.

### Output yang diharapkan

- satu alur layar yang tersambung, bukan kumpulan mockup lepas;
- komponen yang dapat dipakai ulang untuk tiga flavor;
- semantic color, tipografi, spacing, radius, ikon, dan state komponen;
- catatan perbedaan Retail, Grosir, dan Kuliner;
- hasil review accessibility dasar sebelum style diterapkan ke Compose.

## Aturan penerapan oleh Codex

- Style buatan user menjadi acuan visual utama.
- Behavior transaksi dan keamanan tidak diubah hanya demi mengikuti tampilan.
- Jika ada bagian desain yang tidak aman, sulit disentuh, atau tidak jelas, Codex harus menunjukkan masalah dan menawarkan alternatif.
- Implementasi dilakukan per flow supaya bisa diuji sebelum lanjut ke layar berikutnya.
- Perubahan style shared core harus diperiksa pada semua flavor.
