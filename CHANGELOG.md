# Changelog

> **For future AI agents & contributors:** This is the project's living log of changes and features.
> Whenever you modify this repo—add a feature, fix a bug, refactor, migrate a dependency, change the
> build, replace an asset, or deploy—**you MUST add a new entry** describing it. Append entries in
> reverse-chronological order at the top of the list below. Do **not** edit or delete existing entries,
> do **not** rewrite history here, and keep each entry to a single compressed line. If you make a series
> of changes in one session, group them under one `Added/Changed/Fixed` entry.
>
> **Note:** the `v0.1.5` section below was never released as a whole; only its trophy-streak,
> splash-screen and blur-system work was carried forward into `v0.1.6` at the owner's request.
> Its remaining entries keep their original 0.1.5 label and are otherwise unchanged.

## v0.1.6

A private, local-first budgeting app that tracks your daily allowance and banks whatever you don't spend.

### ✨ What's New
- **Daily logging streak** — logging a spend every day builds a streak (kept alive until a whole day is missed), shown on a new **Streak** card that leads with the current run as a large number beside the best streak, today's status and a hint (web + Android). The collectible trophy gems this originally shipped with were dropped before release, leaving the streak on its own.
- **Android intro splash** — a brief branded splash now covers cold start instead of a black frame, matching the window background so there is no seam, and it fades away once the stored data is ready rather than vanishing instantly.
- **Tags on every spend** — attach up to 8 free-form tags to a spend (type and press Enter; chips with an × to remove), shown under each History row and filterable from the "Sort & filter" panel alongside the category pills; tags are searchable and counted in the filter badge (web + Android).
- **Four more Android widgets** — alongside "allowance left today": a category pie chart, budget progress (spent vs the monthly budget, with a percentage), the active piggy bank, and the logging streak; all refresh on the same 15-minute WorkManager cadence and on backgrounding, and share the existing widget styling.
- **Streak grace days** — a new Streaks setting forgives up to two skipped days inside a logging streak, so a missed day no longer resets the run; applied to both the current and the best streak (web + Android).
- **Travel mode** — a trip page that replaces the dashboard: log spends in a foreign currency at a rate you set, and the hero shows what you've spent abroad next to the equivalent in your home currency. Each entry is stored with its home-currency amount (so budgets and statistics are untouched) plus the original foreign figure, and is auto-tagged `travel`. Trip name, currency, rate and start date live in Prefs → Travel mode; "End trip" returns to the normal dashboard and keeps the entries (web + Android).
- **Full localization** — a Language setting adds Spanish, Simplified Chinese, Russian, Thai, Japanese and Korean, and every user-facing string is translatable: navigation, every card and its title, the entry form, history, all four customization tabs and their descriptions, setup, the command palette, dialogs, locks, reminders and toasts, trophy names/rarities, and the home-screen widgets (dynamic text through `t()`, plus `res/values-xx/strings.xml` for the widget label/description resources). Dictionaries live in `src/lib/i18n.js` + `src/lib/strings/*` + `src/lib/locales/*` (web) and `ui/Strings.kt` + `ui/strings/*` + `ui/locales/*` (Android), and `scripts/check-locales.cjs` asserts every locale covers every key with identical `{placeholders}`.
- **Organized customization drawer** — every settings group in the Theme tab is collapsed behind its own button with a chevron, so the drawer reads as a short index instead of one long scroll; in Prefs the **Language** and **Streaks** groups now come first, and the Travel tab's trip controls are shown directly rather than behind a subcategory (web + Android).
- **Edge blur & fade toggle** — Theme → Screen edges turns the top/bottom blur+fade off entirely for a crisp edge (Android gates the progressive blur; web drops the sticky top bar's backdrop blur).
- **Travel tab & live exchange rates** — travel mode moved out of Prefs into its own settings tab, with the trip's controls sitting directly at the top of that tab rather than behind a subcategory, and the home-currency picker and per-currency breakdown below them; the rate now syncs automatically from the ECB reference rates (`api.frankfurter.app`, no API key — only the two currency codes are sent, never amounts or identifiers) when a trip starts or either currency changes, with a manual refresh, an auto-sync toggle, and a fallback to the saved rate when offline.
- **Logged currencies are never rewritten** — every travel entry keeps the currency it was logged in, so changing the trip's currency or refreshing the rate only affects *new* entries: the trip page shows a per-currency subtotal beside the home-currency total, a breakdown of every currency used, and re-editing an older entry uses that entry's own currency rather than the trip's current one.
- **Language picker on first run** — the setup screen now asks for the language before the budget, so the rest of onboarding — and every screen after it — reads in the chosen language.
- **Ending a trip asks first** — the End trip button (on the trip page and in the Travel tab) now opens a confirmation that spells out what happens: the trip's spends stay in History, tagged, and nothing is deleted.
- **Travel logs read in both currencies** — a History row for a spend logged abroad now shows the foreign figure it was entered in, with its home-currency equivalent underneath (web + Android).
- **A trip tags its own logs** — while a trip is active every new spend is tagged `travel` *and* with the trip's name, so one trip's entries can be filtered by name from the History filter row; renaming a trip only affects entries logged after the change.
- **Streak in the daily reminder** — the daily log reminder now carries your current streak, and on the day a missed yesterday leaves the run resting on a grace day, a separate, firmer notification fires instead: today is the last chance to keep it (Android).
- **Home-screen widgets: light or dark** — Theme → Home-screen widgets offers a light/dark choice that applies to all five widgets and is independent of the app's own theme. (Android only — the web app has no widgets.)

### 🐛 Fixes
- Fix (Android): cloud sync no longer erases an expense's tags and its travel currency — `FirebaseSync` mapped only seven fields in each direction, so Firestore's immediate local echo silently dropped `tags`, `currency` and `foreignAmount` a moment after every edit. Travel entries fell back to the home currency and auto-tags never stuck; every field an expense carries now round-trips.
- UI (web): the hero badge that counts days under allowance now reads "Nd under budget" instead of "N-day streak", so it isn't confused with the new daily spend streak.
- UI (Android): dashboard content now blurs *and* fades out at the top and bottom edges instead of hard-clipping — the scrolling cards are captured into a backdrop layer and a progressive gaussian blur (the theme's liquid-glass machinery, strongest at the screen edge and easing to sharp further in) is drawn over them, under a colour fade to the background, above the cards but below the nav pill; the status-bar clip was removed so cards scroll all the way under the blur rather than being cut off.
- Fix (Android): the edge blur is skipped below Android 13, where the mask runtime shader doesn't exist and the blur would render as a hard-edged, uniformly-blurred slab that smeared across the cards while scrolling — those devices get the fade on its own.

---

> **Version:** 0.1.6 · **Platform:** Android (APK) + Web · **Requires:** Android 8.0+ (API 26); full liquid-glass effects on Android 13+ (API 33)

---

## v0.1.5

A private, local-first budgeting app that tracks your daily allowance and banks whatever you don't spend.

### ✨ What's New
- **Budget alerts** — 80%/100% alerts for the monthly budget and per-category budgets, fired once per threshold per period (Android notifications; web in-app alert plus optional browser notification requested from settings).
- **Monthly insights** — a new card comparing this month to last: total spent (+/− %), biggest category change, avg/day, projected month-end vs pace, best/worst day.
- **Receipt photos** — attach an optional downscaled photo to any spend; shown as a thumbnail in History that opens full-screen (web + Android).
- **App lock** — a device-local lock toggle: biometric / device-credential on Android, PIN with optional WebAuthn device unlock on web; re-locks when backgrounded.
- **Smart category suggestions** — typing a note auto-selects the category, learned from your own history first (exact notes, then personal vocabulary such as brand names, resolved by most-used with a recency tie-break), falling back to the built-in keyword list (rice, latte, grab, laundry, …); a manual pick always wins, and starting a new entry re-enables auto-pick.
- **Home-screen widget (Android)** — a small app-widget showing today's remaining allowance ("RM x left today"), tap to open the app; refreshes every 15 minutes (WorkManager; `updatePeriodMillis` is clamped to 30 min by the platform) and whenever the app is backgrounded.
- **Undo for deleted spends** — deleting a spend offers an Undo that re-inserts it at its original position.

### 🐛 Fixes
- Fix (Android): the new corrupt-blob guard no longer false-positives on nullable slices — `saveSettings(null)`/`saveSavedTheme(null)` wrote the JSON literal `null`, which the loader mistook for corruption and paused sync; null slices are now stored as absent and read as absent, and the warning names the affected slice.
- Fix (Android): sync no longer swallows the next edit — removed the unconsumed `skipNextPush` flag that made the first change after any cloud pull never reach Firestore.
- Fix (web + Android): validate settings / expenses / top-ups / piggies on import and Firestore load (`periodDays >= 1`, parseable `startDate`, drop `null`/invalid entries) so a malformed backup can't render NaN or crash the app.
- Fix (web + Android): recurring automations advance by their configured frequency and no longer drift — monthly rules anchored on the 31st clamp correctly instead of sliding to the 28th, and malformed rule dates are normalized instead of crashing.
- Fix (web + Android): device-local piggy texture/sound survive a cloud pull (merged by id) instead of being wiped by the stripped remote copy.
- Fix (web + Android): a failed Firestore write now clears the dedupe hash so it retries, and the hash resets on sign-out/account switch — edits are no longer silently dropped.
- Fix (web + Android): "saved to balance" now uses each elapsed day's allowance in effect (top-ups dated on/before that day) instead of retroactively rewriting past days with today's allowance.
- Fix (web + Android): first sign-in no longer overwrites existing local data with a cloud copy when this device already has data (it pushes local instead).
- Fix (Android): corrupt DataStore blobs are kept rather than silently overwritten with defaults, and `lastSync` read-modify-write is now atomic.
- Fix (Android): reminders/notifications schedule reliably via `goAsync()`; wallpaper/receipt input streams are closed; a future start date no longer marks day 0 as elapsed.
- Fix (Android UI): dashboard up/down reordering matches the visible (filtered) order; the daily strip no longer steals vertical scroll; the Customize sheet no longer clips in short/landscape windows; the heatmap opens on the newest weeks; toasts animate out and the slider haptic baseline isn't stale.
- Fix (web): command-palette actions no longer use stale data; history search tolerates note-less entries; the Confirm dialog can't fire twice on Enter; chart prefs with non-numeric values no longer crash; the piggy file picker resets after a "too large" error; the error-boundary reset works with storage blocked; ids sort/compare consistently; the palette trigger and toasts are keyboard/AT accessible.
- Fix (web + Android): smart category auto-selection is no longer left permanently off after a single manual category pick — it re-runs on every note edit, while a manual selection is still honoured until the note changes again.
- Fix (Android): the panels nested inside cards now follow the "Liquid glass cards" toggle — hero stat tiles, the daily-spend strip, insights day tiles, piggy panels, secondary buttons and chips turn frosted instead of staying solid black, so the card's glass shows through. The strength is adjustable with the new **Sub-card opacity** slider (0% = fully clear, 100% = solid, default 40%).
- Fix (web build): the GitHub Pages bundle now uses a relative base (`base: './'`) so `dist/` resolves its assets from any sub-path, as the README documents — the previous absolute `/ledger/` base 404'd whenever the site was served from a different path.

---

> **Version:** 0.1.5 · **Platform:** Android (APK) + Web · **Requires:** Android 8.0+ (API 26); full liquid-glass effects on Android 13+ (API 33)

---

## v0.1.4

A private, local-first budgeting app that tracks your daily allowance and banks whatever you don't spend.

### ✨ What's New
- **Scrub the daily spend strip** — slide your finger across the "Daily spend · this period" bars to preview each day's date and spending in real time, with a haptic tick per day; tap any bar to pin it.
- **Month browser** — a ‹ / › selector on the Spending trend and Category breakdown cards lets you look back at previous months with full-month views.
- **Liquid glass screens** — give the Log a spend and History views a frosted glass look from Settings → Theme → Liquid glass, and optionally make the inner cards themselves liquid glass on a flat, lighter background.
- **Full color picker** — new Hue, Saturation, and Brightness sliders with a live preview, so you can pick any color alongside the preset swatches and hex field.
- **Auto-continue from cloud** — sign in with Google on the intro screen and, if your cloud save exists, the app loads it and skips setup automatically.

### 🔧 Improvements
- Spending trend now opens on a 30-day view.
- Heatmap weekday labels no longer clip at the bottom in the 1-year view, and the 1y heatmap scrolls horizontally.
- Toast notifications swipe away, and the undo action no longer overlaps the card surface.
- Setup screen: clearer error flashes and Google sign-in restore.
- Small UI polish — removed a cramped third stat on the breakdown, fixed the budget "LinkText" buttons, trimmed text-field clipping, tidied the Piggy bank layout, reset the bottom pill highlight, and applied keyboard/navigation insets across all forms.

### 🐛 Fixes
- Fix (web + Android): recurring automations now advance by their configured frequency (daily / weekly / monthly) instead of firing every day after the first run; the next-occurrence date was computed as last + 1 day regardless of freq, so a monthly rule activated daily. Both runRecurring and the next-run label now use advanceDate(last, freq) (web App.jsx, Android LedgerViewModel.kt).
- The budget period now rolls over at each month boundary — on the 1st you see Day 1 / total, and the daily strip, category breakdown, and month label all agree.
- Full-screen liquid glass falls back to the safe blur + tint recipe (the heavier refraction shader could crash full-screen views), and the drawer sheets stay opaque so scrolling stays smooth.

---

> **Version:** 0.1.4 · **Platform:** Android (APK) · **Requires:** Android 8.0+ (API 26); full liquid-glass effects on Android 13+ (API 33)

---

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

- Fix (web + Android): "Saved to balance" now banks using the current net daily budget (monthlyBudget + net top-ups), so it always matches the shown "Daily allowance"; the old per-day top-up accumulation inflated the saved amount when allowance top-ups were later returned.
- Feature (web + Android): new "Overspends come from balance" budget preference (Settings → Budget settings and Settings → Prefs) — choose whether an overspend drains the bank balance or is covered by the monthly budget (default: monthly budget); the pref is now cloud-synced.
- Fix (web + Android): banked leftover is now clamped to non-negative — overspending reduces the total budget (and shows in Budget Progress) instead of draining the bank balance; combined with the month rollover, the daily strip / "Saved to balance" no longer count a stale day.
- Fix (web + Android): budget period now rolls over at each month boundary — `startDate` realigns to the 1st of the current month and `periodDays` syncs to the month length, so on the 1st it shows Day 1/total (not a stale 30/30) and the breakdown, daily strip and month label all agree.
- Change (Android): spending trend now defaults to 30d instead of 14d.
- Feature (Android): color picker dialog now includes Hue / Saturation / Brightness sliders + live preview, so any color can be chosen beyond the swatches and hex field.
- Fix (Android): heatmap weekday labels no longer clip at the bottom in 1y/scrollable mode (tight line height + single-line).
- Feature (Android): intro/setup screen auto-closes after Google sign-in when the cloud already has a save (forces a load when local settings are empty).
- Fix (Android): UI/UX polish & fixes — "Run now" automation button made clickable, toast swipe-to-dismiss added and Undo action isolated from card surface, setup screen gained error notifications + Google sign-in restore, heatmap layout overlap fixed and 1y mode made horizontally scrollable, breakdown card "Top" stat removed to decramp 3-stat row and budget LinkText buttons fixed, AppTextField text clipping removed, Piggy bank saved/goal layout polished, floating navbar selection highlight reset on dashboard, and keyboard/navigation insets applied across all forms.
- Feature (Android): "Glass the inside cards" toggle added under Liquid glass — lets the Log a spend / History cards themselves be liquid glass on a flat light background instead of frosting the whole backdrop; the backdrop frosted mode was also lightened (white lift + lower tint) so the screens aren't so dark.
- Fix (Android): History/Log a spend full-screen glass reverted to safe blur + tint that follows the glass blur/transparency settings (the full refraction shader crashed on full-screen); removed the laggy liquid-glass panel from the Settings/Budget/Money drawers (kept solid).
- Fix (Android): Log a spend / History full-screen backdrops and the drawer panels now render as real shader-based liquid glass (blur + refraction + chromatic, like the cards) with a translucent tint, so they look glassy rather than a near-opaque blur.
- UI (Android): Settings/Budget/Money drawer panels now render as frosted liquid glass (blur + tint over the wallpaper) when "Liquid glass screens" is on, instead of a solid surface.
- Feature (Android): "Liquid glass screens" toggle added in Settings → Theme; when on, the Log a spend and History drawers render a frosted liquid-glass backdrop (new `prefs.glassScreens` + `GlassStyle.screensGlass`).
- Fix (Android): "Daily spend · this period" strip now supports sliding a finger across the bars (scrub) — each bar under the finger is selected with a haptic tick, showing that day's date + spending; single taps still work.
- Fix (Android): "Daily spend · this period" strip replaced horizontal scrolling with tap-to-inspect — tapping a bar selects it and shows that day's date + spending (and the detail line clears on re-tap).
- Feature (Android): month selector added to the Spending trend and Category breakdown cards so you can browse previous months; added `monthStartKey`/`monthEndKey`/`monthLabel` date helpers and made `trend()`/`breakdown()` accept an end-date reference.
- Build: version bumped to 0.1.3 (versionCode 3); disabled the crashing `NullSafeMutableLiveData` lint detector so `assembleRelease` passes, and signed the release APK with the debug keystore for installable sideload/test builds.
- Fix (Android): settings/budget/money drawer sheets now use the theme `surface` color (adaptive to light/white themes) instead of a hardcoded black container, so text stays readable in light mode.
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
