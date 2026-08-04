# Audit Menyeluruh CatatToko

Tanggal audit: 3 Agustus 2026  
Repository: `Luciansvon/APK-POS-CatatToko`  
Commit yang diaudit: `0f265fd` (`main`)  
Versi aplikasi: `0.4.8` (`versionCode 17`)  

## Kesimpulan utama

Fondasi CatatToko sudah cukup sehat untuk dilanjutkan: satu codebase untuk tiga flavor, transaksi penjualan disimpan atomik lewat Room, snapshot harga/barang sudah dipakai, pemisahan akses Owner sudah ada, dan CI terakhir lulus untuk lint, unit test, debug APK, instrumentation APK, serta minified release pada seluruh flavor.

Bagian yang belum siap disebut matang adalah **Laporan**, **pemulihan backup**, dan beberapa edge case transaksi. Masalah laporan bukan hanya tampilan: saat ini pilihan produk diam-diam dibatasi lima produk, data belum bisa diurai per varian, dan periode grafik dapat berbeda dari periode ringkasan sehingga angka awal bulan dapat hilang dari grafik. Sebelum menambah fitur impor catatan lama, sebaiknya tiga area prioritas di bawah dibereskan dulu.

1. Cegah restore backup mengganti PIN Owner tanpa peringatan atau membuat pemilik terkunci.
2. Samakan periode semua komponen laporan dan buat rekap produk/varian yang bisa dicari.
3. Betulkan harga grosir agar tier dihitung dari total kuantitas produk, bukan per baris satuan di keranjang.

Solusi impor yang direkomendasikan **tidak menanam AI ke CatatToko**. APK hanya menyediakan panduan, template CSV, prompt siap salin, pemilih file, validasi, dan preview. Foto buku dibaca oleh AI eksternal pilihan pengguna; CatatToko hanya menerima hasil terstruktur.

## Batas audit dan bukti

| Area | Status | Keterangan |
|---|---|---|
| Struktur repo dan arsitektur | Selesai | Ditelusuri dari entry point, ViewModel, repository, DAO, Room, backup, export, dan flavor configuration. |
| Correctness transaksi | Selesai | Ditelusuri dari cart sampai checkout, stok, kas, utang, dan unit grosir. |
| Laporan dan Excel | Selesai dari kode | Query, pemodelan periode, state UI, chart, dan writer XLSX diperiksa. |
| Backup, PIN, dan restore | Selesai | Format paket, checksum, validasi, pergantian database, dan skenario rollback diperiksa. |
| Test otomatis | Selesai | Ditemukan 106 fungsi `@Test`; coverage penting dan celahnya dipetakan. |
| CI GitHub | Lulus | Workflow PR terakhir lulus untuk semua flavor. |
| Build lokal | Terhalang | JDK 17 tersedia, tetapi Gradle wrapper perlu mengunduh distribusi dan akses jaringan runtime tidak berhasil. |
| Emulator/perangkat | Tidak dijalankan | Tidak ada target `adb` aktif. Instrumentation test hanya dibangun oleh CI, tidak dijalankan di emulator pada workflow. |
| Audit visual langsung | Belum dijalankan | Tidak ada screenshot live layar Laporan dari build commit ini. Penilaian UI di dokumen ini berbasis implementasi Compose, bukan klaim visual QA. |

## Hal yang sudah bagus

- Tiga flavor `retail`, `wholesale`, dan `culinary` berbagi satu sumber kode dengan capability yang cukup jelas.
- Checkout memakai transaksi database dan mutex, lalu menyimpan snapshot nama, harga, kuantitas, stok, pembayaran, dan referensi kas/utang.
- Akses ringkasan laporan, prediksi, dan export dibatasi sesi Owner.
- Room memiliki migration eksplisit dari schema 1 sampai 4 dan schema JSON ikut disimpan.
- Test suite sudah mencakup alur penting seperti checkout, stok negatif, varian, topping/resep, shift, PIN, backup round-trip, export periode, dan forecast.
- Laporan sudah menyatakan omzet bukan laba; ini benar karena metode HPP belum ditentukan.

## Prioritas temuan

Definisi prioritas:

