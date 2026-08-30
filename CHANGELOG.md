# Changelog

> **For future AI agents & contributors:** This is the project's living log of changes and features.
> Whenever you modify this repo—add a feature, fix a bug, refactor, migrate a dependency, change the
> build, replace an asset, or deploy—**you MUST add a new entry** describing it. Append entries in
> reverse-chronological order at the top of the list below. Do **not** edit or delete existing entries,
> do **not** rewrite history here, and keep each entry to a single compressed line. If you make a series
> of changes in one session, group them under one `Added/Changed/Fixed` entry.

## v0.1.3

A private, local-first budgeting app that tracks your daily allowance and banks whatever you don't spend.

### ✨ What's New
- **Liquid-glass close button** — the bottom navigation pill now springs (with a bounce) into a circular glass close button whenever History, Log spend, Settings, Budget, or Money is open. It floats above every drawer, and the redundant top-right close buttons were removed.
- **Haptic feedback** — settings sliders deliver a tactile tick as you cross each step, and the pill's close button pulses when tapped.

### 🔧 Improvements
- The pill is now bottom-center and samples the wallpaper directly beneath it, so the glass no longer refracts a misaligned region or gets blocked by a rectangle.
- Closing a view is snappier — the pill runs a lighter blur-only glass pass during its morph, so it stays smooth.
- Drawer sheets (Settings / Budget / Money) now use a dark, cohesive container that matches the full-screen History and Log spend views.
- History and Log spend open with a frosted-glass backdrop while the inner cards stay solid.
- Budget period now auto-syncs to the real number of days in the current month (web + Android).
- Small and muted text no longer renders gray.

### 🐛 Fixes
- History and Log spend no longer crash when opened.
- Chromatic aberration now actually applies and has a 0–100% intensity slider in Settings → Theme.
- The system back gesture (edge swipe) now closes the open drawer/view instead of exiting the app.

---

> **Version:** 0.1.3 · **Platform:** Android (APK) · **Requires:** Android 8.0+ (API 26); full liquid-glass effects on Android 13+ (API 33)

---

## v0.1.x — one-off session log (reverse-chronological)

- Revert (Android): video wallpaper support removed — wallpaper picker returns to image-only (`image/*`), the ExoPlayer/media3 video background and `LoopedVideoBackground`/`isVideoWallpaper` helpers are deleted, and the picker labels/`setWallpaperFromUri` accept photos only.
- Feature (Android): haptic feedback added to the settings sliders (tick per step crossing) and to the bottom pill's circular close button.
- Fix (Android): bottom pill moved back into the main window (no more Popup) and the Settings/Budget/Money drawer sheets converted from `ModalBottomSheet` dialogs to in-window animated sheets so the pill z-orders above every overlay; pill is bottom-center-aligned, uses blur-only glass (no per-frame refraction) to stop the morph-to-close lag, and samples the wallpaper backdrop at the correct coordinates.
- UI (Android): pill springs (bounce) into a circular liquid-glass close button whenever History / Log spend / Settings (or Budget / Money) is open; removed the redundant top-right close buttons on screens and drawer headers.
- UI (Android): drawer sheets (ModalBottomSheet) now use a black container to match the full-screen views.

- Feature (Android): video wallpapers — upload a short video loop as the dashboard background (cover-scaled, muted, looping via ExoPlayer?media3), with dim/blur controls; wallpaper picker accepts image/video ("*/*").

- Feature (Android): "Chromatic aberration amount" slider (0–100%) added in Settings → Theme, replacing the old on/off toggle; higher values intensify the prismatic fringing (and bring it off at 0%).

- Fix (Android): History/Log-spend no longer crash on open — full-screen glass background switched to the safe pure-blur recipe (`drawPlainBackdrop` + blur + `colorControls`), dropping the per-frame `vibrancy`/`lens` passes that the skill warns cause jank/crashes over scrolling content.

- UI (Android): History/Log-spend full-screen views now render a frosted glass backdrop while the inner cards are solid (inverted from before).
- UI (Android): small/muted text no longer renders gray — `onSurfaceVariant` maps to the bright primary text color.
- Fix (Android): chromatic aberration now actually applies when toggled — `lens(...)` was passing `depthEffect`; it now sets `chromaticAberration = true` alongside `depthEffect`.

