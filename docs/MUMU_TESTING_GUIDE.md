# Panduan Pengujian Emulator MuMuPlayer

Baca dokumen ini sebelum menjalankan connected test. Flow ini sudah dipakai dan lulus untuk dua device MuMu dan tiga flavor pada 2026-08-01.

## Aturan agent

- Jangan menebak serial device. Jalankan `adb devices -l` dan gunakan serial yang benar-benar muncul.
- Dua device boleh diuji paralel oleh satu Gradle task. Jangan menjalankan test yang sama dari dua agent karena ADB/emulator bisa saling berebut.
- Split sub-agent hanya untuk pekerjaan independen setelah test, misalnya audit screenshot satu device per agent.
- Connected test hanya boleh dijalankan setelah user mengonfirmasi device/emulator.
- Jalankan satu flavor per perintah sebagai default. Ini menghindari masalah MuMu `RecentTasks` (`ERR-010`). Gunakan `--continue` bila memang perlu melanjutkan flavor berikutnya setelah flavor pertama gagal.
- Jangan menganggap screenshot setelah connected test sebagai screenshot aplikasi; runner meng-uninstall APK saat cleanup.

## 1. Cek ADB dan profil device

```powershell
adb devices -l
```

Jika status `offline`:

```powershell
adb reconnect offline
adb devices -l
```

Jangan lanjut sebelum status berubah menjadi `device`. Bila MuMu hanya muncul sebagai TCP device, sambungkan dulu memakai alamat yang ditampilkan MuMu, misalnya `adb connect 127.0.0.1:7555`.

Profil yang sudah terbukti pada workspace ini:

| Serial | Model | Resolusi efektif | Orientasi |
|---|---|---:|---|
| `emulator-5554` | ASUS AI2205 | `1080x1920` | portrait |
| `emulator-5556` | ALT AL10 | `1600x900` | landscape; physical `900x1600` |

Verifikasi bila profil berubah:

```powershell
adb -s emulator-5554 shell wm size
adb -s emulator-5554 shell wm density
adb -s emulator-5556 shell wm size
adb -s emulator-5556 shell wm density
```

Gunakan Microsoft OpenJDK 17 atau JDK 17+ yang sudah dipakai workspace. Wrapper Gradle adalah sumber perintah utama.

## 2. Unit test dan build

```powershell
.\gradlew.bat testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRetailDebugAndroidTest assembleWholesaleDebugAndroidTest assembleCulinaryDebugAndroidTest
```

## 3. Connected test seluruh flavor

Dengan dua serial aktif, setiap perintah di bawah otomatis menjalankan test pada kedua device:

```powershell
.\gradlew.bat connectedRetailDebugAndroidTest --console=plain
.\gradlew.bat connectedWholesaleDebugAndroidTest --console=plain
.\gradlew.bat connectedCulinaryDebugAndroidTest --console=plain
```

Jika Retail gagal, tetap jalankan Wholesale dan Culinary dengan perintah terpisah. Jangan menyimpulkan flavor berikutnya ikut gagal hanya karena Gradle berhenti pada task pertama.

Report HTML:

```text
app/build/reports/androidTests/connected/debug/flavors/retail/index.html
app/build/reports/androidTests/connected/debug/flavors/wholesale/index.html
app/build/reports/androidTests/connected/debug/flavors/culinary/index.html
```

## 4. Test Owner lengkap

Test `owner_mode_covers_all_relevant_screens_and_locks_again` mencakup:

- buka PIN Owner;
- destination Operasional, Keuangan, Laporan, dan Lainnya;
- seluruh tab relevan Produk, Stok, Pembelian, Pekerja, Grosir/Kuliner;
- shift dan catatan kas;
- utang/piutang, transaksi;
- laporan, forecast, dialog ganti PIN;
- profil, backup lokal, dan penguncian kembali Mode Owner.

Catatan coverage: test otomatis memverifikasi layar dan kontrol Owner yang aman untuk dijalankan tanpa mengubah data bisnis. Dialog `Ganti PIN`, pembuatan backup, dan restore tetap harus diaudit melalui visual/manual smoke setelah APK dipasang ulang; jangan mengisi PIN baru atau menjalankan restore pada data penting.

Jalankan satu flavor per perintah. Argumen wajib diapit tanda kutip di PowerShell karena `#` dianggap komentar:

```powershell
.\gradlew.bat connectedRetailDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.bimacore.usahakecil.MainActivitySmokeTest#owner_mode_covers_all_relevant_screens_and_locks_again' --console=plain
.\gradlew.bat connectedWholesaleDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.bimacore.usahakecil.MainActivitySmokeTest#owner_mode_covers_all_relevant_screens_and_locks_again' --console=plain
.\gradlew.bat connectedCulinaryDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.bimacore.usahakecil.MainActivitySmokeTest#owner_mode_covers_all_relevant_screens_and_locks_again' --console=plain
```

