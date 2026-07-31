# Worklog Usaha Kecil Suite

## Tujuan

File ini mencatat pekerjaan yang benar-benar dilakukan pada project, keputusan yang sudah disetujui, bukti verifikasi, dan pekerjaan yang masih terbuka.

Worklog bukan pengganti:

- `AGENTS.md` untuk aturan kerja;
- `docs/ARCHITECTURE.md` untuk arsitektur;
- `docs/ERROR_SOLUTIONS.md` untuk bug dan root cause;
- spesifikasi fitur untuk requirement implementasi.

## Aturan pencatatan

- Tambahkan catatan secara kronologis.
- Jangan menghapus catatan lama hanya karena rencana berubah.
- Bedakan status `Selesai`, `Disetujui`, `Direncanakan`, `Ditunda`, dan `Belum diverifikasi`.
- Jangan mencatat rencana sebagai fitur yang sudah tersedia.
- Cantumkan file yang dibuat atau diubah.
- Cantumkan test, build, smoke test, atau pemeriksaan aktual yang dilakukan.
- Jika tidak ada kode yang berubah, tulis dengan jelas bahwa test dan build tidak dijalankan.
- Perubahan arsitektur harus disinkronkan dengan `docs/ARCHITECTURE.md`.
- Bugfix harus mempunyai entri di `docs/ERROR_SOLUTIONS.md`.

---

## 2026-07-29 - Inisialisasi wadah project

Status: Selesai.

### Tujuan

Membuat project baru yang terpisah dari MAUCAFE untuk keluarga APK operasional usaha kecil.

### Hasil

- Folder `usaha-kecil-suite` dibuat sebagai project terpisah.
- Repository dan source MAUCAFE tidak dipindahkan.
- Percobaan mengganti nama folder MAUCAFE dibatalkan sesuai instruksi user.
- Belum ada repository Git baru.
- Belum ada source aplikasi.

### Verifikasi aktual

- Folder `usaha-kecil-suite` terdeteksi di filesystem.
- Folder MAUCAFE lama tetap ada dan tidak berubah.

---

## 2026-07-29 - Penguncian arah produk awal

Status: Disetujui pada tingkat konsep.

### Keputusan yang disetujui

- Produk utama berupa APK Android offline-first.
- MVP berjalan di satu HP owner.
- Operasional dasar tidak bergantung pada akun, internet, atau cloud.
- Backup lokal dan export file menjadi mekanisme pemulihan awal.
- Cloud berlangganan baru dipertimbangkan setelah APK offline stabil dan digunakan nyata.
- Satu source bersama akan menghasilkan beberapa APK.
- Varian awal:
  - Warung dan UMKM;
  - Grosir dan Agen;
  - Kuliner dan Pedagang Kaki Lima.
- Tenaga kerja mendukung pekerja harian dan freelancer/panggilan.
- Kehadiran atau pekerjaan dicatat oleh owner dari HP-nya.
- Login, PIN, GPS, selfie, atau absensi mandiri pekerja tidak termasuk MVP.

### Catatan batas

- Urutan APK yang dibuat pertama belum disetujui final.
- Teknologi aplikasi dan database belum dipilih.
- Bisnis jasa seperti laundry, salon, dan bengkel belum masuk scope yang disetujui.
- Provider, harga, dan arsitektur cloud belum dipilih.

### Verifikasi aktual

Tidak ada test atau build karena tahap ini hanya mengunci arah produk.

---

## 2026-07-29 - Pembuatan dokumentasi dasar

Status: Selesai.

### File yang dibuat

- `AGENTS.md`
- `docs/ARCHITECTURE.md`
- `docs/ERROR_SOLUTIONS.md`
- `docs/WORKLOG.md`

### Isi utama

- Aturan kerja dan batas implementasi.
- Arsitektur offline satu HP owner.
- Shared core untuk tiga varian APK.
- Domain penjualan, stok, pembelian, pengeluaran, utang-piutang, pegawai, pajak opsional, laporan, backup, dan restore.
- Pemisahan tegas dari MAUCAFE.
- Format wajib pencatatan bug, root cause, solusi, serta bukti verifikasi.