- **P0**: berisiko mengunci akses pemilik atau merusak pemulihan data.
- **P1**: angka bisnis berpotensi salah, state membingungkan, atau data sensitif berisiko bocor.
- **P2**: menghambat pemahaman, skala, aksesibilitas, atau keandalan penggunaan rutin.
- **P3**: utang teknis dan konsistensi yang meningkatkan risiko regresi.

### P0 — selesaikan sebelum rilis berikutnya

#### CT-P0-01 — Restore dapat mengganti PIN Owner secara diam-diam

`BackupManager.restore()` mengganti seluruh database aktif dengan database dari paket backup. Tabel `report_security` ikut terganti, tetapi dialog restore tidak menjelaskan bahwa PIN lama dari backup akan menjadi PIN aktif. Jika pemilik tidak ingat PIN pada backup lama, ia dapat terkunci dari Laporan setelah restore.

Bukti: `backup/BackupManager.kt:72-121`, `ui/ManagementScreens.kt:1225-1243`.

Perbaikan yang disarankan:

- Pertahankan credential Owner yang sedang aktif ketika restore, atau wajibkan PIN backup sebelum restore.
- Setelah restore sukses, tampilkan konfirmasi yang menjelaskan PIN mana yang berlaku.
- Tambahkan test restore dengan PIN aktif berbeda dari PIN di backup.

### P1 — correctness dan keamanan

#### CT-P1-01 — Periode ringkasan dan grafik laporan tidak memiliki satu sumber kebenaran

Ringkasan memakai `ReportPeriod.range()`, sedangkan `readTrend()` selalu membuat rolling window tetap berdasarkan granularitas: harian, mingguan, bulanan, atau tahunan. Memilih “Bulan” otomatis memilih grafik mingguan delapan bucket. Kartu “Pergerakan omzet bulan ini” lalu hanya menyimpan bucket yang tanggal mulainya berada di bulan tersebut. Bucket mingguan yang dimulai pada akhir bulan sebelumnya tetapi mencakup awal bulan berjalan dapat dibuang seluruhnya.

Bukti: `ui/OperationsViewModel.kt:530-545`, `data/ReportRepository.kt:150-224`, `ui/ReportDashboardComponents.kt:171-180`.

Dampak: grafik dapat terlihat lebih kecil daripada angka omzet pada kartu, walaupun keduanya berlabel periode yang sama.

Perbaikan:

- Ubah kontrak menjadi `readTrend(range, granularity)`.
- Potong bucket berdasarkan irisan waktu, bukan hanya `bucketStart in range`.
- Semua kartu, rekap produk, grafik, detail, dan export harus membaca `ReportFilter` yang sama.
- Tambahkan test bulan yang dimulai di tengah minggu dan zona waktu Indonesia.

#### CT-P1-02 — Laporan produk diam-diam hanya menyediakan lima produk

Menu memilih `products.take(5)`. Produk keenam dan seterusnya tidak bisa dicari atau dipilih, tanpa penjelasan bahwa daftar dipotong.

Bukti: `ui/ReportDashboardComponents.kt:495-508`.

Perbaikan: ganti dropdown menjadi pencarian produk dengan `LazyColumn`; jangan membatasi data. Jika perlu ringkasan cepat, beri label eksplisit “5 produk terlaris” dan sediakan “Lihat semua produk”.

#### CT-P1-03 — Belum ada laporan per varian

Query trend produk tidak mengambil `variantId`/`variantName` dan repository mengelompokkan hanya berdasarkan `productId`. Produk dengan varian berbeda menyatu menjadi satu garis tanpa kemampuan drill-down.

Bukti: `data/OperationalDaos.kt:306-314` dan `405-423`, `data/ReportRepository.kt:171-195`.

Perbaikan: sediakan dua tingkat:

1. Rekap semua produk.
2. Detail produk terpilih yang mengurai setiap varian dan satuan.

#### CT-P1-04 — State periode bisa berubah tetapi data lama tetap tampil

`execute()` langsung berhenti tanpa pesan bila `_busy` bernilai true. Namun `selectReportPeriod()` mengubah state periode sebelum memanggil `execute`. Jika pengguna cepat mengganti periode saat operasi lain berjalan, label periode bisa berubah sementara ringkasan tetap data periode lama.

Bukti: `ui/OperationsViewModel.kt:530-556` dan `705-720`.

