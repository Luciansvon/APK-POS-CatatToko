# Stitch Master Prompt — Kasir Retail dan UMKM

Create a coherent seven-screen Android mobile point-of-sale flow at `360 x 800 dp`, using the attached reference images as the exact visual target. Preserve their composition, Jade-and-white palette, typography hierarchy, spacing rhythm, large financial numbers, product-row density, step indicator, and large thumb-friendly controls. This is a faithful reconstruction and cleanup, not a redesign.

Read and follow `DESIGN.md` as the design-system source of truth.

## Product context

- Indonesian offline-first POS app for Retail and UMKM.
- The app always starts in `Mode Kasir / Pekerja`.
- Cashiers can sell products, view stock on the cashier screen, and see the active transaction count.
- Owner-only areas require `Buka Mode Owner` and Owner PIN verification.
- Core operation must work without internet, account, cloud, or server.
- Use realistic Indonesian product names and Rupiah formatting.

## Generate these screens as one connected flow

1. **Mode Kasir / Pekerja**
   - Active transaction count, low-stock count, out-of-stock count.
   - Primary action `Mulai Transaksi`.
   - Secondary action `Lihat Stok`.
   - Owner-only reminder.

2. **Pilih Produk — default list**
   - Four-step progress indicator with step 1 active.
   - Search field without an active barcode action.
   - Category filters.
   - Product rows showing normal, selected, low-stock, and out-of-stock states.
   - Sticky cart summary and `Lanjut ke Keranjang`.

3. **Pilih Produk — search state**
   - Search query `gula`.
   - Filtered realistic product results.
   - Quantity stepper for the selected product.
   - Low-stock and out-of-stock states.

4. **Keranjang**
   - Step 2 active; step 1 completed.
   - Three Retail products with image, name, price, quantity stepper, and delete action.
   - `Hapus Semua` is visually destructive and must require confirmation in behavior.
   - No culinary order-note field.
   - Subtotal and total.
   - Sticky `Lanjut ke Pembayaran`.

5. **Pembayaran — cash sufficient**
   - Step 3 active.
   - Total `Rp97.500`.
   - Supported methods only: `Tunai`, `QRIS`, `Transfer`.
   - Cash received `Rp120.000`.
   - Quick amount controls including `Uang Pas`.
   - Change `Rp22.500`.
   - Positive copy: `Uang cukup. Transaksi bisa diselesaikan.`
   - Enabled primary action `Bayar & Selesai`.

6. **Pembayaran — cash insufficient**
   - Same payment composition and step 3.
   - Cash received `Rp50.000`.
   - Error block titled `Uang Kurang`.
   - Exact shortfall `Rp47.500`.
   - Recovery copy: `Minta tambahan uang atau ubah metode pembayaran.`
   - Do not show negative change.
   - Disabled `Bayar & Selesai`.

7. **Transaksi Berhasil**
   - Step 4 active.
   - Header title `Selesai`.
   - Large success state.
   - Change `Rp22.500` is the strongest content.
   - Total `Rp97.500`, cash received `Rp120.000`, transaction number `TRX-0726-018`.
   - Offline copy: `Transaksi disimpan di perangkat.`
   - Actions `Bagikan Struk` and `Transaksi Baru`.

## Critical constraints

- Do not add E-Wallet, `Lainnya`, cloud sync, or online-delivery copy.
- Do not show a barcode control.
- Do not show a Retail order-note field.
- Do not change the approved visual style.
- No gradients, purple, neon, glassmorphism, marketing copy, or decorative dashboard clutter.
- Use icons plus text for every warning, error, success, and disabled state.
- Keep every touch target at least `48dp`.
- Ensure the sticky bottom action area never covers content or Android navigation insets.
