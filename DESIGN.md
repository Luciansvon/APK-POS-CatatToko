# Design System: Kasir Retail dan UMKM

## 1. Visual Theme & Atmosphere

A practical Android point-of-sale interface for Indonesian small-business owners and cashiers. Preserve the approved reference images closely: clean white canvas, strong Jade header, large transaction numbers, obvious status feedback, and large thumb-friendly controls.

- Density: `7/10` — operationally dense but never cramped.
- Variance: `2/10` — predictable, aligned, and consistent across repetitive cashier tasks.
- Motion: `2/10` — restrained feedback only; speed and clarity matter more than decoration.
- Base viewport: `360 x 800 dp`, portrait Android phone.
- The interface must remain understandable to users with little technical experience.
- Do not redesign the approved composition, hierarchy, or visual character.

## 2. Color Palette & Roles

- **Canvas White** (`#FBFCFB`) — primary screen background.
- **Pure Surface** (`#FFFFFF`) — cards, inputs, bottom action areas, and dialogs.
- **Jade Primary** (`#0B6B5E`) — single brand accent for headers, primary buttons, active steps, selected controls, focus rings, and important financial values.
- **Jade Deep** (`#075A4F`) — pressed and high-emphasis Jade state.
- **Jade Mist** (`#EAF5F1`) — selected rows, positive status backgrounds, and subtle icon circles.
- **Charcoal Ink** (`#171A1F`) — primary text and high-emphasis values; never use pure black.
- **Muted Graphite** (`#6B7280`) — secondary labels, descriptions, and inactive steps.
- **Whisper Border** (`#D8DEDB`) — one-pixel structural dividers and input outlines.
- **Warning Amber** (`#F59E0B`) — low-stock state only.
- **Warning Mist** (`#FFF4DD`) — low-stock background only.
- **Error Red** (`#D92D20`) — insufficient cash, destructive actions, and out-of-stock emphasis.
- **Error Mist** (`#FFF0F0`) — error container background.
- **Disabled Gray** (`#E7E9E8`) — disabled control fill.
- **Disabled Ink** (`#9CA3A1`) — disabled text and icons.

Jade is the only brand accent. Amber and red are semantic status colors, never decorative accents. No purple, neon blue, gradient text, or outer glow.

## 2.1 Identitas flavor dan aset awal aplikasi

- Retail dan UMKM memakai logo etalase hijau.
- Grosir dan Agen memakai logo etalase biru dengan simbol paket.
- Kuliner dan PKL memakai logo etalase jingga dengan simbol struk.
- Launcher memakai versi tanpa teks pada latar warna muda per flavor. Logo berada di safe zone tengah agar tidak terpotong mask bulat atau squircle Android.
- Layar pemuatan awal memakai versi logo bertulisan sesuai flavor dengan `ContentScale.Fit`; logo tidak boleh diregangkan, dipotong, atau dipakai untuk menambah jeda palsu.
- `master-logo.png` tidak dipakai sebagai pengganti identitas tiga flavor.

## 3. Typography Rules

- **Display and financial numbers:** `Roboto Condensed`, tabular figures when available, weight `700`.
- **Titles and labels:** `Roboto`, weight `600–700`.
- **Body and helper text:** `Roboto`, weight `400–500`, relaxed line height.
- **Fallback:** Android system sans-serif.
- Screen title: `24sp`, weight `700`.
- Primary section title: `20sp`, weight `700`.
- Product title: `17sp`, weight `700`.
- Body: `15–16sp`, minimum `14sp`.
- Secondary metadata: `13–14sp`.
- Product price: `19–20sp`, weight `700`.
- Transaction total and change: `36–48sp`, weight `700`, tabular figures.
- Button label: `16–18sp`, weight `700`.

Never use serif fonts, Inter, decorative display fonts, or thin text for financial values. Hierarchy comes from weight, spacing, and color rather than oversized marketing typography.

## 4. Component Stylings

### Header

- Solid Jade Primary background.
- White store icon, screen title, mode label, and `Buka Mode Owner`.
- Height approximately `72–80dp`.
- No gradient, glass effect, or decorative texture.