Perbaikan: gunakan job khusus laporan dengan pola latest-wins/cancel-and-reload. Jangan gunakan satu flag sibuk global untuk semua operasi layar.

#### CT-P1-05 — Harga grosir dihitung per baris satuan, bukan total produk

ID baris keranjang memasukkan `unitId`, sehingga produk yang sama dalam `pcs` dan `dus` menjadi baris berbeda. Tier grosir dihitung dari `baseQuantity` setiap baris. Kombinasi dua baris yang secara total mencapai batas tier dapat tetap mendapat harga non-tier.

Bukti: `data/PosRepository.kt:113-130`, `372-395`, dan `809-813`.

Perbaikan: hitung total `baseQuantity` per `(productId, variantId)` terlebih dahulu, pilih tier satu kali, lalu terapkan harga pada semua baris produk tersebut. Tambahkan test kombinasi `pcs + dus`.

#### CT-P1-06 — Backup tidak terenkripsi

Paket `.ukbackup` berisi file SQLite mentah dan manifest; SHA-256 hanya memeriksa perubahan, bukan menyembunyikan isi atau membuktikan siapa pembuatnya. Database dapat berisi data pelanggan, pekerja, transaksi, dan hash PIN.

Bukti: `backup/BackupManager.kt:28-47` dan `123-135`.

Perbaikan minimum: tampilkan peringatan jelas sebelum “Bagikan backup”. Perbaikan ideal: enkripsi paket memakai passphrase Owner dan cipher authenticated encryption. Jangan sebut checksum sebagai perlindungan privasi.

#### CT-P1-07 — Identitas manifest belum dibandingkan penuh dengan database hasil restore

Setelah restore, kode memeriksa integrity SQLite dan `businessType`, tetapi tidak memastikan `businessUid` dan `businessName` database hasil restore sama dengan preview manifest. Paket yang tidak konsisten dapat menampilkan identitas A di preview lalu memasang database B.

Bukti: `backup/BackupManager.kt:55-110`.

Perbaikan: bandingkan `businessUid`, `businessName`, `businessType`, schema, dan checksum setelah database dibuka, sebelum restore dinyatakan berhasil.

#### CT-P1-08 — Aturan impor histori harus memisahkan omzet dari stok dan kas saat ini

Catatan lama biasanya tidak cukup lengkap untuk merekonstruksi stok dan arus kas secara akurat. Jika importer memakai alur checkout normal, stok hari ini bisa berkurang lagi dan kas dapat dihitung ganda.

Perbaikan: histori impor masuk ke `sales`/`sale_items` dan ikut omzet, tetapi **tidak** mengubah stok sekarang, shift, atau `cash_entries`. Beri sumber `IMPORT` dan batch ID agar bisa diaudit dan dibatalkan sebagai satu batch.

### P2 — kejelasan UI, skala, dan keandalan

#### CT-P2-01 — Laporan produk masih berupa grafik satu seri, belum rekap yang mudah dibaca

Pengguna perlu membandingkan produk, tetapi UI utama hanya menampilkan agregat semua produk atau satu produk terpilih. Belum ada tabel/ranking dengan nama produk, varian, jumlah terjual, omzet, jumlah transaksi, harga jual rata-rata, dan kontribusi omzet.

Perbaikan: letakkan tabel/ranking “Produk terlaris” sebelum grafik. Grafik menjadi detail sekunder setelah pengguna memilih produk.

#### CT-P2-02 — Hirarki layar Laporan menunda informasi utama

Di atas ringkasan terdapat pemilih periode, tombol refresh penuh beserta paragraf, lalu tombol Excel penuh beserta paragraf. Data yang paling dicari—omzet dan produk—terdorong ke bawah.

Bukti: `ui/ManagementScreens.kt:929-997`.

Perbaikan: ringkasan muncul tepat setelah periode. Refresh menjadi icon action dengan teks “Diperbarui …”; export menjadi action sekunder di app bar atau menu.

#### CT-P2-03 — Laporan tidak otomatis segar setelah transaksi

Salinan UI sendiri menjelaskan bahwa pengguna harus menekan refresh. Ini mudah membuat pemilik membaca data lama.

Perbaikan: invalidasi laporan ketika transaksi/stock/cash revision berubah, atau gunakan Flow Room untuk summary. Tombol refresh tetap boleh ada sebagai fallback.

