# Stitch Prompt — Owner Reports Overview

Project: `5839994889813616259`

Create a mobile Android screen for CatatToko, an offline-first POS for an Indonesian small-business owner after Owner PIN unlock. Use a practical retail visual language: light Canvas White background, white surfaces, solid Jade `#0B6B5E` header and primary actions, Charcoal Ink text, muted graphite metadata, amber only for low stock, and red only for errors or insufficient cash. Use Roboto-style sans-serif text, condensed bold numerals for Rupiah, a 4dp spacing rhythm, 16dp side margins, 48dp minimum touch targets, and a compact operational layout at 360x800dp.

Screen priority:

- compact Jade top bar with `Laporan` and a visible `Mode Owner` label;
- period selector `Hari ini` with a readable date range;
- primary summary `Omzet penjualan` with a large Rupiah value and the note `Total nilai penjualan, bukan laba.`;
- metrics for `Transaksi`, `Uang masuk`, and `Pengeluaran` that keep long values untruncated;
- full-width action `Simpan Laporan Excel`;
- section `Pergerakan penjualan` with an accessible line chart and visible date/value labels;
- secondary action `Lihat transaksi`;
- footer note `Data tersimpan di perangkat.`.

Use realistic Indonesian labels but do not invent business numbers; render `Rp0` when data is empty. Do not claim cloud sync, profit, or HPP. Avoid purple, neon, gradients, glass, glow, oversized empty header space, three-column marketing cards, tiny text, fake percentages, and clipped navigation. Keep the result feasible for Jetpack Compose Material 3 and responsive for a wider tablet.

The generated screen is visual direction only. Compose remains the source of behavior and must preserve Owner PIN protection, period refresh, chart detail, Excel export, and offline storage truth.
