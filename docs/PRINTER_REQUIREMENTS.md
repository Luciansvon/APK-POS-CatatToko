# Catatan Sistem Printer CatatToko

Status: Masukan desain tahap berikutnya, belum diimplementasikan pada `0.5.0`.

## Tujuan

Membuat lapisan printer thermal universal berbasis ESC/POS. MP-58ECO/RPP02N menjadi perangkat uji pertama, bukan ketergantungan inti aplikasi.

```text
Transaksi tersimpan
        ↓
ReceiptData
        ↓
ReceiptFormatter per flavor
        ↓
ESC/POS Encoder
        ↓
PrinterManager
        ├── Bluetooth Classic
        ├── USB OTG
        └── LAN / TCP
```

## Batas transaksi yang wajib

- `Bayar & Selesai` menyimpan transaksi terlebih dahulu.
- Kegagalan printer tidak boleh membatalkan atau menggandakan transaksi.
- Setelah transaksi berhasil, layar struk menampilkan `Cetak Struk`, `Bagikan Struk`, dan `Transaksi Baru`.
- Jika cetak gagal, tampilkan status jelas serta aksi `Coba Lagi`; transaksi tetap berhasil.
- Cetak otomatis bersifat pengaturan opsional. Tombol cetak ulang tetap tersedia.
- Printer menerima data struk terstruktur, bukan screenshot UI Compose.
- Setiap output struk dapat memakai logo CatatToko sesuai flavor dari sumber `ReceiptData`/formatter yang sama.

## Model data dan abstraction yang disarankan

- Satu `ReceiptData` menjadi sumber preview layar, share PNG/PDF, dan ESC/POS.
- `PrinterConnection` menangani `connect`, `write`, `disconnect`, dan status koneksi.
- Implementasi transport dipisah menjadi Bluetooth, USB, dan TCP.
- `PrinterProfile` menyimpan transport, alamat, lebar kertas, karakter per baris, code page, serta capability cut/QR/image.
- Perintah auto cutter hanya dikirim jika profile printer menyatakan dukungan cutter.
- Logo thermal dikonversi menjadi bitmap monokrom ESC/POS dan diskalakan sesuai lebar cetak 58 mm atau 80 mm.
- Jika printer tidak mendukung image command, formatter memakai fallback judul teks `CatatToko` agar proses cetak tetap berhasil.
- Pengaturan menyediakan toggle `Cetak logo`; default final ditentukan setelah test kecepatan pada printer V1.

## Tahapan kandidat

### V1

- Bluetooth Classic RFCOMM/SPP;
- ESC/POS;
- pilihan printer yang sudah dipasangkan lewat Settings Android;
- kertas 58 mm dan 80 mm yang dapat dikonfigurasi;
- test print, cetak manual, cetak ulang, dan auto print;
- default awal yang diuji: MP-58ECO/RPP02N, 58 mm.

### Tahap lanjutan

- USB OTG;
- LAN/Wi-Fi TCP;
- QR, logo bitmap, barcode, cash drawer, auto cutter;
- printer dapur terpisah untuk flavor Kuliner.

## Permission dan keamanan

- Permission Bluetooth belum ditambahkan pada `0.5.0`.
- Implementasi Android 12+ perlu menilai `BLUETOOTH_CONNECT`; `BLUETOOTH_SCAN` hanya diperlukan bila aplikasi melakukan pencarian sendiri.
- Kandidat V1 memakai daftar perangkat yang sudah paired agar tidak menambah flow scanner kompleks.
- USB memerlukan permission perangkat dari pengguna sebelum komunikasi.
- Jangan log alamat printer lengkap, isi struk sensitif, atau data pelanggan yang tidak diperlukan.

## Keputusan yang masih perlu dikunci

- transport V1 final dan model printer uji yang benar-benar tersedia;
- lebar kertas default serta karakter per baris hasil test print;
- code page untuk karakter Indonesia;
- default auto print aktif/nonaktif;
- isi footer, logo, QR, dan format struk tiap flavor;
- ukuran/dithering logo thermal yang paling tajam serta batas waktu cetaknya pada 58 mm dan 80 mm;
- apakah printer dapur masuk MVP Kuliner;
- strategi retry, timeout, dan penyimpanan printer aktif.

## Referensi dari catatan user

- Rongta RPP02N product information: <https://www.rongtatech.cn/en/download/rpp02n-portable-receipt-printer-product-introduction-and-performance-features/>
- Android Bluetooth device API: <https://developer.android.com/reference/android/bluetooth/BluetoothDevice>
- Android Bluetooth permissions: <https://developer.android.com/develop/connectivity/bluetooth/bt-permissions>
- Android Bluetooth adapter API: <https://developer.android.com/reference/android/bluetooth/BluetoothAdapter>
- Android USB host overview: <https://developer.android.com/develop/connectivity/usb>