#### CT-P2-04 — Forecast dihitung untuk semua produk setiap reload laporan

Setiap ganti periode atau refresh memuat sampai 730 hari histori semua produk aktif dan menghitung prediksi, walaupun bagian forecast belum dibuka dan prediksi tidak bergantung pada periode terpilih.

Bukti: `ui/OperationsViewModel.kt:663-679`, `data/ReportRepository.kt:114-147`.

Perbaikan: load saat section dibuka, cache berdasarkan revisi penjualan, dan hitung hanya produk yang diminta atau batch di background.

#### CT-P2-05 — Excel selalu berisi 19 sheet, termasuk fitur flavor lain

Retail tetap menerima sheet Wholesale dan Culinary. File menjadi berat dan membingungkan.

Bukti: `export/ExcelExportManager.kt:63-89`.

Perbaikan: ekspor hanya sheet yang relevan dengan capability flavor dan sediakan pilihan sederhana “Ringkas” atau “Lengkap”.

#### CT-P2-06 — Semua nilai Excel ditulis sebagai teks

Uang, jumlah, dan tanggal ditulis sebagai `inlineStr`. Pengguna tidak dapat langsung menjumlah, mengurutkan numerik, membuat pivot, atau memformat tanggal dengan benar.

Bukti: `export/ExcelWorkbookExporter.kt:109-155`.

Perbaikan: model cell harus punya tipe `Text`, `Number`, `Currency`, `DateTime`; gunakan number format XLSX, freeze pane, dan autofilter.

#### CT-P2-07 — Export menampung seluruh hasil dan XML sheet di memori

Setiap query menjadi `List<List<String>>`, lalu seluruh XML worksheet dibangun sebagai satu `String` sebelum zip. Test skala hanya 500 order.

Bukti: `export/ExcelExportManager.kt:624-634`, `export/ExcelWorkbookExporter.kt:109-156`.

Perbaikan: stream cursor langsung ke entry XLSX, tetapkan batas praktis, dan uji puluhan ribu item pada device RAM rendah.

#### CT-P2-08 — Rekap Excel mengelompokkan nama, bukan ID stabil

Produk yang pernah diganti nama, kategori, varian, atau satuan dapat terpecah menjadi beberapa baris yang tampak seperti produk berbeda.

Bukti: `export/ExcelExportManager.kt:232-260`.

Perbaikan: kelompokkan `productId` dan `variantId`; tampilkan snapshot nama terbaru serta opsi melihat label historis.

#### CT-P2-09 — Daftar operasional tidak di-lazy-load atau dipaginasi

Layar operasional, finance, dan laporan memakai `Column.verticalScroll` sambil mengamati daftar penuh produk, purchase, kas, utang, sales, pekerja, dan pesanan. Data besar akan meningkatkan waktu query, komposisi, memori, dan jank.

Bukti utama: `ui/ManagementScreens.kt:84-135`, `630-765`; DAO mengamati tabel penuh.

Perbaikan: `LazyColumn`, paging/filter tanggal, dan hanya collect flow milik tab aktif.

#### CT-P2-10 — Tap kuantitas cepat berpotensi kehilangan increment

Tombol mengirim nilai absolut dari snapshot UI, sedangkan setiap perubahan diluncurkan dalam coroutine sendiri. Dua tap sebelum Flow Room mengirim state baru dapat mengirim target kuantitas yang sama.

Bukti: `ui/CartScreen.kt`, `ui/PosViewModel.kt:182-187`.

Perbaikan: operasi repository atomik `incrementQuantity(lineId, delta)` dan serialisasi per line.

#### CT-P2-11 — Label internal bocor ke UI

Beberapa layar menampilkan nilai seperti `CASH`, `CREDIT`, `STOCK_IN`, dan status attendance mentah. Formatter Indonesia sudah ada, tetapi belum dipakai konsisten.

Bukti: `ui/ManagementScreens.kt:266-270`, `374-379`, `760-765`, `1060-1065`.

Perbaikan: satu formatter domain ke label manusia untuk semua enum.

#### CT-P2-12 — Pekerja nonaktif masih bisa diberi aktivitas

