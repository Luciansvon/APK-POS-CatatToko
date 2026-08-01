# Handoff Antigravity — Implementasi UI Kasir Stitch

> Dokumen ini adalah handoff historis untuk redesign Stitch. Status aktif repository berada di branch `antigravity/forecasting-foundation-0.3.3`; gunakan `docs/WORKLOG.md`, `docs/handoffs/ANTIGRAVITY_FORECASTING_FOUNDATION_2026-08-01.md`, dan `docs/RELEASE_NOTES.md` sebagai rujukan terbaru.

## Tujuan

Implementasikan tujuh layar kasir Retail dari desain Stitch ke aplikasi Android native Jetpack Compose tanpa mengubah behavior transaksi, keamanan Owner, histori stok, atau fungsi offline.

User sudah memberi persetujuan coding dengan instruksi `langsung buat aja`.

## Status saat handoff

- Implementasi Compose sudah dimulai pada branch `codex/retail-cashier-stitch-redesign`.
- Landing Mode Kasir/Pekerja, alur empat langkah, katalog vertikal, keranjang, pembayaran, dan hasil transaksi sudah terhubung.
- Unit test tiga flavor, build tiga APK debug, dan compile tiga APK androidTest sudah lulus.
- Visual QA aktual dan connected test belum dijalankan karena target emulator/perangkat belum dikonfirmasi user.
- Dokumentasi, referensi visual, sistem desain, prompt, dan hasil HTML Stitch sudah tersedia.
- Worktree mempunyai perubahan dokumentasi yang harus dipertahankan.
- Jangan menghapus atau menimpa file dirty yang ada.
- Jangan membuat tiga codebase flavor terpisah.

## Sumber kebenaran

Urutan prioritas:

1. Tujuh layar di project Stitch:
   - https://stitch.withgoogle.com/projects/4168463593621581978
2. `DESIGN.md`
3. `docs/superpowers/specs/2026-07-30-retail-cashier-visual-design.md`
4. PNG referensi:
   - `docs/design-references/retail-cashier-approved-2026-07-30/`
5. HTML hasil generasi Stitch yang sudah diekstrak:
   - `docs/design-audits/stitch-retail-2026-07-30/01-mode-kasir.html`
   - `docs/design-audits/stitch-retail-2026-07-30/02-pilih-produk.html`
   - `docs/design-audits/stitch-retail-2026-07-30/03-cari-produk.html`
   - `docs/design-audits/stitch-retail-2026-07-30/04-keranjang.html`
   - `docs/design-audits/stitch-retail-2026-07-30/05-pembayaran-cukup.html`
   - `docs/design-audits/stitch-retail-2026-07-30/06-pembayaran-kurang.html`
   - `docs/design-audits/stitch-retail-2026-07-30/07-transaksi-berhasil.html`
6. Prompt Stitch:
   - `docs/stitch/retail-cashier-master-prompt.md`

HTML Stitch hanya acuan visual. Jangan memasukkan WebView atau HTML ke APK.

## Tujuh layar yang harus dibuat

1. Mode Kasir/Pekerja.
2. Pilih Produk — daftar default.
3. Pilih Produk — pencarian `gula`.
4. Keranjang.
5. Pembayaran tunai — uang cukup.
6. Pembayaran tunai — uang kurang.
7. Transaksi berhasil.

## Koreksi wajib terhadap hasil Stitch

Pertahankan komposisi dan gaya visual, tetapi:

- hapus barcode dan copy `scan barcode`; requirement barcode belum disetujui;
- jangan tampilkan E-Wallet atau `Lainnya`;
- jangan menjanjikan sinkronisasi/cloud;
- gunakan `Transaksi disimpan di perangkat`;
- jangan tampilkan catatan pesanan pada Retail;
- tombol akhir tetap bernama `Bayar & Selesai`;
- uang kurang menampilkan selisih, tidak menampilkan kembalian negatif, dan menonaktifkan tombol;
- `Hapus Semua` wajib memakai konfirmasi;
- area pengelolaan tetap membutuhkan PIN Owner.

## File Compose utama

