# Handoff Antigravity: Fondasi Forecasting Penjualan CatatToko

Tanggal: 2026-08-01
Target repository: `Luciansvon/APK-POS-CatatToko`
Baseline yang diperiksa: `main` commit `3fce274168274584e816f249f9d2239eaaf5d488`
Versi baseline: `0.3.2`, `versionCode 7`

## Ringkasan

Patch ini menambahkan fondasi forecasting offline dalam shared core. Mesin memilih model berdasarkan rolling-origin backtesting dan menyediakan moving average, simple exponential smoothing, Holt linear, Holt-Winters additive, serta Croston-SBA.

Patch belum menghubungkan engine ke Room, ViewModel, navigation, atau Compose. Pemisahan ini sengaja dilakukan agar algoritma dapat direview dan dites tanpa mengubah transaksi, schema, stok, atau UI yang sudah aktif.

## File yang harus ditambahkan

1. `app/src/main/java/com/bimacore/usahakecil/domain/forecast/SalesForecastEngine.kt`
2. `app/src/test/java/com/bimacore/usahakecil/domain/forecast/SalesForecastEngineTest.kt`
3. `docs/superpowers/specs/2026-08-01-sales-forecasting-foundation.md`
4. `docs/handoffs/ANTIGRAVITY_FORECASTING_FOUNDATION_2026-08-01.md`

File tersedia juga dalam arsip dan patch yang menyertai handoff ini.

## Yang sudah diverifikasi

| Check | Status | Bukti |
|---|---|---|
| Kompilasi engine dengan `kotlinc` | PASS | Tidak ada error compiler |
| Kompilasi source + test | PASS | Tidak ada error compiler |
| Unit scenario runner lokal | PASS | 8/8 skenario lulus |
| Gradle test seluruh flavor | NOT RUN | Repository penuh tidak tersedia di environment |
| Android lint | NOT RUN | Repository penuh tidak tersedia di environment |
| `assembleDebug` | NOT RUN | Repository penuh tidak tersedia di environment |
| Connected test | NOT RUN | Tidak ada checkout repo dan target emulator |
| Visual QA | NOT RUN | Belum ada UI forecasting |

Jangan mengubah `NOT RUN` menjadi `PASS` berdasarkan inspeksi source.

## Tugas Antigravity

### 1. Terapkan patch di branch baru

Gunakan branch terpisah, contoh:

```text
antigravity/forecasting-foundation-0.3.3
```

Jangan menulis langsung ke `main`.

### 2. Jalankan verifikasi fondasi

```powershell
.\gradlew.bat testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest
.\gradlew.bat lintRetailDebug lintWholesaleDebug lintCulinaryDebug
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRetailDebugAndroidTest assembleWholesaleDebugAndroidTest assembleCulinaryDebugAndroidTest
```

Connected test hanya dijalankan setelah target emulator/perangkat dikonfirmasi.

### 3. Audit source sebelum integrasi data

Telusuri alur aktual:

```text
Sale/Transaction entity
→ sale item snapshot
→ DAO laporan atau transaksi
→ repository laporan
→ ViewModel Owner
→ Compose report screen
```

Pastikan query memakai transaksi penjualan yang sah dan kuantitas stok dasar. Jangan menebak field, DAO, atau status transaksi.

### 4. Tambahkan adapter histori

Buat adapter yang menghasilkan:

```kotlin
List<DailySales>
```

Aturan:

- satu target forecast per produk atau per varian;
- Grosir memakai base quantity;
- tanggal tanpa penjualan akan diisi nol oleh engine;
- duplicate date aman dijumlahkan engine;
- nilai negatif dilarang sampai kontrak retur/void disetujui;
- proses pada dispatcher background;
- akses hanya dari sesi Owner.

### 5. Tambahkan UI secara terpisah

Jangan gabungkan integrasi data, redesign laporan besar, HPP, Excel, dan forecasting ke satu commit raksasa. Urutan yang aman:

1. engine + tests;
2. DAO/query histori + tests;
3. repository/use case + tests;
4. ViewModel state;
5. Compose screen dan empty/error state;
6. visual QA HP/tablet;
7. export setelah kontrak anggaran dikunci.

### 6. Perbarui dokumentasi repo

Wajib append ke `docs/WORKLOG.md` dengan status aktual. Karena ini fitur, bukan bugfix, `docs/ERROR_SOLUTIONS.md` tidak perlu ditambah kecuali ditemukan dan diperbaiki bug.

