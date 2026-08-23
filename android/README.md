# Ledger (Android)

Native Android port of the Ledger personal-budget app (the web version lives in
the parent folder). Kotlin + Jetpack Compose + DataStore + Firebase. Data stays
on-device by default; cloud sync is opt-in, and backups use the exact same JSON format
as the web app so you can move freely between platforms.

## Build & run

1. Open this folder in **Android Studio** (Ladybug or newer) and let it sync.
2. Plug in a device (or start an emulator) and press Run.
3. CLI alternative:
   ```sh
   ./gradlew assembleDebug
   # APK at app/build/outputs/apk/debug/app-debug.apk
   ```

Requirements: **JDK 17+**, Android SDK (API 35), and **Kotlin 2.3.0**.
The full liquid-glass shaders (blur / refraction / vibrancy / chromatic aberration)
require **Android API 33+** (AGSL); on API 26–32 the glass gracefully degrades to a
basic blur.

## What's ported

Everything that matters for budgeting, ported 1:1 from the web app's logic:

- Setup (budget, period, start date, bank balance, currency)
- Daily allowance, rollover, bank balance + banked leftovers, streaks,
  top-ups / money moves (budget · balance · return · withdraw)
- Log-a-spend card with quick amounts, frequent suggestions, single-category
  selection, edit/duplicate/delete with undo
- Category breakdown: period/7d/month/all/custom ranges, donut chart,
  per-category bars with optional budgets, insights
- Spending trend: line chart (per-category series, allowance baseline,
  tap-to-inspect) and GitHub-style heatmap (30/90/365d)
- History: search, category filters, date range, 4 sort modes, date grouping
- Automations: recurring spending / top-ups / balance rules with backfill
- Piggy bank: goal, deposits, break (moves back to balance)
- Data & backup: JSON backup (web-compatible format), CSV export, restore
- Cloud sync: Firebase Firestore + Google Sign-In synchronization across web and Android apps
- Wallpaper: Custom photo backdrop with adjustable background dimming and blur intensity
- Liquid glass: shader-backed glass on the bottom navigation pill (History · Log spend · Settings) with a sliding switch thumb and spring animation; opt-in frosted-glass cards. Blur, transparency, refraction amount/height and chromatic aberration are adjustable in Settings → Theme
- Notifications & Reminders: Daily evening log-spend reminders (custom time + toggle in settings) and budget allowance alerts
- 10 theme presets, full color customization, heatmap palettes, font choice,
  compact density, card reordering, light/dark toggle
- History and Log spend are full-screen views that open with a slide-up animation
  from the bottom navigation pill; the dashboard keeps the hero summary + breakdown + trend + automations + piggy + backup

## What's intentionally different (web-only cosmetics)

- Weather (rain/snow) canvas effects, 3D tilt, the desktop cat, tab-title typewriter
- Piggy-bank GIF → 🐷 emoji; deposit sounds/confetti → toasts
- ⌘ keyboard shortcuts → the History and Log spend buttons on the bottom navigation pill
- Home-screen widgets / custom launchers are not ported

## Data & files

- Persistence: DataStore (`ledger`), keys mirror the web app's `ledger-*`
  localStorage keys.
- Backup format: `ledger-backup` v5 — identical to the web export, so a backup
  from `samncg.github.io/ledger` restores here and vice versa.
- Model/derived-math port: `app/src/main/java/com/ledger/app/ui/LedgerViewModel.kt`
  mirrors `src/components/App.jsx` in the web project.
