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