- Change (web + Android): budget period (`periodDays`) now auto-syncs to the real number of days in the current month on load/resume/render — previously it was only the setup-time default and stayed fixed.

- Fix (Android): system back gesture (side swipe) now closes the open History / Log-spend full-screen view instead of exiting the app — added `BackHandler` for each and enabled predictive back (`android:enableOnBackInvokedCallback`).

- Change (web + Android): setup "Period length (days)" now defaults to the real number of days in the current month via `daysInMonth()` (hardcoded `placeholder="30"` replaced with a dynamic value).

- Deploy: web build (`base: /ledger/`) published to `samncg.github.io/ledger` via the `ledger/` folder of `samncg/samncg.github.io`.
- Asset: app icon replaced with `ledger.png`, wrapped in a 21dp-inset adaptive-icon foreground so it isn't zoomed.
- UI (Android): hero headline enlarged to 34sp; "On track" health badge and streak badge removed.
- UI (Android): History & Log-spend now open with a slide-up + fade `AnimatedVisibility` transition.
- UI (Android): settings drawers use a fixed `contentHeight` so the Categories tab no longer shrinks the sheet.
- UI (Android): liquid-glass bottom navigation pill (History · Log spend · Settings) with a spring-sliding switch thumb, centered via a `Box` overlay (thumb is not a layout child).
- UI (Android): proper shader-based liquid glass on cards + pill via `com.kashif_e.backdrop` (`drawBackdrop`: vibrancy + blur + lens refraction + chromatic aberration).
- UI (Android): glass controls added in Settings → Theme (blur, transparency, refraction amount, refraction height, chromatic aberration toggle); exposed via `GlassStyle`/`LocalGlassStyle`.
- Fix (Android): removed opaque root background and `onDrawSurface` rect so glass isn't blocked by a theme-colored rectangle; draws glass in a shape-clipped container.
- Fix (Android): pie chart now uses proportional degrees (`pct * 3.6`) instead of raw percent-as-degree sweep.
- Feature (Android): notifications & reminders — daily evening log reminder (time + on/off in Settings), budget allowance alerts, notification channel, boot receiver, POST_NOTIFICATIONS permission.
- Feature (Android): custom photo wallpaper with background-dim and blur sliders, upload/replace/remove; device-local (excluded from cloud sync).
- Feature (Android): Firebase cloud sync parity with web — Google Sign-In, Firestore `ledger/{uid}`, `google-services.json`, `com.google.gms.google-services` plugin.
- Change (build): project toolchain upgraded Kotlin 2.0.20 → 2.3.0 (and migrated `kotlinOptions` → `kotlin.compilerOptions`) to enable the shader-backed liquid-glass library; added `backdrop` + `play-services-auth` deps.
- Change (data): spending categories are strictly single-category (web + Android); existing multi-category logs are auto-normalized on load / sync / import.
- Change (web): added `normalizeExpense(s)` + single-category `expCats`; `addExpense`/`updateExpense` enforce one category.
- Change (Android): `Prefs` extended for wallpaper + glass (blur/opacity/refraction/refractionHeight/chromaticAberration) and notification prefs (reminder hour/minute, budgetAlerts).
- Change (Android): removed top nav/hero bar; actions moved to bottom pill; dashboard hero summary card restored at top of the list.
- Change (data): `Repository` gained `getLastSync`/`setLastSync` (DataStore `ledger-synclast2`) and `appContext`; `Models` gained `AuthUser`, `normalizeExpense(s)`, single-cat `expCats`.
- Change (Android): Firestore payloads use single-category expenses; `FirebaseConfig`/`FirebaseManager` init.
- Fix (Android): liquid-glass pill no longer had off-center buttons or a full-width rectangular shadow.
- Docs: README + android/README updated (Kotlin 2.3.0, API 33+ note, single-category, liquid glass, bottom pill).
- Build: `gradlew` uses `JAVA_HOME=C:/Program Files/Java/jdk-17`; `local.properties` points to Android SDK; debug APK produced at `app/build/outputs/apk/debug/app-debug.apk`.
