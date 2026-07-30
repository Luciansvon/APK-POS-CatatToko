# Offline Operations Suite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Menyelesaikan fungsi operasional offline yang sudah disetujui untuk APK Retail, Grosir, dan Kuliner dari satu shared core.

**Architecture:** Room tetap menjadi source of truth lokal. Aturan bisnis, repository, laporan, dan backup berada di shared source; `BusinessCapabilities` mengatur fitur yang terlihat dan berlaku pada setiap flavor. Seluruh operasi finansial dan stok yang saling terkait memakai transaksi database atomik serta snapshot histori.

**Tech Stack:** Kotlin 2.3.0, Jetpack Compose BOM 2026.06.00, Room 2.8.4, Android Gradle Plugin 8.13.2, Gradle 8.14.3, JUnit 4, AndroidX instrumented tests.

**Status akhir:** Selesai pada 30 Juli 2026. Seluruh unit test, build tiga flavor, build androidTest, full connected suite Retail, connected smoke Wholesale/Culinary, audit manifest offline, dokumentasi, kenaikan versi, dan packaging APK telah dilakukan. Bukti rinci ada di `docs/WORKLOG.md` dan `docs/RELEASE_NOTES.md`.

---

## Aturan eksekusi

- Setiap behavior baru dimulai dari test yang gagal.
- Jangan lanjut ke implementasi sebelum kegagalan test sesuai dengan behavior yang belum ada.
- Setelah satu irisan hijau, jalankan unit test seluruh flavor.
- Jangan menjalankan connected test tanpa target perangkat yang dikonfirmasi user.
- Jangan menimpa APK distribusi sebelum seluruh verifikasi dan dokumentasi rilis selesai.
- `docs/ERROR_SOLUTIONS.md` hanya diperbarui untuk bug nyata yang ditemukan selama eksekusi.

### Task 1: Toolchain dan baseline

**Files:**
- Modify jika diperlukan: `gradle.properties`
- Verify: seluruh source dan test saat ini

- [ ] Unduh atau temukan JDK 17+ yang dapat dipakai tanpa mengubah instalasi sistem user.
- [ ] Jalankan tiga unit-test flavor dan catat hasil baseline.
- [ ] Jalankan `assembleDebug` dan tiga androidTest build.
- [ ] Jika baseline gagal, buktikan root cause dan catat di `docs/ERROR_SOLUTIONS.md`.

Commands:

```powershell
.\gradlew.bat testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRetailDebugAndroidTest assembleWholesaleDebugAndroidTest assembleCulinaryDebugAndroidTest
```

Expected: seluruh command keluar dengan `BUILD SUCCESSFUL`.

### Task 2: Konfigurasi kemampuan dan shell navigasi

**Files:**
- Create: `app/src/main/java/com/bimacore/usahakecil/domain/BusinessCapabilities.kt`
- Create: `app/src/test/java/com/bimacore/usahakecil/domain/BusinessCapabilitiesTest.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/ui/AppDestination.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/ui/HomeScreen.kt`
- Modify: `app/src/main/java/com/bimacore/usahakecil/ui/PosApp.kt`

- [ ] Tulis test bahwa Retail, Wholesale, dan Culinary hanya mengaktifkan fitur pada matriks spesifikasi.
- [ ] Jalankan test dan pastikan gagal karena `BusinessCapabilities` belum ada.
- [ ] Implementasikan mapping capability murni tanpa dependensi Android.
- [ ] Jalankan test sampai lulus.
- [ ] Tambahkan navigasi `Kasir`, `Operasional`, `Keuangan`, `Laporan`, dan `Lainnya`.
- [ ] Pastikan flow kasir lama tetap dapat dicapai tanpa login.

Expected assertions:

```kotlin
assertTrue(BusinessCapabilities.forType(BusinessType.WHOLESALE).multiUnit)
assertFalse(BusinessCapabilities.forType(BusinessType.RETAIL).multiUnit)
assertTrue(BusinessCapabilities.forType(BusinessType.CULINARY).culinaryOrders)
assertFalse(BusinessCapabilities.forType(BusinessType.CULINARY).customerReceivables)
```

### Task 3: Skema data operasional dan migrasi

**Files:**
- Create: `app/src/main/java/com/bimacore/usahakecil/data/OperationalEntities.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/data/OperationalDaos.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/data/DatabaseMigrations.kt`
- Modify: `app/src/main/java/com/bimacore/usahakecil/data/Entities.kt`
- Modify: `app/src/main/java/com/bimacore/usahakecil/data/Daos.kt`
- Modify: `app/src/main/java/com/bimacore/usahakecil/data/PosDatabase.kt`
- Test: `app/src/androidTest/java/com/bimacore/usahakecil/data/DatabaseMigrationTest.kt`

- [ ] Tulis migration test versi 1 menuju skema final dan isi data versi 1.
- [ ] Pastikan test gagal karena migrasi belum tersedia.
- [ ] Tambahkan kolom snapshot kompatibel pada penjualan dan stock movement.
- [ ] Tambahkan tabel profil, pihak terkait, unit/harga, pembelian, kas, utang-piutang, tenaga kerja, resep, topping, status pesanan, pengaturan keamanan, dan metadata backup.
- [ ] Implementasikan migrasi eksplisit tanpa destructive fallback.
- [ ] Verifikasi data penjualan versi 1 tetap terbaca setelah migrasi.

