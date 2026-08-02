# Design QA - Owner Retail 0.4.8

## Acuan dan bukti

- Acuan opsi 2: `C:\Users\shint\.codex\generated_images\019fc27b-30dc-7a90-aa18-be6dcc160962\exec-091d1b03-ad3e-4f0e-a210-22a29e8b17eb.png`
- Implementasi HP portrait 1080 x 1920: `C:\Users\shint\.codex\visualizations\2026\08\02\019fc27b-30dc-7a90-aa18-be6dcc160962\owner-redesign-0.4.8\final-report-portrait.png`
- Implementasi tablet landscape 1600 x 900: `C:\Users\shint\.codex\visualizations\2026\08\02\019fc27b-30dc-7a90-aa18-be6dcc160962\owner-redesign-0.4.8\final-report-landscape.png`
- Perbandingan dalam satu gambar: `C:\Users\shint\.codex\visualizations\2026\08\02\019fc27b-30dc-7a90-aa18-be6dcc160962\owner-redesign-0.4.8\comparison-report-reference-vs-final.png`
- Halaman lain: `final-purchase-*`, `final-worker-*`, `final-finance-*`, dan `final-more-*` pada folder bukti yang sama.

## Perbandingan wajib

- Tipografi dan hierarki: judul, periode, omzet, penjelasan, angka, serta aksi utama terbaca jelas tanpa teks terpotong.
- Tata letak: alur periode -> simpan Excel -> ringkasan -> grafik dipertahankan. Kartu angka otomatis berubah menjadi baris saat nominal panjang agar nilai tidak menjadi elipsis.
- Ukuran layar: tidak ada tumpang tindih atau aksi terpotong pada HP portrait dan tablet landscape.
- Warna dan bentuk: warna hijau Retail, permukaan putih/hijau muda, radius, tab aktif, dan navigasi konsisten dengan sistem visual aplikasi.
- Isi: istilah teknis yang tidak perlu sudah diganti menjadi bahasa usaha sehari-hari. Rentang tanggal memakai tanda ASCII ` - ` agar tidak berubah menjadi teks rusak.
- Ikon dan interaksi: ikon kalender, Excel, navigasi, pemilih periode, tab Owner, tombol aksi, rincian, salinan, dan pemulihan dapat ditekan serta mempunyai label semantik.
- Aksesibilitas: tombol utama selebar layar, target sentuh minimal mengikuti komponen Material, kontras teks jelas, dan aksi destruktif dipisahkan dari aksi utama.

## Perbedaan yang disengaja dari acuan

- Ikon unduh kecil pada acuan diganti tombol penuh `Simpan Laporan Excel`. Ini keputusan produk untuk pengguna UMKM yang kurang terbiasa dengan ikon tanpa tulisan.
- Ringkasan nominal panjang ditampilkan per baris pada HP agar angka utuh, bukan diperkecil berlebihan atau dipotong.
- Label navigasi `Piutang` menjadi `Keuangan` karena halaman juga memuat kas dan transaksi.

## Temuan dan perbaikan

- P1 selesai: rentang tanggal `â€“` diperbaiki menjadi `27 Jul - 2 Agu 2026`; regression test menolak karakter rusak dan mewajibkan tiga rentang bertanda ` - `.
- P1 selesai: `Rp165.000` yang sebelumnya terpotong diperbaiki dengan susunan metrik adaptif.
- P2 selesai: sisa `supplier`, `freelancer`, `Edit`, dan kode `RETAIL` diganti menjadi `pemasok`, `pekerja panggilan`, `Ubah`, dan `Toko & UMKM`.
- P0 tersisa: tidak ada.
- P1 tersisa: tidak ada.
- P2 tersisa: tidak ada.

## Verifikasi

- Unit test, lint, build debug, dan kompilasi AndroidTest tiga varian lulus.
- Connected test: Retail 47/47 per perangkat; Grosir dan Kuliner masing-masing 49 run dengan 2 test khusus Retail dilewati per perangkat, tanpa kegagalan.
- `ReportDemoTest` lulus pada kedua perangkat dan mencakup periode, laporan, grafik, perkiraan, serta berkas Excel.

final result: passed
