# Design QA - Retail Cashier Stitch

final result: blocked

## Source visual

- `docs/design-references/retail-cashier-approved-2026-07-30/`
- Stitch project: `https://stitch.withgoogle.com/projects/4168463593621581978`

## Implemented states

- Mode Kasir/Pekerja landing.
- Product list and search state.
- Cart.
- Cash payment with sufficient and insufficient money states.
- Successful transaction and change.

## Automated verification

- Unit tests for Retail, Wholesale, and Culinary passed.
- Debug builds for Retail, Wholesale, and Culinary passed.
- Android test APK compilation for Retail, Wholesale, and Culinary passed.

## Blocking verification

No emulator or physical Android target was confirmed by the user. Connected tests,
runtime screenshots, and same-viewport visual comparison against the seven approved
references were therefore not run. Do not mark visual fidelity as passed until those
captures are compared and P0-P2 mismatches are fixed.