### Verifikasi aktual

- Seluruh file dokumentasi terdeteksi di folder project.
- Struktur heading diperiksa.
- Tidak ada entri bug palsu di `docs/ERROR_SOLUTIONS.md`.
- Tidak ada kode aplikasi yang dibuat atau diubah.
- Test dan build tidak dijalankan karena belum ada toolchain.

---

## 2026-07-29 - Requirement kalkulator kasir

Status: Disetujui pada tingkat requirement.

### Keputusan yang disetujui

- Setiap varian yang mempunyai layar kasir wajib menyediakan kalkulator bawaan.
- Kalkulator wajib berfungsi tanpa internet.
- Kalkulator harus mudah dibuka dari layar kasir.
- Hasil kalkulator tidak boleh mengubah transaksi atau pencatatan keuangan tanpa tindakan konfirmasi yang jelas.

### Keputusan yang masih terbuka

- Kalkulator hanya berdiri sendiri atau dapat mengisi kolom uang diterima/kembalian melalui tombol `Gunakan hasil`. Keputusan ini kemudian diselesaikan pada entri `Penguncian flow kasir tunai`.

### Verifikasi aktual

- Requirement tercatat di `AGENTS.md`, `docs/ARCHITECTURE.md`, dan `docs/WORKLOG.md`.
- Tidak ada kode aplikasi yang dibuat atau diubah.
- Test dan build tidak dijalankan karena belum ada toolchain.

---

## 2026-07-29 - Pembatasan akses laporan penjualan

Status: Disetujui pada tingkat requirement.

### Keputusan yang disetujui

- Laporan penjualan hanya dapat dibuka oleh Admin atau Owner.
- Kasir biasa tidak dapat membaca laporan penjualan.
- Akses laporan membutuhkan verifikasi yang tetap berfungsi secara offline.
- Menyembunyikan tombol laporan saja tidak dianggap sebagai pengamanan.
- PIN tidak boleh disimpan dalam bentuk plaintext.

### Rekomendasi desain

- Gunakan PIN sebagai mekanisme utama MVP.
- Kunci kembali akses laporan ketika aplikasi ditutup atau setelah batas waktu tertentu.
- Biometrik dapat ditambahkan sebagai jalan pintas, bukan satu-satunya cara masuk.

### Keputusan yang masih terbuka

- Admin dan Owner memakai satu PIN bersama atau PIN yang berbeda. Keputusan ini kemudian diselesaikan pada entri `Penguncian satu PIN Laporan`.
- Lama akses laporan tetap terbuka sebelum meminta PIN lagi.

### Verifikasi aktual

- Requirement tercatat di `AGENTS.md`, `docs/ARCHITECTURE.md`, dan `docs/WORKLOG.md`.
- Tidak ada kode aplikasi yang dibuat atau diubah.
- Test dan build tidak dijalankan karena belum ada toolchain.

---

## 2026-07-29 - Penguncian satu PIN Laporan

Status: Disetujui.

### Keputusan yang disetujui

- MVP memakai satu PIN Laporan yang sama untuk Admin dan Owner.
- PIN tersebut hanya membuka area laporan yang dilindungi.
- MVP tidak membedakan identitas Admin dan Owner yang memasukkan PIN.
- Keputusan memakai PIN Admin dan PIN Owner terpisah tidak lagi terbuka untuk MVP.

### Verifikasi aktual

- Keputusan diperbarui di `AGENTS.md`, `docs/ARCHITECTURE.md`, dan `docs/WORKLOG.md`.
- Tidak ada kode aplikasi yang dibuat atau diubah.
- Test dan build tidak dijalankan karena belum ada toolchain.

---

## 2026-07-29 - Penguncian flow kasir tunai

Status: Disetujui pada tingkat requirement.

### Flow yang disetujui