- `app/src/main/java/com/bimacore/usahakecil/ui/theme/Theme.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/HomeScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/PosApp.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/PosViewModel.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/CatalogScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/CartScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/PaymentScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/ReceiptScreen.kt`

Disarankan menambah:

- `app/src/main/java/com/bimacore/usahakecil/ui/CashierLandingScreen.kt`
- `app/src/main/java/com/bimacore/usahakecil/ui/CashierComponents.kt`

## Arsitektur implementasi

- Tambahkan layar landing kasir sebagai state awal tanpa mengubah area Owner.
- Pertimbangkan `PosScreen.CASHIER_HOME` sebagai initial state.
- `Mulai Transaksi` membuka katalog.
- `Lihat Stok` membuka katalog dengan fokus/filter stok, bukan area operasional Owner.
- Setelah `Transaksi Baru`, kembali ke landing kasir sesuai desain.
- Gunakan shared component untuk header Jade, progress empat tahap, product row, quantity stepper, status card, dan sticky bottom action.
- Retail menjadi acuan struktur. Wholesale dan Culinary mempertahankan warna serta kemampuan flavor masing-masing.
- Jangan menghapus dukungan varian, multi-satuan Grosir, topping Kuliner, QRIS, Transfer, Piutang, atau kalkulator yang sudah ada.

## Data landing kasir

Desain meminta:

- jumlah transaksi hari ini;
- jumlah produk stok menipis;
- jumlah produk stok habis.

Data stok dapat dihitung dari `CatalogSnapshot`.

Transaksi hari ini harus berasal dari data nyata, bukan angka contoh `12`. `SaleDao` sudah mempunyai `observeSales()` dan `getSalesBetween(...)`. Buat aliran/read-model yang aman untuk kasir tanpa membuka omzet atau laporan.

## TDD

Tulis test gagal terlebih dahulu untuk kontrak yang bisa diuji tanpa screenshot:

- state awal adalah landing kasir;
- `Mulai Transaksi` membuka katalog;
- `Transaksi Baru` kembali ke landing;
- perhitungan stok menipis/habis benar;
- uang kurang tetap menonaktifkan penyelesaian;
- copy offline tidak menyebut sinkronisasi;
- metode pembayaran tidak menambah E-Wallet atau `Lainnya`;
- akses Owner tetap dilindungi.

Tambahkan test tag seperlunya tanpa melemahkan test lama.

## Verification wajib

Jalankan:

```powershell
.\gradlew.bat testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRetailDebugAndroidTest assembleWholesaleDebugAndroidTest assembleCulinaryDebugAndroidTest
```

Connected test hanya setelah user mengonfirmasi emulator atau HP:

```powershell
.\gradlew.bat connectedRetailDebugAndroidTest
```

Lakukan smoke test aktual:

```text
Mode Kasir -> Pilih Produk -> Keranjang -> Pembayaran cukup/kurang -> Selesai -> Transaksi Baru
```

Periksa tiga flavor agar shared UI tidak membocorkan fitur khusus.

## Dokumentasi dan rilis

- Perbarui `docs/WORKLOG.md` setelah implementasi dan bukti tes tersedia.
- Perbarui `docs/UI_UX_REQUIREMENTS.md`.
- Redesign bukan bugfix; jangan membuat entri palsu di `docs/ERROR_SOLUTIONS.md`.
- Jika APK distribusi akan ditimpa:
  - naikkan `versionCode` dan `versionName`;
  - perbarui `docs/RELEASE_NOTES.md`;
  - test dan build harus lulus;
  - jalankan `scripts/package-apks.ps1`.

## Larangan

- Jangan memakai WebView.
- Jangan copy HTML/Tailwind Stitch ke source Android.
- Jangan menambah internet permission.
- Jangan menambah cloud, sync, barcode, E-Wallet, printer, marketplace, atau payment gateway.
- Jangan memindahkan source MAUCAFE.
- Jangan menghapus behavior lama demi mencocokkan screenshot.
- Jangan menyatakan selesai hanya karena build hijau; lakukan smoke test flow.