Expected migration invariant:

```kotlin
assertEquals("INV-LAMA", migratedSale.receiptNumber)
assertEquals(25_000L, migratedSale.total)
assertEquals(1, migratedProduct.stock)
```

### Task 4: Produk, satuan, harga, dan stok

**Files:**
- Create: `app/src/main/java/com/bimacore/usahakecil/domain/InventoryRules.kt`
- Create: `app/src/test/java/com/bimacore/usahakecil/domain/InventoryRulesTest.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/data/InventoryRepository.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/ui/InventoryViewModel.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/ui/InventoryScreen.kt`
- Modify: `app/src/main/java/com/bimacore/usahakecil/data/PosRepository.kt`

- [ ] Tulis test konversi satuan, harga bertingkat, batas jumlah, alasan penyesuaian, dan larangan stok negatif.
- [ ] Pastikan test gagal sebelum implementasi.
- [ ] Implementasikan aturan domain murni.
- [ ] Implementasikan CRUD kategori/produk/varian tanpa menghapus histori.
- [ ] Implementasikan stok masuk, stok keluar, rusak, hilang, dan penyesuaian dengan alasan.
- [ ] Implementasikan unit dasar, pak/dus, faktor konversi, minimum quantity, dan harga bertingkat khusus Wholesale.
- [ ] Buat layar daftar/form/riwayat yang fungsional.
- [ ] Jalankan seluruh unit test flavor.

Expected unit conversion:

```kotlin
assertEquals(24, InventoryRules.toBaseQuantity(quantity = 2, factor = 12))
assertEquals(90_000L, InventoryRules.resolveTierPrice(basePrice = 10_000L, baseQuantity = 10, tiers = listOf(PriceTier(10, 9_000L))))
```

### Task 5: Supplier, pembelian, kas, utang, dan piutang

**Files:**
- Create: `app/src/main/java/com/bimacore/usahakecil/domain/LedgerRules.kt`
- Create: `app/src/test/java/com/bimacore/usahakecil/domain/LedgerRulesTest.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/data/OperationsRepository.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/ui/OperationsViewModel.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/ui/PurchasesScreen.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/ui/FinanceScreen.kt`
- Modify: `app/src/main/java/com/bimacore/usahakecil/data/PosRepository.kt`

- [ ] Tulis test total pembelian, sisa utang/piutang, pembayaran berlebih, serta status lunas.
- [ ] Pastikan test gagal.
- [ ] Implementasikan supplier/pelanggan dan pembelian atomik dengan stok.
- [ ] Implementasikan kas masuk/keluar dan pengeluaran.
- [ ] Implementasikan histori pembayaran utang/piutang.
- [ ] Hubungkan penjualan dengan catatan penerimaan kas.
- [ ] Aktifkan piutang penjualan hanya pada flavor yang mengizinkan.
- [ ] Buat layar daftar, detail, form, dan pembayaran.

Expected debt invariant:

```kotlin
assertEquals(40_000L, LedgerRules.remaining(100_000L, listOf(25_000L, 35_000L)))
assertFailsWith<IllegalArgumentException> {
    LedgerRules.remaining(100_000L, listOf(110_000L))
}
```

### Task 6: PIN dan laporan

**Files:**
- Create: `app/src/main/java/com/bimacore/usahakecil/security/PinHasher.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/security/ReportSession.kt`
- Create: `app/src/test/java/com/bimacore/usahakecil/security/PinHasherTest.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/data/ReportRepository.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/ui/ReportsViewModel.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/ui/ReportsScreen.kt`
- Modify: `app/src/main/java/com/bimacore/usahakecil/MainActivity.kt`

- [ ] Tulis test hash memakai salt, PIN salah, sesi terkunci, dan repository menolak read tanpa sesi.
- [ ] Pastikan test gagal.
- [ ] Implementasikan penyimpanan hash PIN lokal.
- [ ] Implementasikan sesi in-memory yang dikunci saat app background atau keluar laporan.
- [ ] Implementasikan query transaksi, omzet, metode pembayaran, kas, pengeluaran, utang, dan piutang.
- [ ] Buat flow buat PIN, buka laporan, PIN salah, laporan, detail, ganti PIN, dan kunci.
- [ ] Pastikan tidak ada laporan laba karena HPP belum dipilih.

Expected security invariant:

```kotlin
assertNotEquals("123456", stored.pinHash)
assertFalse(session.isUnlocked)
assertFailsWith<ReportLockedException> { reports.readSummary() }
```

### Task 7: Tenaga kerja

**Files:**
- Create: `app/src/main/java/com/bimacore/usahakecil/domain/WorkforceRules.kt`
- Create: `app/src/test/java/com/bimacore/usahakecil/domain/WorkforceRulesTest.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/data/WorkforceRepository.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/ui/WorkforceViewModel.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/ui/WorkforceScreen.kt`