1. Buyer memilih barang atau menu.
2. Kasir menekan barang atau menu pada aplikasi.
3. Aplikasi menghitung jumlah dan total belanja.
4. Kasir memasukkan uang tunai yang diberikan buyer.
5. Aplikasi langsung menghitung dan menampilkan kembalian.

### Aturan perhitungan

- `Kembalian = Uang Diterima - Total Belanja`.
- Total berubah otomatis ketika isi atau jumlah keranjang berubah.
- Kembalian berubah otomatis ketika nilai uang diterima berubah.
- Kalkulator umum tetap menjadi alat bantu terpisah dan tidak mengubah transaksi.
- Uang diterima dan kembalian menjadi bagian snapshot pembayaran transaksi.

### Keputusan yang masih terbuka

- Perilaku aplikasi ketika uang diterima kurang dari total belanja. Keputusan ini kemudian diselesaikan pada entri `Penguncian validasi uang tunai kurang`.
- Tindakan konfirmasi akhir untuk menyelesaikan transaksi. Keputusan ini kemudian diselesaikan pada entri `Penguncian Bayar & Selesai`.

### Verifikasi aktual

- Requirement diperbarui di `AGENTS.md`, `docs/ARCHITECTURE.md`, dan `docs/WORKLOG.md`.
- Tidak ada kode aplikasi yang dibuat atau diubah.
- Test dan build tidak dijalankan karena belum ada toolchain.

---

## 2026-07-29 - Penguncian validasi uang tunai kurang

Status: Disetujui.

### Keputusan yang disetujui

- Jika uang diterima lebih kecil daripada total belanja, transaksi tidak dapat diselesaikan.
- Tombol penyelesaian transaksi tetap nonaktif sampai uang diterima mencukupi.
- Layar menampilkan `Uang Kurang` beserta nominal selisihnya.
- Aplikasi tidak menampilkan nilai kembalian negatif.
- Ketika uang diterima sama dengan atau lebih besar daripada total, transaksi dapat dilanjutkan.

### Verifikasi aktual

- Keputusan diperbarui di `AGENTS.md`, `docs/ARCHITECTURE.md`, dan `docs/WORKLOG.md`.
- Tidak ada kode aplikasi yang dibuat atau diubah.
- Test dan build tidak dijalankan karena belum ada toolchain.

---

## 2026-07-29 - Penguncian Bayar & Selesai

Status: Disetujui.

### Keputusan yang disetujui

- Tombol konfirmasi akhir transaksi tunai bernama `Bayar & Selesai`.
- Transaksi baru dianggap selesai setelah tombol tersebut ditekan dan proses penyimpanan berhasil.
- Penyimpanan transaksi, pengurangan stok, pencatatan kas, dan pembaruan laporan harus berhasil sebagai satu operasi konsisten.
- Jika salah satu bagian gagal, sistem tidak boleh meninggalkan transaksi atau stok dalam kondisi setengah jadi.
- Setelah transaksi berhasil, nominal kembalian ditampilkan dengan ukuran besar.

### Verifikasi aktual

- Keputusan diperbarui di `AGENTS.md`, `docs/ARCHITECTURE.md`, dan `docs/WORKLOG.md`.
- Tidak ada kode aplikasi yang dibuat atau diubah.
- Test dan build tidak dijalankan karena belum ada toolchain.

---

## 2026-07-29 - Implementasi MVP kasir offline tiga varian

Status: Diimplementasikan dan dibangun sebagai APK debug.

### Hasil implementasi

- Satu source Kotlin, Jetpack Compose, dan Room menghasilkan tiga APK:
  - Retail dan UMKM dengan tema Jade;
  - Grosir dan Agen dengan tema Cobalt;
  - Kuliner dan PKL dengan tema Terracotta.
