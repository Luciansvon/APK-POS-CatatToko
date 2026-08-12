# Prompt Konversi Catatan Fisik CatatToko v1

Status: Kontrak importer v1 yang diterapkan mulai APK `0.6.0`.

## Cara pakai

1. Buka Gemini, ChatGPT, atau aplikasi AI lain yang dapat membaca gambar.
2. Unggah foto catatan fisik.
3. Salin seluruh isi bagian `PROMPT MULAI` sampai `PROMPT SELESAI`.
4. Unduh file JSON yang dibuat AI. Jika AI tidak dapat membuat lampiran, salin hanya blok JSON pada bagian `HASIL CATATTOKO`.
5. Masukkan hasil tersebut lewat `CatatToko > Lainnya > Import catatan lama` untuk divalidasi dan diperiksa Owner.

## Prompt siap salin

### PROMPT MULAI

Anda adalah pengonversi catatan usaha fisik ke format draf impor CatatToko. Baca hanya informasi yang benar-benar terlihat pada gambar yang saya lampirkan.

ATURAN WAJIB:

1. Jawaban harus mempunyai tepat dua bagian berurutan: `HASIL CATATTOKO` dan `PANDUAN SELANJUTNYA`.
2. Pada `HASIL CATATTOKO`, buat file bernama `CatatToko-Impor-Catatan-YYYY-MM-DD.json` jika kemampuan aplikasi mendukung lampiran. Jika tidak, tampilkan tepat satu blok kode JSON yang dapat disalin. Isi file/blok wajib berupa satu objek JSON valid tanpa komentar.
3. Gunakan `schemaVersion` tepat `catattoko.history-import.v1`.
4. Jangan mengarang tanggal, waktu, nama, produk, jumlah, satuan, harga, total, metode pembayaran, atau jenis catatan.
5. Jika suatu nilai tidak terlihat, gunakan `null`. Jika sebagian terbaca tetapi meragukan, isi nilai terbaik yang terlihat dan tambahkan nama field ke `uncertainFields`.
6. Nominal Rupiah harus berupa bilangan bulat tanpa `Rp`, titik, koma, atau desimal. Contoh: `12500`.
7. Jumlah barang harus bilangan bulat positif. Jangan mengubah lusin, pak, dus, atau satuan lain menjadi pcs jika faktor konversinya tidak tertulis.
8. Tanggal memakai `YYYY-MM-DD`. Waktu memakai `HH:mm`. Jika tahun, tanggal, atau waktu tidak terlihat lengkap, gunakan `null` dan tandai field terkait sebagai ragu.
9. Metode pembayaran hanya boleh `CASH`, `QRIS`, `TRANSFER`, `CREDIT`, atau `null`. Jangan menganggap tunai jika tidak tertulis.
10. Jenis record hanya boleh `SALE`, `PURCHASE`, `CASH_IN`, `CASH_OUT`, `EXPENSE`, `RECEIVABLE`, `PAYABLE`, `STOCK_ADJUSTMENT`, atau `UNRESOLVED`.
11. Satu baris catatan tidak boleh disalin menjadi lebih dari satu record kecuali sumber jelas menunjukkan dua kejadian berbeda.
12. `SALE` dan `PURCHASE` boleh memiliki `items`. Jangan membuat nama produk seperti `Produk lain-lain` untuk mengejar total.
13. Jika sumber hanya menulis total penjualan harian tanpa rincian barang, gunakan `UNRESOLVED`, simpan total pada `amount`, dan jelaskan `Total penjualan tanpa rincian barang` pada `note`. Jangan membuat transaksi atau produk palsu.
14. Jangan hitung atau isi nilai yang tidak tertulis kecuali penjumlahan item dapat dibuktikan. Bila hasil hitung berbeda dari total tertulis, pertahankan total tertulis, tandai `amount`, dan tulis selisih pada `warnings`.
15. Salin potongan teks sumber yang relevan ke `rawText` agar Owner dapat membandingkan hasil dengan foto.
16. Setiap record wajib mempunyai `sourceRef` unik seperti `halaman-1-baris-3`.

Gunakan struktur ini dan pertahankan semua nama field. Field yang tidak relevan tetap harus ada dengan nilai `null` atau array kosong.

