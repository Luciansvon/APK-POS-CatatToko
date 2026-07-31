# Panduan Pengujian Emulator MuMuPlayer

Dokumen ini berisi panduan teknis dan perintah standar untuk menguji aplikasi **APK-POS-CatatToko** menggunakan emulator MuMuPlayer (Mode HP dan Tablet).

---

## 1. Persiapan Koneksi ADB
MuMuPlayer terhubung melalui port ADB `7555`.

```cmd
adb connect 127.0.0.1:7555
adb devices
```

---

## 2. Environment JDK
Gunakan **Microsoft OpenJDK 17** (`C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot`) untuk semua perintah Gradle.

---

## 3. Perintah Pengujian & Kompilasi

### A. Unit Tests (3 Flavor)
```cmd
cmd /c "set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot&& .\gradlew.bat testRetailDebugUnitTest testWholesaleDebugUnitTest testCulinaryDebugUnitTest"
```

### B. Build Debug APKs
```cmd
cmd /c "set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.8-hotspot&& .\gradlew.bat assembleDebug"
```

### C. Install & Buka Aplikasi

- **Varian Retail (Retail & UMKM)**:
  ```cmd
  cmd /c "adb connect 127.0.0.1:7555 && adb -s 127.0.0.1:7555 install -r app\build\outputs\apk\retail\debug\app-retail-debug.apk && adb -s 127.0.0.1:7555 shell am start -n com.bimacore.usahakecil.retail/com.bimacore.usahakecil.MainActivity"
  ```

- **Varian Grosir (Wholesale & Agen)**:
  ```cmd
  cmd /c "adb connect 127.0.0.1:7555 && adb -s 127.0.0.1:7555 install -r app\build\outputs\apk\wholesale\debug\app-wholesale-debug.apk && adb -s 127.0.0.1:7555 shell am start -n com.bimacore.usahakecil.wholesale/com.bimacore.usahakecil.MainActivity"
  ```

- **Varian Kuliner (Culinary & PKL)**:
  ```cmd
  cmd /c "adb connect 127.0.0.1:7555 && adb -s 127.0.0.1:7555 install -r app\build\outputs\apk\culinary\debug\app-culinary-debug.apk && adb -s 127.0.0.1:7555 shell am start -n com.bimacore.usahakecil.culinary/com.bimacore.usahakecil.MainActivity"
  ```

### D. Mengambil Screenshot UI
```cmd
cmd /c "adb connect 127.0.0.1:7555 && adb -s 127.0.0.1:7555 shell screencap -p /sdcard/screenshot_name.png && adb -s 127.0.0.1:7555 pull /sdcard/screenshot_name.png <destination_path>"
```

---

## 4. Aturan Penting MuMuPlayer
1. **Gunakan 1 Perintah per Varian**: Hindari menjalankan `connectedAndroidTest` 3 varian secara berurutan sekaligus dalam 1 perintah untuk mencegah bug `ERR-010` (crash Recent Tasks pada launcher MuMu).
2. **Cek Orientasi HP vs Tablet**:
   - Mode HP (1080x1920): Pastikan katalog produk tidak terpotong (menggunakan `fillMaxSize()`).
   - Mode Tablet (1600x900): Pastikan posisi tombol nominal tunai cepat dan `Bayar & Selesai` pas dan nyaman disentuh.