- Katalog adaptif HP/tablet, pencarian, kategori, stok, dan varian produk.
- Keranjang disimpan lokal dan tetap ada setelah aplikasi ditutup.
- Pembayaran Tunai, QRIS, dan Transfer dengan validasi uang kurang serta konfirmasi manual.
- Penyimpanan penjualan, snapshot item, pengurangan stok, dan pergerakan stok dilakukan dalam satu transaksi Room.
- Double tap pembayaran mengembalikan struk yang sama dan tidak membuat transaksi kedua.
- Struk dapat dibagikan sebagai PNG melalui FileProvider.
- Kalkulator kasir berdiri sendiri dan tidak mengubah keranjang.
- Aplikasi tidak meminta izin internet.

### Batas MVP saat ini

- APK masih debug dan belum ditandatangani dengan release key.
- Laporan dan PIN Laporan belum dibuat.
- CRUD produk, pembelian, supplier, pegawai, pajak, backup/restore, printer Bluetooth, dan cloud belum dibuat.
- Pengujian akhir tidak memasang APK ke emulator atau HP tanpa izin user.

### Verifikasi aktual

- `testRetailDebugUnitTest`, `testWholesaleDebugUnitTest`, dan `testCulinaryDebugUnitTest`: 24 test lulus, 0 gagal.
- `assembleDebug`: APK Retail, Wholesale, dan Culinary berhasil dibuat.
- Android test APK untuk ketiga flavor berhasil dikompilasi.
- Hasil Gradle final: `BUILD SUCCESSFUL`, 234 task.
- Pemeriksaan manifest APK memastikan package dan nama aplikasi berbeda serta tidak ada izin `INTERNET`.
- Audit UI generik tidak menemukan file yang dapat dipindai karena audit tersebut belum mendukung source Kotlin/Compose; hasilnya tidak dipakai sebagai bukti aksesibilitas.
- Pengujian perangkat fisik belum dilakukan.

---

## 2026-07-30 - Penyelesaian fitur operasional offline versi 0.2.0

Status: Selesai, diverifikasi, dan dipaketkan sebagai APK debug.

### Keputusan pelaksanaan

- User menyetujui penyelesaian fungsi tiga APK sesuai scope yang sudah dikunci.
- Perombakan visual ditunda; fungsi, keamanan data, validasi, dan keterpakaian dasar didahulukan.
- User memberi izin eksplisit untuk connected test memakai MuMu Player.
- Project tetap satu shared source; fitur khusus dikendalikan oleh konfigurasi capability flavor.

### Shared core yang diselesaikan

- Database Room dinaikkan ke skema 2 dengan migrasi eksplisit dari skema 1 tanpa destructive fallback.
- Profil usaha lokal dengan identitas stabil.
- Navigasi Kasir, Operasional, Keuangan, Laporan, dan Lainnya.
- Kelola kategori, produk, varian, status aktif, stok, serta riwayat pergerakan.
- Penyesuaian stok masuk, keluar, rusak, dan hilang dengan alasan wajib serta penolakan stok negatif.
- Supplier, pelanggan, pembelian, kas, pengeluaran, utang, piutang, dan pembayaran bertahap.
- Daftar dan detail transaksi.
- Penjualan mencatat penerimaan kas atau piutang dan menyimpan snapshot finansial.
- Laporan omzet, jumlah transaksi, penerimaan per metode, arus kas, pengeluaran, utang, dan piutang tanpa klaim laba/HPP.
- Satu PIN Laporan offline memakai salted PBKDF2 hash, sesi in-memory, ganti PIN, dan penguncian saat app background.
- Pekerja harian dan freelancer/panggilan, tarif bertanggal, kehadiran, komponen upah, pekerjaan, serta pembayaran.
- Backup ZIP lokal berversi dengan manifest, SHA-256, WAL checkpoint, berbagi file, preview identitas, safety backup, restore atomik, integrity check, dan rollback.

### Fungsi per flavor

- Retail: kasir umum, pelanggan, penjualan piutang, serta seluruh modul bersama yang relevan.
- Grosir: multi-satuan, faktor konversi ke stok dasar, harga bertingkat, pelanggan, dan piutang.
- Kuliner: topping, catatan item, status pesanan, resep sederhana, dan pengurangan bahan secara atomik.
- Capability test dan connected navigation smoke memastikan fitur Grosir tidak muncul di Kuliner/Retail serta fitur Kuliner tidak muncul di Grosir/Retail.

