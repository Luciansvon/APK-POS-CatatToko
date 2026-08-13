# Barcode Offline V1

Status: Diimplementasikan pada kandidat `0.7.0`; QA kamera fisik dan signing produksi masih pending.

## Scope yang dikunci

- Retail dan Grosir menampilkan scanner barcode pada katalog kasir.
- Kuliner memakai schema shared yang sama, tetapi scanner dan pengelolaan barcode disembunyikan pada V1.
- Pemindaian, lookup, dan penambahan keranjang berjalan lokal tanpa internet.
- Format V1: EAN-13, EAN-8, UPC-A, UPC-E, dan Code 128. QR tidak dipakai.
- Satu barcode menunjuk tepat ke kombinasi produk, varian opsional, dan satuan opsional.
- Satu target boleh mempunyai beberapa barcode; satu kode barcode hanya boleh terdaftar sekali.

## Aturan data

- Barcode disimpan sebagai `String`; spasi luar dibuang, nol depan serta besar-kecil huruf dipertahankan.
- Nilai kosong, control character, dan nilai di atas 128 karakter ditolak pada repository.
- Mapping tidak dihapus permanen. Owner menonaktifkan atau mengaktifkannya kembali.
- Mapping aktif ditolak bila produk, varian, atau satuannya tidak aktif atau bukan milik produk yang sama.
- Seluruh mutasi barcode memerlukan `ReportSession.requireOwner()` pada repository.
- Worker hanya dapat lookup barcode aktif untuk menambah keranjang.

## Alur kasir

1. Tombol scan 48 dp berada di samping pencarian katalog Retail/Grosir.
2. Kamera belakang dibuka dengan CameraX dan model ML Kit bundled.
3. Analyzer hanya membaca hasil frame; lookup Room dan perubahan keranjang dilakukan ViewModel/repository.
4. Mapping lengkap langsung memakai `PosRepository.addProduct(productId, variantId, unitId)`.
5. Mapping produk yang masih memerlukan varian atau satuan memakai picker kasir yang sudah ada.
6. Scanner tetap terbuka setelah barang berhasil ditambahkan.
7. Barcode yang diam menunggu frame kosong sebelum dapat dipindai ulang.
8. Beberapa barcode dalam satu frame ditolak dengan pesan `Arahkan satu barcode saja`.
9. Barcode tidak dikenal hanya memberi pesan pada Worker; Owner yang sesinya masih terbuka mendapat aksi `Daftarkan`.

## Izin dan fallback

- Hanya manifest Retail dan Grosir meminta `android.permission.CAMERA`.
- Izin diminta ketika scanner dibuka, bukan saat aplikasi pertama berjalan.
- Jika ditolak, kasir tetap dapat mengetik barcode manual atau kembali ke pencarian produk.
- Penolakan permanen menyediakan aksi `Buka Pengaturan`.
- CameraX use case, analyzer, ML Kit scanner, dan executor dilepas ketika layar scanner ditutup.

## Penyimpanan, backup, dan export

- Room schema naik dari 5 ke 6 melalui migration non-destruktif.
- Tabel `product_barcodes` mempunyai unique index barcode dan foreign key produk/varian/satuan.
- Backup database otomatis mencakup mapping barcode dan restore mempertahankannya.
- Export Excel Lengkap mempunyai sheet `Barcode Produk`; Export Ringkasan tidak berubah.

## Batas verifikasi

- Unit test dan compile AndroidTest mencakup normalisasi, gate scanner, capability flavor, repository, migration, backup, dan export.
- Kamera, torch, rotasi, background/foreground, serta scan 20 barang beruntun wajib diuji pada perangkat fisik setelah user mengizinkan target.
- APK produksi tetap diblokir sampai keystore/secret signing tersedia.