PIN yang dipakai test adalah fixture instrumentation di `MainActivitySmokeTest.kt`; jangan menjadikannya credential fallback production.

Bukti terakhir: perintah Owner di atas lulus `6/6` run pada 2026-08-01 (Retail, Wholesale, Culinary; `emulator-5554` dan `emulator-5556`).

## 5. Install dan buka APK untuk visual QA

Connected runner melakukan uninstall setelah test. Install ulang flavor yang ingin di-screenshot:

```powershell
adb -s emulator-5554 install -r -d -g "app\build\outputs\apk\retail\debug\app-retail-debug.apk"
adb -s emulator-5554 shell am start -W -n com.bimacore.usahakecil.retail/com.bimacore.usahakecil.MainActivity
```

Mapping package:

| Flavor | APK | Package |
|---|---|---|
| Retail | `app-retail-debug.apk` | `com.bimacore.usahakecil.retail` |
| Wholesale | `app-wholesale-debug.apk` | `com.bimacore.usahakecil.wholesale` |
| Culinary | `app-culinary-debug.apk` | `com.bimacore.usahakecil.culinary` |

Untuk tiga flavor, ulangi install dan `am start` dengan package masing-masing pada kedua serial.

Setelah membuka layar Owner dan melakukan backup lokal, snackbar `Backup siap dibagikan` dapat menutup tombol bawah pada tablet landscape. Sebelum screenshot atau menekan `Keluar Mode Owner`, scroll ke teks `Offline-first` agar posisi kontrol tidak tertutup snackbar.

Untuk aksi manual Owner seperti membuka dialog `Ganti PIN`, membuat backup, atau memilih file restore, jangan menebak koordinat lintas device. Dump hierarki UI sebelum tap, cari teks atau `resource-id` target, lalu tap titik tengah `bounds`; setelah layar berubah, dump ulang:

```powershell
adb -s emulator-5554 shell uiautomator dump /sdcard/window.xml
adb -s emulator-5554 shell cat /sdcard/window.xml
```

Ulangi dengan serial `emulator-5556` untuk tablet. Jika target berada di dalam scroll, scroll berdasarkan teks target lalu dump ulang. Setelah dialog atau file picker terbuka, gunakan dump terbaru; bounds dari portrait tidak boleh dipakai untuk landscape.

## 6. Screenshot yang valid

Jangan memakai redirection PowerShell langsung karena binary PNG dapat berubah menjadi UTF-16. Gunakan `cmd /c`:

```powershell
cmd /c "adb -s emulator-5554 exec-out screencap -p > C:\temp\mumu-retail-5554.png"
cmd /c "adb -s emulator-5556 exec-out screencap -p > C:\temp\mumu-retail-5556.png"
```

Validasi header PNG:

```powershell
$bytes = Get-Content -LiteralPath "C:\temp\mumu-retail-5554.png" -Encoding Byte -TotalCount 8
$bytes -join ','
```

Hasil valid harus dimulai dengan `137,80,78,71,13,10,26,10`.

Ambil screenshot minimal untuk setiap kombinasi flavor-device. Simpan bukti di folder run, bukan di source project, misalnya `C:\Users\shint\.codex\visualizations\YYYY\MM\DD\<run-id>`.

## 7. Audit vision

- Buka screenshot valid dengan vision dan cek status layar, clipping, overlap, kontras, scroll, serta ukuran target sentuh.
- Bila memakai sub-agent, bagi satu device per agent dan kirim semua screenshot flavor device tersebut.
- Jangan mengulang connected test dari sub-agent; test sudah dijalankan Gradle pada dua serial.
- Catat perbedaan portrait `1080x1920` dan tablet landscape `1600x900`; jangan menganggap hasil HP mewakili tablet.

Bukti audit terakhir (2026-08-01): kontrol `Muat ulang`, `Ganti PIN`, dan `Kunci Mode Owner` lulus visual di kedua device; tidak wrap, overlap, atau clipping. Label `Operasional` sudah diperbaiki agar tetap satu baris di ASUS portrait. Temuan visual yang masih tercatat: kontras ikon/jam status bar terlalu rendah pada latar terang.

## 8. Troubleshooting dan bukti

- Compose timeout bukan otomatis berarti aplikasi crash. Baca XML/logcat test dan cek `mCurrentFocus`/`mFocusedApp` sebelum menyimpulkan.
- Screenshot setelah runner selesai biasanya menunjukkan launcher karena APK sudah di-uninstall; itu bukan bukti UI aplikasi. Install dan buka APK lagi sebelum visual QA.
- Jika MuMu system server crash atau device kembali `offline`, tunggu proses selesai, jalankan `adb reconnect offline`, lalu ulangi satu flavor saja.
- Simpan report HTML, XML, logcat, device profile, dan path screenshot pada laporan akhir.