### Bug yang ditemukan dan diselesaikan

- Binary mismatch `kotlinx-serialization` membuat migration test crash.
- Backup melewatkan data WAL karena cursor checkpoint tidak dibaca.
- Bagian pembayaran tunai tertutup tombol selesai pada landscape.
- Test tag transaksi baru salah terpasang pada tombol berbagi struk.
- Form penyesuaian stok mengirim kode jenis yang tidak diterima repository.
- Edit produk mengaktifkan kembali produk nonaktif dan form stok edit memberi kesan stok dapat ditimpa tanpa histori.
- MuMu system server crash saat dua flavor diuji beruntun; stack trace membuktikan crash pada `RecentTasks` milik MuMu, lalu smoke test lulus ketika flavor dijalankan satu-satu.

Detail root cause, solusi, dan bukti ada di `docs/ERROR_SOLUTIONS.md` ERR-005 sampai ERR-011.

### Dokumentasi dan versi

- Menambahkan spesifikasi `docs/superpowers/specs/2026-07-30-offline-operations-suite-design.md`.
- Menambahkan rencana `docs/superpowers/plans/2026-07-30-offline-operations-suite.md`.
- Memperbarui `README.md`, `docs/ARCHITECTURE.md`, `docs/UI_UX_REQUIREMENTS.md`, `docs/ERROR_SOLUTIONS.md`, `docs/RELEASE_NOTES.md`, dan worklog ini.
- Menaikkan `versionCode` dari 2 ke 3.
- Menaikkan `versionName` dari 0.1.1 ke 0.2.0.

### Verifikasi aktual

- Unit test Retail: 34 lulus, 0 gagal.
- Unit test Wholesale: 34 lulus, 0 gagal.
- Unit test Culinary: 34 lulus, 0 gagal.
- Total unit test: 102 lulus.
- Final command unit test + `assembleDebug` + tiga androidTest build: `BUILD SUCCESSFUL in 34s`, 234 task.
- Full connected suite Retail pada MuMu Player Android 12: 15/15 lulus.
- Connected smoke Wholesale: 4 test, 3 lulus dan 1 checkout khusus Retail dilewati.
- Connected smoke Culinary: 4 test, 3 lulus dan 1 checkout khusus Retail dilewati.
- `OperationalRepositoryTest`: 7/7 lulus sebagai bagian full connected suite Retail.
- Migrasi versi 1 ke 2 lulus dan menjaga data transaksi/stok lama.
- Backup, penolakan backup rusak, serta restore data lulus pada MuMu.
- AAPT membuktikan tiga APK memakai `versionCode 3`, `versionName 0.2.0-<flavor>`, minimum SDK 23, target SDK 36.
- Manifest ketiga APK tidak meminta permission `android.permission.INTERNET`.
- Packaging final menghasilkan:
  - `Kasir-Retail-UMKM.apk` — SHA-256 `3C688B266EBBF11264EA9B78D70E31691DBDB804D8FF8ED84F0EE54546DD1621`;
  - `Kasir-Grosir-Agen.apk` — SHA-256 `B7029AA583E29EEBB3CE02079C75DE8A2E26A517B234EB0923604D0E87D7C3E7`;
  - `Kasir-Kuliner-PKL.apk` — SHA-256 `133A66DC8E59A9FF88C329F711D9CDCECF35223F8FFC25FD48C729B41B4580E6`.
- Hash file distribusi sama dengan APK hasil build untuk setiap flavor.

### Batas yang tetap berlaku

- Visual final belum dikerjakan sesuai keputusan user.
- APK masih debug dan belum memakai release signing key.
- HP fisik belum diuji.
- HPP/laba, pajak otomatis, cloud, multi-device, BPJS, payroll formal, printer, marketplace, dan payment gateway tidak ditambahkan karena berada di luar scope.

