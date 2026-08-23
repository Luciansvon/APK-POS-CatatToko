# Design QA - Owner / Operasional Bento Overview

## Comparison target

- Source visual truth: `C:\Users\shint\Downloads\ChatGPT Image 23 Agu 2026, 09.15.43.png`
- Source pixels: `841 x 1870`
- Runtime device: MuMu `emulator-5554`, Android 12, physical `1080 x 1920`, density `480`
- Runtime viewport: approximately `360 x 640 dp`
- Implementation: native Jetpack Compose `OwnerOperationsOverview`
- Runtime top screenshot: `C:\Users\shint\OneDrive\Dokumen\MuMuSharedFolder\Screenshots\MuMu-20260823-104520-769.png`
- Runtime lower screenshot: `C:\Users\shint\OneDrive\Dokumen\MuMuSharedFolder\Screenshots\MuMu-20260823-104618-704.png`
- State: Retail Owner mode, `Operasional` selected, protected zero-value daily summary

## Full-view comparison evidence

The reference and final native MuMu screenshot were opened together in one comparison input. The implementation matches the reference hierarchy and proportions: header, summary heading, asymmetric top bento, amber stock card, four shortcuts, report link, backup link, and five-item bottom navigation.

The source has a taller aspect ratio than the confirmed MuMu portrait device. The final implementation therefore keeps the reference sizing and scrolls the lower cards on the `360 x 640 dp` runtime instead of shrinking text and touch targets. Top and lower native screenshots cover the complete surface.

## Focused-region comparison evidence

- Header: title, subtitle, divider, and outline Owner icon align with the reference hierarchy.
- Metrics: protected `Rp0`, transaction count, and cash-in values render in the intended bento proportions.
- Stock: amber treatment and closest Material cube icon render consistently; the runtime message reflects seeded device data (`2 produk perlu diisi ulang`) instead of the zero-alert source state.
- Shortcuts: Produk, Pembelian, Kas, and Pekerja retain equal card widths and large touch targets.
- Links: Lihat laporan keeps the chevron; Backup & Keamanan uses a shield/storage icon and no chevron, matching the source.
- Navigation: Operasional uses the clipboard icon and visible green top indicator; Reports uses the document icon.

## Resolved findings

- [P1] Owner metrics originally bypassed the protected report read boundary.
  - Resolution: daily metrics now come from `ReportRepository.readSummary(...)` and clear when Owner locks.
- [P1] The Retail `Kas` shortcut originally opened the default `Utang & Piutang` tab.
  - Resolution: the shortcut overrides Finance to tab `0`, while direct bottom-navigation keeps each flavor default.
- [P1] Connected Compose click coverage was unstable after scrolling.
  - Resolution: the test uses bounded waits plus the semantics `OnClick` action for scrollable bento targets.
- [P2] Several icons and the selected navigation treatment visibly differed from the reference.
  - Resolution: BarChart, ReceiptLong, AccountBalanceWallet, PersonOutline, Assignment, Description, Group, shield/storage, and a foreground green selection line are used.
- [P2] Archived variants could affect the stock-warning count.
  - Resolution: inactive variants are excluded.

## Verification

- Unit tests: `testRetailDebugUnitTest`, `testWholesaleDebugUnitTest`, and `testCulinaryDebugUnitTest` passed; 234 tests, 0 failures.
- Builds: all three debug APKs and all three AndroidTest APKs built successfully.
- Connected test: `owner_overview_shortcuts_route_to_existing_destinations` passed on `emulator-5554` after verifying Kas opens the Kas content and the report, backup, and product routes remain reachable.
- Runtime: final Retail APK installed with replace mode and left installed on MuMu.
- Crash buffer: empty after final launch and interaction pass.
- Visual QA: native MuMu PNGs inspected directly; Computer Use was not used for the final evidence.

## Accepted runtime differences

- Source zero-alert copy differs from the emulator because the emulator contains two seeded low/out-of-stock products.
- The confirmed MuMu viewport is shorter than the reference aspect ratio, so the lower cards require scrolling. This preserves readable type and touch sizes.

prior overview result: passed

## Implementation status — 2026-08-23

Shared `CatatToko` owner header and bento tokens are now applied to all non-Kasir Owner destinations and shared report/history/forecast components across the three flavors. Cashier screens remain out of scope. This records implementation status only; final visual verdict remains an audit decision by root.

## Final owner-wide audit — 2026-08-23

- Scope audit: modified production UI files are limited to Owner/shared management surfaces; the Kasir landing, catalog, cart, payment, and receipt files are untouched.
- Branding audit: `CatatTokoOwnerHeader` is used by Operasional, Keuangan, Laporan, Lainnya, and Import catatan lama. The shared header renders the CatatToko icon, `CatatToko`, and the current page title.
- Density audit: shared outer padding is `16.dp`, group spacing is `12.dp`, cards size to their content, and `48.dp` minimum height is reserved for touch targets rather than decorative whitespace.
- Retail visual audit: Produk, Stok, Pembelian, Pekerja, Kas, Transaksi, Laporan, Lainnya, and Import catatan lama were inspected from native MuMu screenshots.
- Cross-flavor visual audit: the shared bento overview was inspected in Wholesale/Grosir and Culinary/Pesanan; CatatToko branding is consistent while each flavor keeps its own color and navigation label.
- Automated verification: 234 unit tests passed with zero failures; all three debug APKs and all three AndroidTest APKs built successfully.
- Connected verification: `owner_mode_covers_all_relevant_screens_and_locks_again` passed independently for Retail, Wholesale, and Culinary on `emulator-5554`.
- Runtime verification: all three final debug APKs were installed with replace mode and left installed. Retail was returned to the foreground. The emulator crash buffer has no `com.bimacore.usahakecil` entries; older unrelated TikTok crash entries remain in the shared buffer.
- Packaging audit: Retail, Wholesale, and Culinary `0.7.2` (`versionCode 24`) were packaged under the fixed `dist/debug/CatatToko-*.apk` names, verified as Android Debug signed, installed with `adb install -r`, and reported the expected flavor-suffixed version names through `dumpsys package`.
- Native screenshot evidence:
  - Retail Laporan: `C:\Users\shint\OneDrive\Dokumen\MuMuSharedFolder\Screenshots\MuMu-20260823-145715-942.png`
  - Retail Produk: `C:\Users\shint\OneDrive\Dokumen\MuMuSharedFolder\Screenshots\MuMu-20260823-145915-510.png`
  - Retail Stok/Pembelian/Pekerja: `MuMu-20260823-150108-839.png`, `MuMu-20260823-150110-290.png`, `MuMu-20260823-150111-767.png`
  - Retail Kas/Transaksi: `MuMu-20260823-150200-588.png`, `MuMu-20260823-150202-045.png`
  - Retail Import catatan lama: `MuMu-20260823-150232-360.png`
  - Wholesale/Grosir: `MuMu-20260823-150359-546.png`
  - Culinary/Pesanan: `MuMu-20260823-150526-936.png`

final result: passed