UI menampilkan pekerja nonaktif bersama action aktif; repository pencatatan kehadiran/freelance tidak memeriksa `isActive`.

Bukti: `ui/ManagementScreens.kt:351-359`, `data/WorkforceRepository.kt:123-139` dan `203-211`.

Perbaikan: sembunyikan action, validasi ulang di repository, dan izinkan hanya melihat histori.

#### CT-P2-13 — Pembayaran utang selalu dicatat tunai dari UI

`payDebt()` meneruskan metode `CASH` tanpa pilihan. Pembayaran melalui transfer/QRIS akan mengotori komposisi pembayaran dan kas.

Bukti: `ui/OperationsViewModel.kt:406-411`.

Perbaikan: pilih metode pembayaran dan validasi pengaruhnya ke `cash_entries`.

#### CT-P2-14 — Session Owner terkunci pada setiap `onStop`

Membuka share sheet Excel/backup, berpindah singkat ke aplikasi lain, atau lifecycle tertentu dapat mengunci Owner dan mengembalikan pengguna ke mode kasir. Aman, tetapi terlalu agresif untuk alur eksternal.

Bukti: `MainActivity.kt:63-66`, `security/ReportSession.kt`.

Perbaikan: gunakan timeout pendek berbasis background duration; alur eksternal sensitif tetap melakukan re-auth eksplisit.

#### CT-P2-15 — Aksesibilitas grafik belum terverifikasi

Canvas garis hanya memiliki satu semantics node untuk titik terpilih. Batang memakai `clickable` tanpa role, selected state, atau content description per titik. Label sumbu kecil berpotensi sulit dibaca.

Bukti: `ui/ReportDashboardComponents.kt:215-287` dan `713-768`.

Perbaikan: sediakan daftar/tabel data sebagai alternatif selalu terlihat, semantics per titik, minimum touch target, dynamic type, dan uji TalkBack. Kontras warna harus diuji dari screenshot live; audit kode saja tidak cukup untuk menyatakan lulus.

#### CT-P2-16 — Penghapusan kategori dan produk belum lengkap

DAO katalog tidak memiliki operasi hapus kategori, produk, atau varian. Produk dan varian hanya dapat diaktifkan/dinonaktifkan. Kategori bahkan belum memiliki action nonaktif meskipun kolom `isActive` sudah ada di schema; UI hanya menyediakan tambah dan ubah.

Bukti: `data/Daos.kt:10-71`, `data/InventoryRepository.kt:54-70` dan `170-178`, `ui/ManagementScreens.kt:138-188`.

Perilaku yang disarankan:

- Ganti istilah `Nonaktifkan` menjadi **Arsipkan** agar maksudnya mudah dipahami.
- Produk yang sudah mempunyai penjualan, pembelian, stok, atau referensi lain hanya boleh diarsipkan agar histori laporan tetap utuh.
- Produk yang benar-benar belum pernah dipakai boleh memiliki **Hapus permanen**, setelah pemeriksaan referensi dan konfirmasi dampak.
- Kategori kosong boleh dihapus permanen.
- Kategori yang masih memiliki produk tidak boleh dihapus diam-diam; tampilkan jumlah produk dan minta pengguna memindahkan atau mengarsipkan produk terlebih dahulu.
- Sediakan filter `Aktif | Diarsipkan | Semua` serta action **Pulihkan**.

Jangan membuat hard-delete berantai dari kategori ke transaksi atau histori stok.

### P3 — maintainability dan coverage

- `ManagementScreens.kt` sekitar 2.095 baris, `ReportDashboardComponents.kt` sekitar 1.036 baris, `OperationsViewModel.kt` sekitar 750 baris, dan `PosRepository.kt` sekitar 888 baris. Pecah berdasarkan screen/use case secara bertahap.
- Banyak entity hanya memiliki index tanpa foreign key Room. Tambahkan FK pada schema baru secara selektif untuk mencegah orphan, dengan migration dan test yang aman.
- Migration instrumentation baru menguji jalur `1 -> 4`; belum ada test langsung `2 -> 4` dan `3 -> 4`.
- `ReportTrendRepositoryTest` hanya menguji tren kosong. Belum ada boundary periode, produk >5, varian, rename, atau overflow.
- CI membangun instrumentation APK tetapi tidak menjalankan test pada emulator.
- Onboarding mengatakan mode aktif “diingat saat dibuka kembali”, sementara session Owner selalu dimulai terkunci dan dikunci saat `onStop`. Salinan onboarding harus disesuaikan.
- Release variant di-minify, tetapi tidak tampak konfigurasi signing produksi. Artefak CI “release” tidak otomatis berarti siap Play Store.