### Cashier flow context

- The current screen title and back action identify the active cashier flow.
- Do not render a four-stage step indicator; it consumes valuable vertical space on HP.
- Keep the header compact so search, categories, and products remain visible.

### Buttons

- Primary: Jade fill, white label, `12–16dp` corner radius, minimum `52dp` height.
- Secondary: white fill, Jade border, Jade label.
- Destructive: white or Error Mist surface with Error Red icon and label.
- Disabled: Disabled Gray fill and Disabled Ink label; no shadow.
- Active feedback: subtle `0.98` scale or one-pixel downward movement for `120ms`.
- No outer glow, pill buttons, or excessive shadows.

### Product grid

- Use a compact adaptive grid: two columns on a phone and up to three on wider tablet/content areas.
- Product photos use a rounded-square frame with `1:1` ratio; fallback category icons keep the active flavor theme.
- Product name, price, stock, and cart badge remain readable inside each card.
- Selected products use Jade Mist; low stock uses Amber; out of stock reduces emphasis and shows `Habis`.

### Quantity stepper

- Jade minus and plus segments with a white number field.
- Minimum `48dp` touch targets.
- Never rely on color alone; plus, minus, and numeric value remain visible.

### Inputs and search

- Label above the field.
- White surface with Whisper Border; Jade border for focus.
- `12–16dp` corner radius.
- Search has leading search icon and optional clear action.
- Error copy appears below or in a dedicated Error Mist block.
- No floating labels.

### Payment method selector

- Segmented container with only supported methods.
- Selected method uses Jade icon/text and Jade border.
- Unselected methods use Muted Graphite.
- MVP methods shown: `Tunai`, `QRIS`, and `Transfer`; `Piutang` only when the flavor permits it.
- Do not show E-Wallet or `Lainnya`.

### Status containers

- Success: Jade Mist, Jade icon, direct confirmation copy.
- Low stock: Warning Mist, Amber icon, direct label.
- Error: Error Mist, Error Red icon, title, exact missing amount, and recovery instruction.
- Every state combines icon, text, and color.

### Bottom action bar

- Sticky to the bottom but never covers scrollable content.
- Jade summary area with cart count and total.
- Clear white or Jade primary action depending on the screen.
- Respect Android gesture/navigation insets.

### Cards and dividers

- Use cards only for grouped financial summaries, success confirmation, or destructive warnings.
- Standard corner radius: `16dp`.
- Subtle background-tinted shadow only when hierarchy needs it.
- Product lists use thin dividers and negative space instead of separate floating cards.

### Loading and empty states

- Use skeleton rows that match product and transaction layout.
- No generic circular spinner as the only feedback.
- Empty cart and empty search states must explain the next useful action.

## 5. Layout Principles

- Use a `4dp` base spacing system.
- Page horizontal padding: `16dp`.
- Major section gap: `24–28dp`.
- Row vertical padding: `12–16dp`.
- Minimum touch target: `48 x 48dp`.
- Keep primary actions within easy thumb reach.
- Use a single-column vertical flow at all phone sizes.
- No horizontal scrolling, overlapping layers, floating decorative objects, or hidden primary actions.
- Sticky bottom bars must reserve matching content padding.
- Maintain the same component geometry across Retail, Grosir, and Kuliner; only capabilities, labels, products, and flavor color may change.

## 6. Motion & Interaction

- Motion is restrained and functional.
- Tap feedback: `120ms`, ease-out, transform and opacity only.
- Screen transition: `180ms` fade or short horizontal slide.
- Selected product row may fade into Jade Mist.
- Success check may use one short scale-in animation, then remain static.
- Error containers appear immediately without shake loops.
- Never use infinite animation, shimmer outside loading skeletons, bouncing indicators, confetti, or cinematic transitions.
- Respect reduced-motion preferences.

## 7. Content and Product Truth