## 2026-07-30 - Patch akses Owner, shortcut flavor, dan ikon 0.2.1

Status: Selesai, diverifikasi, dan dipaketkan sebagai APK debug.

### Keputusan user

- Pekerja tidak boleh membuka operasional.
- Pekerja hanya boleh memakai kasir serta melihat total transaksi aktif dan stok.
- Area pengelolaan hanya boleh dibuka Owner.

### Implementasi

- Mode awal aplikasi diubah menjadi Mode Kasir/Pekerja tanpa navigasi pengelolaan.
- Satu PIN Owner membuka Operasional, Keuangan, Laporan, Lainnya, profil, backup, dan restore.
- Sesi Owner dikunci saat aplikasi masuk background atau lewat `Keluar Mode Owner`.
- Shortcut Owner dibedakan: Retail menonjolkan Produk/Piutang, Grosir membuka alat Grosir, dan Kuliner membuka Pesanan/Kuliner.
- Foreground adaptive icon tiga flavor diganti dengan aset transparan yang berada di safe zone Android.
- `versionCode` dinaikkan dari 3 ke 4 dan `versionName` dari 0.2.0 ke 0.2.1.

### Verifikasi final

- Red test akses gagal sebelum implementasi karena filter akses dan state sesi belum tersedia.
- Targeted unit test akses, sesi Owner, dan presentasi flavor lulus setelah implementasi.
- Unit test `40` per flavor, total `120`, lulus tanpa kegagalan.
- `assembleDebug` dan tiga APK androidTest lulus.
- Connected Retail `15/15` lulus pada MuMu Player Android 12.
- Connected Wholesale `14` lulus dengan `1` checkout Retail-only dilewati.
- Connected Culinary `14` lulus dengan `1` checkout Retail-only dilewati.
- Smoke manual membuktikan Mode Kasir/Pekerja hanya menampilkan kasir, stok produk, dan total transaksi aktif.
- Setelah PIN Owner dibuat, navigasi Operasional, Piutang, Laporan, dan Lainnya muncul.
- Launcher MuMu menampilkan ikon Retail, Grosir, dan Kuliner tanpa logo terpotong.
- Metadata build ketiga flavor memakai `versionCode 4` dan `versionName 0.2.1-<flavor>`.
- Packaging menghasilkan:
  - `Kasir-Retail-UMKM.apk` — SHA-256 `C07EB80C016F311C16192701C89FF82E11CA156EB571303B4A255D18A982A5D5`;
  - `Kasir-Grosir-Agen.apk` — SHA-256 `B5C1A41110C3CCA3460DC50CE3831716F24124E6B29E40D67D395EE7B812B29D`;
  - `Kasir-Kuliner-PKL.apk` — SHA-256 `DBAC358CAD53302EC538A0D4F9188C8E4694FFC7720AC9CE0A1A349A28912259`.

### Dokumentasi

- Root cause dan solusi dicatat sebagai ERR-012 sampai ERR-014 di `docs/ERROR_SOLUTIONS.md`.
- Keputusan akses diperbarui pada `AGENTS.md`, arsitektur, spesifikasi, dan kebutuhan UI/UX.

## 2026-07-30 - Catatan audit eksternal dan rencana desain Stitch

Status: Dicatat, belum diverifikasi, dan belum diimplementasikan.

### Arahan user

- Simpan hasil pembacaan repo dari koneksi GitHub sebagai bahan pemeriksaan lanjutan.
- Tahap berikutnya adalah membuat desain UI di Stitch dengan bantuan Codex.
- Permintaan ini belum menjadi persetujuan untuk mengubah source, memperbaiki bug, atau menerapkan desain.

### Temuan audit yang perlu dibuktikan dari source dan runtime