{
  "schemaVersion": "catattoko.history-import.v1",
  "source": {
    "title": null,
    "pageCount": 0,
    "businessType": null,
    "timezone": "Asia/Jakarta"
  },
  "records": [
    {
      "sourceRef": "halaman-1-baris-1",
      "type": "UNRESOLVED",
      "date": null,
      "time": null,
      "partyName": null,
      "category": null,
      "paymentMethod": null,
      "amount": null,
      "amountPaid": null,
      "items": [],
      "stockDelta": null,
      "note": null,
      "rawText": "",
      "uncertainFields": [],
      "warnings": []
    }
  ],
  "summary": {
    "recordCount": 0,
    "readyCount": 0,
    "needsReviewCount": 0,
    "dateFrom": null,
    "dateTo": null,
    "warnings": []
  }
}

Struktur setiap item di dalam `items`:

{
  "productName": null,
  "variantName": null,
  "quantity": null,
  "unitLabel": null,
  "unitPrice": null,
  "subtotal": null,
  "uncertainFields": []
}

KETENTUAN PER JENIS:

- `SALE`: isi item penjualan yang terlihat, `amount` sebagai total, `amountPaid` sebagai uang yang benar-benar dibayar jika tertulis, dan `partyName` jika pelanggan tertulis.
- `PURCHASE`: isi item pembelian, `amount` sebagai total tagihan, `amountPaid` jika tertulis, dan `partyName` sebagai pemasok.
- `CASH_IN`, `CASH_OUT`, `EXPENSE`: `amount` wajib berasal dari catatan; `category` dan `note` hanya diisi jika terlihat atau dapat disalin langsung.
- `RECEIVABLE`: `partyName` adalah pelanggan, `amount` nilai awal piutang, dan `amountPaid` pembayaran yang sudah terjadi jika tertulis.
- `PAYABLE`: `partyName` adalah pemasok/pihak pemberi utang, `amount` nilai awal utang, dan `amountPaid` pembayaran yang sudah terjadi jika tertulis.
- `STOCK_ADJUSTMENT`: gunakan tepat satu item, isi `stockDelta` dengan bilangan positif untuk penambahan atau negatif untuk pengurangan hanya jika arah perubahan jelas.
- `UNRESOLVED`: gunakan saat jenis, rincian, atau hubungan antarangka belum cukup jelas. Jangan memaksa record menjadi jenis lain.

Atur `summary.readyCount` sebagai jumlah record tanpa `uncertainFields` dan tanpa `warnings`. Record lainnya dihitung ke `needsReviewCount`. Urutkan record sesuai urutan pada catatan, bukan berdasarkan perkiraan waktu.

Periksa ulang JSON sebelum menjawab: harus dapat diparse, tidak boleh ada trailing comma, semua string memakai tanda kutip ganda, dan tidak boleh ada teks penjelasan di dalam file/blok JSON.

Setelah file/blok selesai, tulis bagian `PANDUAN SELANJUTNYA` dalam bahasa Indonesia yang singkat dan mudah diikuti. Panduan wajib:

1. menyebut jumlah seluruh record, jumlah siap, jumlah perlu dicek, serta rentang tanggal dari `summary`;
2. menyebut `sourceRef` yang perlu dicek dan alasan utamanya;
3. meminta pengguna mengunduh file JSON atau menyalin hanya blok JSON, bukan seluruh jawaban;
4. mengarahkan pengguna ke `CatatToko > Lainnya > Import catatan lama`, lalu memilih `Pilih file` atau menempel JSON;
5. mengingatkan agar semua baris dibandingkan dengan foto sebelum menekan `Masukkan catatan siap`;
6. melarang memasukkan file ini lewat menu `Backup & pemulihan` karena file impor bukan backup database;
7. mengingatkan agar file yang sama tidak diimpor dua kali;
8. jika menu `Import catatan lama` belum tersedia pada APK pengguna, mengatakan dengan jujur: `Simpan file ini dulu. Versi CatatToko Anda belum menyediakan menu import catatan lama.` Jangan mengklaim data sudah masuk aplikasi;
9. tidak meminta API key dan tidak mengarahkan pengguna membeli layanan tertentu.

### PROMPT SELESAI

## Keputusan format

- Format ini adalah draf pertukaran data, bukan backup database.
- Nama atau ID internal database tidak pernah diminta dari AI.
- CatatToko tetap harus memvalidasi ulang seluruh enum, nominal, jumlah, tanggal, subtotal, total, duplikasi, serta pemetaan produk/pihak.
- Record `UNRESOLVED` tidak boleh langsung masuk histori.
- Seluruh record, termasuk yang terlihat siap, tetap membutuhkan review dan konfirmasi Owner.
- Teks `PANDUAN SELANJUTNYA` hanya membantu pengguna dan tidak ikut ditempel atau diimpor ke CatatToko.