- All primary copy is Indonesian and uses familiar business terms.
- `Bayar & Selesai` is the final cash transaction action.
- When cash is insufficient, display `Uang Kurang`, show the exact missing amount, hide negative change, and disable `Bayar & Selesai`.
- Offline copy must say that the transaction is stored locally on the device.
- Never promise synchronization, cloud upload, or sending data when those capabilities are not implemented.
- The cashier cannot access reports, operations, backup, or restore without Owner PIN verification.
- Retail does not show culinary order notes.
- Barcode scan remains hidden until its requirement is approved.

## 8. Anti-Patterns (Banned)

- No redesign away from the approved reference images.
- No emojis.
- No Inter or serif typography.
- No pure black.
- No purple, neon color, gradient text, or outer glow.
- No decorative dashboard cards that do not help the cashier act.
- No three-column marketing-card layout.
- No pill-shaped controls everywhere.
- No overlapping elements or clipped bottom actions.
- No tiny touch targets.
- No color-only status communication.
- No fake cloud or synchronization copy.
- No unapproved E-Wallet, `Lainnya`, barcode, printer, marketplace, or payment-gateway controls.
- No generic AI marketing copy.

## 9. Owner UI Direction — Stitch 2026-08-11

The approved cashier/worker UI is frozen. The existing cashier references and Compose flow remain the source of truth; no cashier redesign is part of this pass. Any cashier screens that were generated in Stitch during exploration are drafts only and must not be implemented over the approved cashier screens.

This pass redesigns the Owner area after Owner PIN verification: report reading, stock attention, cash overview, and backup/security. It keeps the offline-first product truth and shared Compose source.

The Owner report screen extends the same Retail Pragmatist system after Owner PIN verification. It is an operational reading surface, not a decorative dashboard.

- Keep the white header compact and show only the screen title; Owner access is already established by the surrounding shell.
- Put the period selector first, then the main sales summary, then the full-width `Simpan Laporan Excel` action.
- Keep `Omzet hari ini` or the active period label readable, but state that it is total sales value, not profit.
- Keep `Transaksi`, `Uang masuk`, and `Pengeluaran` visible without truncating long Rupiah values; switch to rows when the screen is narrow.
- Show `Pergerakan penjualan` with visible date/value labels and a semantic description; do not rely on color alone.
- Use `Lihat transaksi` as the next clear action, followed by deeper cash/detail sections only when opened.
- Show `Data tersimpan di perangkat.` and never imply cloud sync, upload, profit, or HPP calculation.
- On an empty period, use real zero values such as `Rp0`; do not invent example revenue, dates, or percentages.
- For Stock, Kas, and Backup, use one clear task per screen: flat sections, text tabs, divider-separated rows, and one primary action per state.

Stitch handoff used for this direction:

- Project: `CatatToko UI Redesign - Owner & Cashier 2026-08-11` (`5839994889813616259`).
- Final generated screen: `Laporan Owner (Revised Final)` (`203fd8ae97dd49199cb395c205e33e7a`).
- Generated design system asset: `a6dbd2c2601448e8b6ddbb9eabe82c9d`.
- Owner-only reference screens:
  - `Operasional - Stok Owner Final` (`27a28e73ec6845d5bb04bbaa5649cf5b`).
  - `Keuangan - Kas Owner (Empty State)` (`315cbf64dad04a12b093f39c116e228e`).
  - `Lainnya - Backup & Keamanan (Final)` (`60f6e98df53a4fc9b858c3f0233063a8`).

The earlier purple-frame, large-hero, card-heavy explorations were rejected as visual drafts. They are not acceptance references. These final references were checked for truthful empty states, readable bottom navigation, no fake dates or values, and no global stock action when only row-level stock actions are needed.

Acceptance criteria for Compose:

- At `360 x 800dp`, the period selector, summary, export action, and chart remain reachable by scrolling without overlap or clipped bottom navigation.
- Every primary action keeps at least a `48dp` touch target.
- The report remains Owner-only and continues to use the existing period, refresh, chart, Excel, and detail behaviors.
- Retail, Wholesale, and Culinary continue to use the shared screen structure; only capabilities, labels, and flavor colors vary.