- [ ] Tulis test hadir, setengah hari, bonus, kasbon, potongan, freelancer, dan snapshot tarif.
- [ ] Pastikan test gagal.
- [ ] Implementasikan pekerja harian dan freelancer tanpa login pekerja.
- [ ] Implementasikan riwayat tarif bertanggal.
- [ ] Implementasikan kehadiran/tugas dan pembayaran bertahap.
- [ ] Buat layar daftar, form, pencatatan, dan histori pembayaran.

Expected daily pay:

```kotlin
assertEquals(
    135_000L,
    WorkforceRules.dailyPay(rate = 100_000L, attendance = HALF_DAY, overtime = 25_000L, bonus = 70_000L, deduction = 10_000L),
)
```

### Task 8: Backup dan restore

**Files:**
- Create: `app/src/main/java/com/bimacore/usahakecil/backup/BackupManifest.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/backup/BackupManager.kt`
- Create: `app/src/test/java/com/bimacore/usahakecil/backup/BackupManifestTest.kt`
- Create: `app/src/androidTest/java/com/bimacore/usahakecil/backup/BackupRestoreTest.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/ui/BackupRestoreScreen.kt`
- Modify: `app/src/main/res/xml/file_paths.xml`
- Modify: `app/src/main/java/com/bimacore/usahakecil/PosApplication.kt`

- [ ] Tulis test manifest berversi dan penolakan hash salah.
- [ ] Pastikan test gagal.
- [ ] Implementasikan checkpoint dan paket backup berisi database serta manifest.
- [ ] Implementasikan preview metadata sebelum restore.
- [ ] Implementasikan backup pengaman, replace atomik, pemeriksaan integritas, dan rollback.
- [ ] Implementasikan berbagi backup tanpa izin penyimpanan luas.
- [ ] Uji backup, file rusak, restore, dan rollback pada data pengujian.

Expected integrity:

```kotlin
assertTrue(BackupManifest.verify(databaseBytes, manifest.sha256))
assertFalse(BackupManifest.verify(databaseBytes + 1, manifest.sha256))
```

### Task 9: Fitur Kuliner

**Files:**
- Create: `app/src/main/java/com/bimacore/usahakecil/domain/CulinaryRules.kt`
- Create: `app/src/test/java/com/bimacore/usahakecil/domain/CulinaryRulesTest.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/data/CulinaryRepository.kt`
- Create: `app/src/main/java/com/bimacore/usahakecil/ui/CulinaryScreen.kt`
- Modify: `app/src/main/java/com/bimacore/usahakecil/data/PosRepository.kt`
- Modify: `app/src/main/java/com/bimacore/usahakecil/ui/CartScreen.kt`

- [ ] Tulis test topping, catatan, transisi status, resep, dan stok bahan.
- [ ] Pastikan test gagal.
- [ ] Implementasikan topping dan catatan sebagai snapshot item penjualan.
- [ ] Implementasikan status `BARU -> DIPROSES -> SIAP -> SELESAI`.
- [ ] Implementasikan resep serta pengurangan bahan atomik saat transaksi selesai.
- [ ] Buat layar antrean pesanan dan pengelolaan resep.
- [ ] Pastikan fitur tidak muncul pada Retail/Wholesale.

Expected status rules:

```kotlin
assertTrue(CulinaryRules.canMove(NEW, PROCESSING))
assertFalse(CulinaryRules.canMove(READY, NEW))
```

### Task 10: Integrasi, dokumentasi, versi, dan paket APK

**Files:**
- Modify: `README.md`
- Modify: `docs/ARCHITECTURE.md`
- Modify: `docs/WORKLOG.md`
- Modify: `docs/ERROR_SOLUTIONS.md` untuk bug nyata
- Modify: `docs/RELEASE_NOTES.md`
- Modify: `docs/UI_UX_REQUIREMENTS.md`
- Modify: `app/build.gradle.kts`
- Run: `scripts/package-apks.ps1`

- [ ] Jalankan seluruh unit test tiga flavor.
- [ ] Build tiga APK debug.
- [ ] Build tiga APK androidTest.
- [ ] Lakukan smoke test non-connected yang tersedia dan catat batas perangkat.
- [ ] Pastikan manifest tidak meminta `INTERNET`.
- [ ] Audit matriks fitur tiap flavor.
- [ ] Tambahkan catatan error yang benar-benar ditemukan beserta root cause dan bukti.
- [ ] Tambahkan worklog lengkap.
- [ ] Naikkan `versionCode` dan `versionName`.
- [ ] Tambahkan release notes sebelum menimpa APK.
- [ ] Jalankan packaging dan periksa tiga file APK terbaru.

Final commands:

```powershell
.\gradlew.bat testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRetailDebugAndroidTest assembleWholesaleDebugAndroidTest assembleCulinaryDebugAndroidTest
.\scripts\package-apks.ps1
```

Expected:

```text
BUILD SUCCESSFUL
dist/debug/Kasir-Retail-UMKM.apk
dist/debug/Kasir-Grosir-Agen.apk
dist/debug/Kasir-Kuliner-PKL.apk
```