## Rancangan Laporan yang lebih jelas

### Hirarki layar yang direkomendasikan

1. Judul **Laporan** dengan action kecil Refresh dan Export.
2. Filter periode tunggal: Hari, Minggu, Bulan, Tahun, atau rentang tanggal.
3. Kartu utama: **Omzet**, jumlah transaksi, kas masuk, kas keluar. Cantumkan “bukan laba”.
4. Bagian **Produk terlaris** yang langsung membandingkan produk.
5. Saat produk dipilih, buka **Detail produk** berisi varian, satuan, dan tren waktunya.
6. Bagian lanjutan yang bisa dilipat: metode pembayaran, arus kas, prediksi, utang/piutang.

### Kolom rekap produk

| Kolom | Tujuan |
|---|---|
| Produk | Identitas yang mudah dicari. |
| Varian | Tampil di detail; `Tanpa varian` bila kosong. |
| Terjual | `baseQuantity` plus label satuan dasar. |
| Omzet | Total `subtotal`. |
| Transaksi | `COUNT(DISTINCT saleId)`. |
| Harga rata-rata | `omzet / jumlah terjual`, diberi label agar bukan harga katalog. |
| Kontribusi | Persentase omzet produk terhadap omzet periode. |

Gunakan ID stabil untuk agregasi. Snapshot nama tetap dipertahankan untuk audit histori, tetapi nama saat ini dipakai sebagai label utama dan perubahan nama tidak boleh memecah satu produk.

### Aturan UX

- Tidak ada daftar yang dipotong diam-diam.
- Empty state menyebut filter aktif dan memberi langkah konkret.
- Semua angka pada satu layar memakai periode yang sama.
- “Semua produk” adalah rekap per produk, bukan satu garis agregat tanpa perbandingan.
- Tabel/list selalu tersedia sebagai alternatif grafik.
- Refresh menunjukkan waktu pembaruan terakhir; jangan memaksa pengguna menebak apakah data sudah baru.
- Filter tetap terlihat saat scroll dan tidak reset setelah membuka detail.

## Solusi sederhana: buku/foto → AI luar → CSV → CatatToko

### Prinsip

- Tidak ada model AI, API key, OCR SDK, server, atau biaya AI di APK.
- Pengguna bebas memakai AI eksternal yang sudah dimiliki.
- CatatToko hanya menyediakan **panduan, template, prompt, impor CSV, validasi, preview, dan konfirmasi**.
- Hasil AI tidak pernah langsung disimpan tanpa ditinjau pengguna.