1. Snapshot nama usaha pada transaksi atau struk diduga masih memakai label flavor statis, bukan nama profil usaha terbaru.
2. Pembayaran awal saat membuat utang/piutang manual diduga belum membuat catatan kas, sedangkan pembayaran lanjutan sudah.
3. Pemeriksaan akses Owner diduga belum konsisten pada seluruh fungsi pengelolaan di lapisan domain/data.
4. Backup masih berupa database dalam ZIP dengan checksum integritas, belum terenkripsi, dan pembacaan ukuran file perlu diperiksa batas amannya.
5. PIN Owner diduga belum mempunyai progressive delay atau pembatasan percobaan gagal.
6. Relasi Room diduga belum memakai foreign key sehingga risiko record yatim perlu diperiksa sebelum fitur hapus/import berkembang.

Temuan di atas berasal dari audit eksternal melalui koneksi GitHub. Temuan tidak boleh dimasukkan ke `docs/ERROR_SOLUTIONS.md` atau disebut sebagai bug terkonfirmasi sebelum reproduksi, pemeriksaan root cause, dan bukti aktual dilakukan.

### Arah desain Stitch

- Mulai dari flow P0 Retail: `Kasir -> Keranjang -> Pembayaran -> Struk`.
- Gunakan behavior aplikasi yang sudah disetujui sebagai batas desain; desain tidak boleh melemahkan validasi pembayaran, akses Owner, histori stok, atau fungsi offline.
- Siapkan state normal, aktif, nonaktif, stok habis, uang kurang, error, kosong, dan sukses.
- Setelah flow Retail disetujui, turunkan sistem visual yang sama ke Grosir dan Kuliner dengan kemampuan serta warna flavor masing-masing.

### Verifikasi aktual

- Isi catatan audit dibaca dan diringkas ke worklog.
- Rencana kerja Stitch ditambahkan ke `docs/UI_UX_REQUIREMENTS.md`.
- Tidak ada source aplikasi yang diubah.
- Test, build, dan smoke test tidak dijalankan karena pekerjaan ini hanya pencatatan requirement dan temuan belum diverifikasi.

## 2026-07-30 - Persetujuan arah visual kasir Retail

Status: Desain visual disetujui, belum diimplementasikan.

### Keputusan user

- Tujuh gambar flow kasir yang dibuat melalui GPT Web sudah sesuai dengan arah visual yang diinginkan.
- Gambar tersebut menjadi acuan visual utama, bukan bahan untuk dibuat ulang dengan arah desain berbeda.

### Hasil pencatatan

- Menyalin tujuh gambar ke `docs/design-references/retail-cashier-approved-2026-07-30/`.
- Menetapkan urutan layar Mode Kasir/Pekerja, daftar produk, pencarian, keranjang, pembayaran cukup, pembayaran kurang, dan transaksi berhasil.
- Membuat `docs/superpowers/specs/2026-07-30-retail-cashier-visual-design.md`.
- Memperbarui checklist desain P0 di `docs/UI_UX_REQUIREMENTS.md`.

### Batas

- Persetujuan ini mengunci arah visual, bukan memberi persetujuan coding.
- Copy sinkronisasi, E-Wallet, `Lainnya`, catatan Retail, dan scan barcode harus mengikuti scope produk yang sudah disetujui tanpa mengubah gaya visual.

### Verifikasi aktual

- Ketujuh file referensi terdeteksi dengan ukuran `841 x 1870 px`.
- Ketujuh file berhasil disalin dan mempunyai hash SHA-256.
- Tidak ada source aplikasi yang diubah.
- Test, build, dan smoke test tidak dijalankan karena pekerjaan ini hanya pencatatan desain.

## 2026-07-30 - Implementasi flow kasir Retail dari Stitch

Status: Implementasi Compose dan verifikasi otomatis berjalan.

### Perubahan