Tambahkan bagian arsitektur setelah integrasi benar-benar aktif. Jangan menulis bahwa forecasting sudah tersedia di UI selama patch masih domain-only.

Snippet worklog untuk fondasi:

```markdown
---

## 2026-08-01 - Fondasi forecasting penjualan tervalidasi

Status: Diimplementasikan pada domain shared core; integrasi Room dan UI belum dilakukan.

### Hasil

- menambahkan moving average, simple exponential smoothing, Holt linear, Holt-Winters additive, dan Croston-SBA;
- memilih kandidat melalui rolling-origin one-step backtesting;
- mencatat MAE, RMSE, sMAPE, WAPE, dan bias;
- menambahkan validasi histori, normalisasi tanggal kosong, serta batas komputasi;
- menambahkan unit test untuk pola stabil, tren, musiman, intermittent, data nol, gap, duplikat, input negatif, dan histori kurang.

### Verifikasi aktual

- isi dengan command dan hasil Gradle yang benar-benar dijalankan;
- connected test dan visual QA belum diperlukan sampai integrasi UI dibuat.

### File

- `app/src/main/java/com/bimacore/usahakecil/domain/forecast/SalesForecastEngine.kt`
- `app/src/test/java/com/bimacore/usahakecil/domain/forecast/SalesForecastEngineTest.kt`
- `docs/superpowers/specs/2026-08-01-sales-forecasting-foundation.md`
```

## Acceptance criteria fondasi

- seluruh test tiga flavor lulus;
- lint tiga flavor lulus;
- build tiga flavor lulus;
- tidak ada dependency baru;
- tidak ada perubahan schema Room;
- tidak ada permission internet;
- tidak ada perubahan HPP, kas, stok, atau transaksi;
- seluruh forecast non-negative dan finite;
- kandidat dibandingkan pada rentang backtesting yang sama;
- data nol tidak menghasilkan NaN atau infinity;
- model dan parameter terpilih dapat dilihat untuk audit.

## Keputusan yang masih diblokir

1. Metode HPP: belum disetujui, jangan implementasikan diam-diam.
2. Perlakuan void/refund/return pada histori: belum ada kontrak final.
3. Target forecast UI: produk, varian, kategori, atau gabungan perlu dipilih berdasarkan kebutuhan layar.
4. Zona waktu bisnis: perlu ditetapkan agar grouping per hari tetap konsisten saat timezone perangkat berubah.
5. Pembulatan rekomendasi stok: expected quantity boleh pecahan, aturan pembulatan milik fitur perencanaan.
6. Anggaran omzet: harus membedakan harga historis, harga aktif, dan harga rencana.

## Risiko audit

- Grid parameter cukup ringan untuk satu produk, tetapi jangan menjalankannya untuk seluruh katalog pada setiap recomposition.
- Holt-Winters dapat menang pada data intermittent yang juga mempunyai pola mingguan. Itu bukan bug jika backtesting memang lebih baik. Croston adalah kandidat, bukan model wajib.
- Histori transaksi yang belum bisa dikoreksi akan membawa kesalahan lama ke forecast. Grafik tidak menyucikan data buruk, meskipun manusia sering berharap demikian.
- Model terbaik dapat berubah ketika data baru masuk. Simpan nama model, parameter, rentang data, dan metrik bersama hasil bila nanti dibuat cache.

## PR yang disarankan

Judul:

```text
feat(analytics): add validated offline sales forecasting foundation
```

Body ringkas:

```markdown
## Ringkasan

- menambahkan engine forecasting pure Kotlin pada shared core;
- mendukung moving average, SES, Holt, Holt-Winters additive, dan Croston-SBA;
- memilih model melalui rolling-origin backtesting;
- menambahkan MAE, RMSE, sMAPE, WAPE, bias, validasi input, dan 8 unit scenario;
- belum mengubah Room, UI, HPP, stok, atau transaksi.

## Verifikasi

- [ ] unit test Retail
- [ ] unit test Wholesale
- [ ] unit test Culinary
- [ ] lint tiga flavor
- [ ] assembleDebug
- [ ] compile androidTest tiga flavor

## Batas

Integrasi histori transaksi dan UI Owner dibuat dalam PR terpisah setelah schema serta timezone bisnis diverifikasi.
```