Layanan AI umum memang dapat menerima gambar/file untuk dianalisis: [Gemini Apps](https://support.google.com/gemini/answer/14903178?co=GENIE.Platform%3DAndroid&hl=en), [ChatGPT image input](https://help.openai.com/articles/8400551-chatgpt-image-inputs-faq), dan [Claude Vision](https://platform.claude.com/docs/en/build-with-claude/vision). Namun tulisan tangan, foto miring, nilai buram, dan istilah lokal tetap dapat salah terbaca. Preview di CatatToko tetap wajib.

### Satu layar baru: “Impor catatan lama”

1. **Siapkan** — tombol `Unduh template CSV` dan `Salin prompt`.
2. **Ubah dengan AI pilihanmu** — panduan tiga kalimat untuk unggah foto, tempel prompt, lalu simpan hasil sebagai `.csv`.
3. **Masukkan ke CatatToko** — tombol `Pilih CSV`, lalu layar preview.

Tidak perlu login AI di CatatToko dan tidak perlu tombol yang memanggil API. Bila ingin membantu, APK cukup menyediakan shortcut “Buka aplikasi AI” melalui intent umum, tanpa integrasi akun.

### Template CSV MVP

```csv
external_id,tanggal_waktu,nomor_transaksi,nama_produk,varian,jumlah,satuan,harga_satuan,subtotal,metode_pembayaran,catatan,status_baca
1,2026-07-31 09:15,,Kopi Susu,,2,gelas,12000,24000,TUNAI,,OK
2,2026-07-31 09:15,,Roti Bakar,Cokelat,1,porsi,15000,15000,,,NEEDS_REVIEW
```

Aturan minimum:

- Satu baris untuk satu item terjual.
- Angka rupiah berupa integer tanpa `Rp`, titik ribuan, atau rumus.
- `subtotal` harus sama dengan `jumlah × harga_satuan`; selisih diblokir.
- Metode pembayaran: `TUNAI`, `QRIS`, `TRANSFER`, `PIUTANG`, atau kosong jika catatan tidak menyebutkannya.
- `status_baca=NEEDS_REVIEW` wajib dibetulkan pengguna.
- Beberapa item pada transaksi yang sama memakai `nomor_transaksi` yang sama. Jika tidak ada nomor, importer boleh mengelompokkan berdasarkan waktu hanya setelah konfirmasi.

### Prompt siap salin untuk AI eksternal

```text
Baca semua foto catatan penjualan yang saya unggah dan ubah menjadi CSV.

Aturan:
1. Keluarkan CSV saja dengan header persis:
external_id,tanggal_waktu,nomor_transaksi,nama_produk,varian,jumlah,satuan,harga_satuan,subtotal,metode_pembayaran,catatan,status_baca
2. Satu baris = satu item terjual.
3. Format tanggal: YYYY-MM-DD HH:mm. Jika jam tidak ada, gunakan 12:00 dan tulis "jam tidak tercatat" pada catatan.
4. Angka rupiah harus integer tanpa Rp atau pemisah ribuan.
5. Metode pembayaran hanya TUNAI, QRIS, TRANSFER, PIUTANG, atau kosong.
6. Jangan menebak tulisan yang buram. Kosongkan nilai yang tidak jelas dan isi status_baca dengan NEEDS_REVIEW.
7. Jika jelas, isi status_baca dengan OK.
8. Pertahankan urutan asli catatan. Jangan membuat transaksi atau produk yang tidak tertulis.
9. Bungkus teks yang mengandung koma dengan tanda kutip CSV.
```

### Preview yang harus tampil

- Ringkasan: jumlah baris, transaksi, rentang tanggal, total omzet, baris bermasalah, dan duplikat.
- Setiap baris menampilkan foto tidak perlu—cukup hasil CSV, status, dan alasan error.
- Nama OCR dipetakan ke produk CatatToko: **Cocok**, **Pilih produk**, atau **Buat produk baru**.
- Pengguna harus menyelesaikan semua `NEEDS_REVIEW` sebelum tombol `Impor` aktif.
- Konfirmasi terakhir harus berbunyi: “Histori ini menambah omzet laporan, tetapi tidak mengubah stok dan kas saat ini.”

### Cara menyimpan tanpa sistem rumit

MVP cukup menambahkan:

- `source = POS | IMPORT` pada transaksi.
- `importBatchId` nullable pada transaksi.
- tabel kecil `import_batches(id, fileHash, fileName, importedAt, rowCount)`.

Satu file diimpor dalam satu transaksi Room. `fileHash` mencegah file sama masuk dua kali. Jika pengguna membatalkan batch, lakukan reversal terkontrol untuk seluruh transaksi pada batch tersebut. Jangan mencoba menghitung ulang stok hari ini.

Jika metode pembayaran kosong, simpan sebagai `UNKNOWN/Tidak dicatat` khusus histori impor; opsi ini tidak boleh muncul pada layar checkout biasa.

### Validasi wajib

- Tanggal valid, tidak jauh di masa depan, dan timezone ditetapkan.
- `jumlah > 0`, harga/subtotal tidak negatif, dan operasi aritmetika memakai pemeriksaan overflow.
- Produk/varian yang tidak cocok harus dipetakan manual.
- Deteksi `external_id` ganda dan hash file ganda.
- Teks dari CSV diperlakukan sebagai data. Saat diekspor lagi ke Excel, awalan `=`, `+`, `-`, atau `@` harus diamankan dari formula injection.
- Batasi ukuran/baris dan tampilkan error per baris, bukan pesan generik.
- Tampilkan peringatan privasi: foto diproses oleh layanan AI luar yang dipilih pengguna, bukan oleh CatatToko.

## Rencana pengerjaan yang disarankan

### Tahap 0 — safety dan correctness

- Pertahankan/validasi PIN saat restore.
- Cocokkan identitas manifest dengan database hasil restore.
- Peringatan backup sensitif.
- Betulkan tier grosir lintas satuan.
- Ganti global `_busy` dengan job per use case.

### Tahap 1 — Laporan per produk yang benar

- Satu `ReportFilter` untuk seluruh layar dan export.
- Query rekap berbasis `productId`/`variantId`.
- Daftar semua produk yang searchable dan lazy.
- Drill-down varian/satuan.
- Auto-refresh berbasis perubahan data.
- Test boundary periode, rename, varian, produk >5, dan pergantian filter cepat.

### Tahap 2 — Impor catatan lama sederhana

- Template dan prompt siap salin.
- CSV parser ketat.
- Preview, pemetaan produk, validasi, duplikat, dan konfirmasi.
- Simpan sebagai histori `IMPORT` tanpa mutasi stok/kas.
- Test atomic rollback dan batch reversal.

### Tahap 3 — skala dan polish

- Lazy list/paging layar manajemen.
- Export flavor-aware dan typed XLSX streaming.
- Aksesibilitas grafik dan visual QA pada device.
- Jalankan instrumentation test di emulator CI.

## Test penerimaan wajib

| Skenario | Hasil yang diharapkan |
|---|---|
| Bulan dimulai di tengah minggu | Omzet kartu, grafik, produk, dan export menjumlah data yang sama. |
| Ada 30 produk | Semua bisa dicari; label “5 terlaris” hanya untuk ranking yang memang dibatasi. |
| Produk punya 4 varian | Rekap produk sama dengan jumlah keempat varian. |
| Produk pernah berganti nama | Tetap satu agregat berdasarkan ID. |
| User mengganti Hari → Bulan → Tahun cepat | UI terakhir menampilkan Tahun dan tidak pernah memasangkan label dengan data lama. |
| Grosir membeli pcs dan dus produk sama | Tier memakai total kuantitas dasar gabungan. |
| Restore backup dengan PIN berbeda | Pengguna tidak terkunci dan UI menjelaskan credential yang berlaku. |
| Impor file yang sama dua kali | Impor kedua diblokir sebagai duplikat. |
| CSV memiliki baris buram | Baris ditandai review dan tidak bisa disimpan diam-diam. |
| Impor histori selesai | Omzet laporan bertambah; stok, shift aktif, dan kas hari ini tidak berubah. |
| Impor gagal di tengah | Tidak ada sebagian baris tersimpan. |
| TalkBack membaca grafik | Pengguna dapat memperoleh tanggal dan nilai tanpa gesture presisi pada Canvas. |
| Hapus kategori kosong | Kategori hilang permanen setelah konfirmasi. |
| Hapus kategori berisi produk | Diblokir dengan penjelasan dan jumlah produk terdampak. |
| Arsipkan produk yang pernah terjual | Produk hilang dari kasir tetapi tetap muncul pada histori laporan. |
| Hapus produk yang belum pernah dipakai | Produk dan data konfigurasi turunannya terhapus atomik tanpa merusak katalog lain. |

## Definition of done untuk Laporan

- Angka summary, grafik, rekap produk, detail, dan export identik untuk filter yang sama.
- Semua produk dan varian dapat ditemukan.
- Tidak ada state label/data yang berbeda saat reload.
- Empty/loading/error state jelas dan tidak memakai enum internal.
- List tetap lancar pada histori besar.
- Test periode dan query agregat lulus di semua flavor.
- Screenshot live ponsel kecil dan besar diperiksa; TalkBack dan ukuran font besar diuji.

## Keputusan yang tidak disarankan

- Jangan pasang OCR/AI SDK, API key, atau server AI di CatatToko untuk fitur ini.
- Jangan otomatis memasukkan hasil AI tanpa preview.
- Jangan mengurangi stok hari ini dari catatan penjualan lama.
- Jangan menganggap backup checksum sama dengan enkripsi.
- Jangan menyelesaikan masalah laporan hanya dengan mengganti chart; sumber data dan periode harus dibetulkan lebih dulu.