- Menambahkan layar awal `Mode Kasir / Pekerja` dengan jumlah transaksi hari ini, stok menipis, dan stok habis dari data lokal.
- Menambahkan navigasi `CASHIER_HOME -> CATALOG -> CART -> PAYMENT -> RECEIPT`.
- Mengubah katalog menjadi daftar vertikal yang lebih dekat dengan acuan Stitch.
- Menambahkan header kasir dan indikator empat langkah pada katalog, keranjang, pembayaran, dan transaksi selesai.
- Mempertahankan kalkulator offline, varian, satuan, topping Kuliner, QRIS, Transfer, Piutang, validasi uang kurang, berbagi struk, dan PIN Owner.
- Memperbarui smoke test agar masuk melalui tombol `Mulai Transaksi`.

### Verifikasi aktual

- `testRetailDebugUnitTest` lulus memakai JDK 17 lokal.
- Unit test Retail, Wholesale, dan Culinary lulus.
- `assembleDebug` untuk tiga flavor lulus.
- Compile APK androidTest Retail, Wholesale, dan Culinary lulus.
- Connected test belum dijalankan karena belum ada konfirmasi target emulator atau perangkat.
- Visual QA masih berstatus blocked sampai ada screenshot runtime dari emulator/perangkat.

---

## 2026-07-31 - Debugging Terarah Integritas POS Suite Versi 0.3.1

Status: Selesai, diverifikasi murni, dan dipaketkan sebagai APK debug v0.3.1.

### Ringkasan Pekerjaan

Melakukan debugging terarah untuk 8 area integritas data dan kestabilan aplikasi:
1. **Stok Produk Bervarian**: Menambahkan validasi `variantId` wajib pada `adjustStock()` dan `recordPurchase()`.
2. **Inkonsistensi Pembayaran Utang & Kas**: Menambahkan pencatatan `CashEntryEntity` saat pembuat utang awal bayar DP > 0, serta `DebtPaymentEntity` saat `recordPurchase()` membayar DP > 0.
3. **Pembersihan Note & Topping Kuliner**: Menghapus `cart_line_notes` dan `cart_line_toppings` di DAO saat kuantitas item keranjang menjadi 0 (`setQuantity(0)`).
4. **Produk & Varian Nonaktif**: Menambahkan validasi `isActive` pada `addProduct()` dan `completeSale()`.
5. **Proteksi Area Owner**: Menambahkan helper `requireOwner()` pada `ReportSession` dan dipanggil di `createBackup()`, `inspectBackup()`, `confirmRestore()`.
6. **Resilience Checkout**: Membungkus `completeSale()` dalam `try/finally` di `PosViewModel` untuk menjamin `_isSaving = false` selalu dieksekusi saat error/exception.
7. **Keamanan & Validasi Backup**: Menambahkan validasi pembatasan `businessType` agar backup tidak silang flavor, membatasi ukuran ZIP max 256MB database & 1MB manifest.
8. **Pengoptimalan Indeks Query**: Memverifikasi keberadaan indeks Room pada `categoryId`, `productId`, `variantId`, `receiptNumber`, `saleId`.
9. **UI & Accessibility Fix**: Memperbaiki `ProductCard` agar mengoper parameter `onClick = onClick` ke Material3 `Card`, serta menambahkan `.verticalScroll()` pada `CashierLandingScreen`.

### Verifikasi Aktual

- **Unit Tests**: Lulus 100% pada ketiga flavor (Retail, Wholesale, Culinary) — 84/84 unit tests PASSED.
- **Connected UI Tests**: Lulus pada emulator MuMu Player Android 12 (`127.0.0.1:7555`) — 24/24 connected UI tests PASSED (`connectedRetailDebugAndroidTest`).
- **Build & Packaging**: `assembleDebug` sukses. `scripts/package-apks.ps1` memperbarui 3 APK versi `0.3.1` (versionCode 6) di `dist/debug/`:
  - `Kasir-Retail-UMKM.apk`
  - `Kasir-Grosir-Agen.apk`
  - `Kasir-Kuliner-PKL.apk`
- **Dokumentasi**: Memperbarui `docs/ERROR_SOLUTIONS.md` (ERR-016 s/d ERR-023), `docs/RELEASE_NOTES.md` (v0.3.1), dan `app/build.gradle.kts`.

