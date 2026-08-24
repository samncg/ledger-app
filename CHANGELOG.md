# Changelog

> **For future AI agents & contributors:** This is the project's living log of changes and features.
> Whenever you modify this repo—add a feature, fix a bug, refactor, migrate a dependency, change the
> build, replace an asset, or deploy—**you MUST add a new entry** describing it. Append entries in
> reverse-chronological order at the top of the list below. Do **not** edit or delete existing entries,
> do **not** rewrite history here, and keep each entry to a single compressed line. If you make a series
> of changes in one session, group them under one `Added/Changed/Fixed` entry.

## v0.1.x — one-off session log (reverse-chronological)

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
