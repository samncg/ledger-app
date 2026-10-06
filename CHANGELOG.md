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

## v0.1.7

A private, local-first budgeting app that tracks your daily allowance and banks whatever you don't spend.

This release is mostly about scroll performance. The headline fix: the dashboard was capturing its whole
scrolling list into an offscreen layer on every frame whenever the *device* supported the edge blur —
ignoring the setting entirely — so the main screen kept stuttering with "Edge blur & fade" switched off.
v0.1.5 never captured at all, which is why it felt faster.

It also adds an experimental tabbed navigation, off by default — see below.

### ✨ What's New
- **The budget bar climbs into a spend** — logging no longer snaps the hero's budget progress: the fill animates up to the new figure and, on Android, a rattle of haptic ticks quickens as it lands, so the spend is felt arriving. The climb is held back while the log sheet is open and spent once it closes, in front of the user; the web gets the same eased climb plus a brief flash, having no haptics engine.
- **Watch-dial history** — scrolling the History list now clicks once per 44dp of travel, like turning a watch bezel (capped at one click per scroll event, so a fling doesn't buzz), and the list's top and bottom edges dissolve into the card it lives in — rows now fade hard into the card's own surface rather than merely softening, with the blur confined to the outermost sliver. On the web it is a pure fade, because a backdrop filter there re-sampled the scrolling list every frame; both follow Theme → Screen edges.
- **More category symbols** — the Add category form now offers a grid of 37 tap-to-pick preset symbols (◇ ◆ ● ○ ◎ ◐ … ⌘) instead of leaving you to type a glyph by hand, on web and Android.
- **Glass cards catch the light** — each liquid-glass card now carries a faint, soft rim instead of a drawn border: the edge light is projected from the card's own shape along a light angle, so it brightens unevenly around the perimeter (strongest at the corners nearest the light, falling away along the far edges) rather than lighting up as a uniform line, and it is composited additively through a blur at low opacity — the refracting brim a glass material has. The flat accent hairline that used to sit on the same edge is gone, so the card has one edge treatment rather than two.
- **The bottom pill refracts too** — the History / Log spend / Settings pill now wears the same liquid-glass material as the cards instead of a blur alone: it samples the scene actually behind it — the cards sliding under it, drawn over the wallpaper — so their bars and edges bend through the pill rather than only the wallpaper showing through, with the shared refraction height, refraction amount and chromatic dispersion and the same faint refracting rim. It reads the appearance sliders, so it refracts while the bar's own liquid-glass switch is on and falls back to a solid surface when it is off (and below Android 13, where the refraction shader is unavailable). For that it re-uses the layer the edge blur already captures, now taking that capture on whenever only a liquid-glass mode is on as well.
- **Experimental tabbed layout** — Theme → Experimental → *Tabbed layout* replaces the single long card dashboard with four destinations — **Today**, **Spending**, **Budget** and **History** — on a glass pill, with the Log action and Settings as their own round glass bubbles of matching size (Settings floats in the top-right corner; the pill and the Log bubble sit centred along the bottom). The pages are full-bleed and scroll right up under the restored top/bottom edge blur, switching destinations slides and fades, and the active tab simply keeps the theme's text colour while its neighbours fade back instead of sitting under a plate. Tapping Settings opens a **full Settings screen** rather than a drawer, whose tab row stays pinned just below the corner back arrow as you scroll; the same bubble closes the Log sheet, which no longer needs a close button in the bar. Back walks to Today, and the bar's glass samples the overlays as well as the page, so it refracts the Settings and Log screens themselves. Off by default.
- **Its own blur for the bottom bar and bubbles** — Theme → Liquid glass gains a *Bar & bubble blur* slider, so the bottom bar and its bubbles can be frosted independently of the cards' Gaussian blur.
- **Tabbed layout, tightened** — the hub pages now start right at the top (the tall gap above the first card is gone, so there is far less empty space to scroll through), the four-tab pill's slots are narrower and the pill and the Log bubble sit centred as one group, and *Move money* and *Budgets* became wide bars at the foot of their cards — the floating Settings bubble owns the cards' top-right corner, so anything tappable lives clear of it.
- **A wallpaper out of the box** — the app now ships a default wallpaper, so a fresh install opens on a photo instead of a flat black screen; the picker still replaces it, and the bundled copy is downscaled so it costs about half a megabyte.
- **The bar and bubbles have their own transparency** — Theme → Liquid glass gains *Bar & bubble transparency*, separate from the cards' *Transparency*, so the bottom bar and its bubbles can be made more or less solid independently of the cards.
- **The bar has its own glass switch (Android)** — Theme → Liquid glass splits the *Liquid glass cards* toggle from a new *Liquid glass bar & bubbles* toggle: the bottom bar and its Log and Settings bubbles (and the classic History · Log spend · Settings pill) keep their liquid-glass material by default, while the cards start solid, so the dashboard stays crisp and the bar still refracts. The bar's own *Bar & bubble transparency* now defaults to **0** rather than 100, so it opens as clear glass over the wallpaper instead of a tinted panel; with the switch off the bar falls back to an opaque surface rather than a transparent one.
- **Left-handed layout (Android)** — Theme → Preferences gains *Left-handed layout*, which mirrors the experimental layout's floating controls: the Settings bubble moves to the top-left corner and the Log bubble flips to the left of the pill, within reach of a left thumb.
- Changed: the liquid-glass defaults are now a clearer, more refracting material — Gaussian blur **0**, bar & bubble blur **0**, transparency **100**, sub-card opacity **40**, refraction height **14dp**, refraction amount **14dp** and chromatic aberration **50%**. Anyone who never touched those sliders gets the new look (the stored prefs omit values equal to the old defaults).
- Changed (web + Android): **overspends now come out of the bank balance by default.** *Overspends come from balance* starts **on**, so a day spent past its allowance is taken out of the balance rather than quietly covered by the monthly budget; the switch still turns it off (Budget settings, and Prefs).
- **The Spending page shows what you saved** — the category breakdown now leads with **Saved**, the allowance left over for the month on screen: green when you are under it and red when you are over, following the month browser so stepping back to an earlier month shows that month's leftover (web + Android).
- **The tabbed hub is now the app** — the tabbed layout (Today · Spending · Budget · History) is the only layout: the old single-scroll dashboard, its per-card reorder arrows and the History · Log spend · Settings pill are gone, along with the *Tabbed layout* switch and the *Card layout* group it left behind (Android).
- **Seven theme presets, and Paper by default (web + Android)** — the preset list is trimmed to **Paper, Mono, Daylight, Graphite, Linen, Sakura** and **Forest** (Midnight, Cream, Arctic and Ember are gone), and a fresh install opens on **Paper**. The wallpaper now follows the theme: *Auto* picks the **white** sheet for the light-leaning presets (Paper, Daylight, Linen, Sakura) and the **black** one for the dark presets (Mono, Graphite, Forest), so changing theme changes the backdrop with it.
- **Wallpaper presets (Android)** — Theme → Wallpaper gains *Auto / White / Black / Forest* chips above the photo picker, so the backdrop is one tap instead of a file picker; the custom-photo path is unchanged.
- **More fonts (Android)** — the Typography list grows from five to twelve: Condensed, Light, Medium, Black, Serif Mono, Casual and Small Caps join System UI, Sans Serif, Serif, Monospace and Cursive, drawn from Android's own system typefaces so nothing extra ships.
- Note (Android): the hub reuses the existing cards (Hero, Streak, Breakdown, Trend, Insights, Auto, Piggy, Backup, History) in fixed destinations rather than new card-less layouts.

### 🐛 Fixes
- Fix (web + Android): **the bank balance now tracks the bank account.** The balance banked each day's leftover allowance back without ever withdrawing the allowance first, so the monthly budget poured into it every period out of nowhere — spend nothing on a RM 3,000 budget and the balance grew by RM 3,000, spend RM 2,000 and it still grew by RM 1,000. Each day's allowance is now taken out at the start of the day and the leftover banked back at the end, so only what was actually spent leaves the balance: no drop on the 1st, no money from nowhere, and the figure matches the bank.
- Fix (Android): **the History page no longer keeps the previous theme's glass after a light/dark toggle.** The liquid-glass backdrop node swaps its draw lambdas without invalidating its draw, so a theme-only change left the old frame up until something else happened to redraw it — the card's text updated but the glass surface and its tint stayed on the old theme. The tint is now read through state inside the draw lambda, so Compose invalidates the glass the moment the theme changes (the cards, the bottom bar and its bubbles, and the full-screen tint).
- Fix (Android): **the tab row's edge fades now follow the scroll instead of sitting there permanently.** The Move money / Settings / History-sort tab rows dissolved their cut-off ends with a constant vignette; the left edge now fades in only once you've scrolled away from the start (and back out as you return) and the right edge mirrors it, each ramping over the 14dp band so the fade tracks the finger rather than snapping on.
- Changed (Android): **the Log a spend screen no longer repeats its title above the form.** The screen printed "Log a spend" as a page heading and then again on the card below it; the page heading is gone, so the card's own title — with its wallet icon — is the only one.
- Fix (Android): **the History list's top and bottom edges now dissolve by masking the list's own alpha — no blur and no fade tint — superseding the earlier "no colour fade" and "blur only on glass cards" fixes.** The backdrop blur sampled the rows themselves and smeared them into dark blobs behind the text, and a fade tinted to `surface` read as a light band on a liquid-glass card, whose surface is translucent (so a fade gated to "off" for glass left those users with no edge treatment at all). The list is now drawn into its own offscreen layer and masked with a vertical alpha gradient, so its rows fade into whatever sits behind them — solid card or glass — matching the web's pure fade. The colour-fade path stays for the dashboard, which keeps its blur (`ScreenEdgeBlur` now takes an optional backdrop).
- Changed (Android): **the bottom edge blur sits in a shorter band.** The bottom band is now **72dp** (it was 140dp), so less of the page sits under a blur that trails the sharp content by a frame — that lag is what reads as a flicker on a fast scroll. The top band stays at the status-bar inset plus the page's own top clearance (≈40dp), since it doubles as that page's top padding.
- Changed (Android): **the dimming behind a sheet now fades in place instead of sliding up.** The scrim under Move money / Budget was inside the same container as the sheet, so the darkening swept up the screen as a hard-edged black band behind it; it is now its own layer that simply fades while the sheet keeps sliding.
- Note (Android): **Cloudy (skydoves) was evaluated for the edge blur and not adopted.** Its backdrop API needs Compose 1.10–1.11, which pulls in AGP 8.9+, compileSdk 36 and Kotlin 2.4 — a full toolchain migration (and the android-36 SDK platform is not installed either) — while it samples the same captured snapshot the app already blurs, so it would not remove the one-frame lag the flicker comes from. Revisit as a dedicated migration if the 72dp band does not settle it.
- Fix (web + Android): **the bank balance now reaches back through every month you have logged**, not just the current period. A spend dated in a past month now moves the balance like any other, and the balance no longer drops when the period rolls over on the 1st. The window runs from your earliest logged day (or the current period's start, if that is older) through today, so it is a running balance — leftover allowance keeps banking, month after month.
- Fix (web + Android): **Budget → Edit budget → Bank balance now agrees with Today → Move money → Balance.** The field was showing the stored *starting* balance while the Money drawer showed the live balance, so the two disagreed by everything banked since; the field now shows the live figure, and typing a new one moves the starting balance to match (a negative balance is allowed, since overspends drain it).
- Fix (Android): **the pinned Settings tabs no longer cut the page off at a hard seam** — the opaque band behind Theme · Chart · Categories · Travel · Prefs now fades out below the row, so the settings scroll into nothing instead of stopping at a line that slides up under the tabs.
- Fix (Android): **the edge blur now spans its whole top/bottom band**, instead of stopping ~halfway and leaving a sharp-but-darkened strip below it — that mismatch is what made a bright line of text flicker as it scrolled in.
- Chore: **the trophy system is entirely gone** — its last remnants (the `trophies → streak` card-id migration kept for old layouts, on both platforms) are removed.
- Fix (Android): **the bubbles and the bottom pill now refract whatever they actually cover.** They sampled the wallpaper layer alone, so with the Settings or Log screen open the bar bent the page hidden behind it; the overlays are now captured into their own layer and combined under the page, so the glass bends the screen that is really there.
- Note (Android): **the bottom pill's liquid glass is back to how it was before the seam fix.** The capture-widening that was added to stop the hard seam also disturbed the pill's material, so it has been reverted and the pill refracts exactly as it did before. The seam returns if the refraction height is pushed past roughly a third of the pill's height.
- Fix (Android): the History list no longer draws the colour fade at its top and bottom — asked for; the rows stay legible right up to the edge and only the edge blur softens them.
- Fix (Android): **the History rows stay sharp when "liquid glass inside cards" is on.** With that look enabled the rows sit on the glass rather than on the card's own surface, so fading them into that surface washed the material out; the colour fade is now dropped while the slight edge blur remains.
- Note (Android): tabs and full-screen views close from their X button or a back press — **not** by dragging the panel down. There is no drag-to-dismiss and no drag handle by design, so the bottom pill's close button stays reachable and the panel can't be half-swallowed mid-swipe.
- Note (Android): the glass is built on the vendored KMP Liquid Glass Compose library (`com.kashif_e.backdrop`), whose `lens` effect runs a rounded-rect SDF refraction shader with optional chromatic dispersion — the same optics as the View-system `QWEA0/Liquid-Glass-Android` library **without** adopting it. That library targets the Android View system and its own README says not to use it from Compose (pointing Compose apps at Kyant0/AndroidLiquidGlass instead), so wrapping it in `AndroidView` would mean re-hosting every glass surface and giving up the shared backdrop layer, the glass style settings and the per-surface tuning the app already has — for no visual gain, since the refraction on screen is the same effect.
- Fix (parity): **the web app's "Saved to balance" now banks what the Android app banks.** Both add up a leftover allowance per elapsed day, but the web recomputed the allowance for each day from top-ups dated on or before it, while Android uses the current net allowance (budget + all top-ups) for every day — so any dated top-up made the two heroes disagree (a RM 100 top-up on day 10 of a RM 310 / 31-day budget differed by ≈ RM 29). The web now matches Android, and "Saved to balance" always agrees with the "Daily allowance" shown above it.
- Fix (parity): **monthly automations on the web no longer drift off the 31st.** The web advanced from the clamped date, so a rule starting 31 Jan produced 28 Feb and then 28 Mar, 28 Apr… for good; it now stays anchored on the rule's start day-of-month (31 Jan, 28 Feb, 31 Mar, 30 Apr), as Android already did.
- Fix (parity): **web Insights "Best day" no longer reports a RM 0.00 day.** It walked every calendar day with a 0 for days with no spend, so any skipped day won; it now takes the minimum over days that actually had a spend, like Android. "Biggest change" is likewise scanned over every category seen this month *or* last (not just the categories that still exist) and is hidden when nothing changed, both matching Android.
- Fix (parity): web `cleanTags` trims again after stripping a leading '#', so "# lunch" stores as "lunch" and not " lunch"; tags are also no longer normalised when entries are loaded, so a stored entry isn't silently rewritten. Both match Android.
- Fix (Android): **a backup carrying an empty `piggies` array no longer bricks the app.** `[]` decodes to a *non-null* empty list, which slipped past the "default piggy" fallback and left the state machine calling `.first()` on nothing during startup — a crash on every launch until app data was cleared. The repository now normalises an empty list back to the default piggy.
- Fix (Android): **Firestore sync now carries language, travel mode, streak grace, the reminder settings and the screen-edge toggle.** The payload and its reader whitelisted twelve `Prefs` fields and silently dropped the rest, so a trip or a language chosen on one device never reached another — even though the web client syncs them. Wallpaper, the liquid-glass look, the widget theme and the app lock stay device-local by design.
- Fix (Android): **restoring a JSON backup restores every preference.** Import copied twelve fields and dropped the rest — language, travel, grace days and every appearance setting among them — while the export wrote the whole of `Prefs`; it now applies each key the backup actually carries and leaves keys it doesn't carry alone.
- Fix (Android): **the notification permission prompt is no longer missed on a cold start.** Readiness wasn't a key of the effect that asks, so whenever the stored prefs finished loading *after* `onResume` had already run, nothing re-checked and the prompt never appeared that session.
- Fix (Android): **the travel exchange-rate field can be typed into.** It was bound straight to the stored `Double` and re-formatted on every keystroke, so typing a decimal point produced a second '.' which parsed to null and reset the rate to 0 — the rate could only ever be set by the automatic sync. It now edits through a local draft that re-syncs only when the rate changes elsewhere.
- Fix (Android): the log form's tag button is translated — it was hard-coded English ("Add") in all seven languages.
- Fix (Android): the frequent-entry chips no longer pair a home-currency amount with the trip's symbol. While a trip is active they show and enter the amount in the trip's currency at the current rate, so re-logging a RM 20 spend on a JPY trip no longer logs ≈ RM 0.60.
- Fix (Android): the travel hero's headline figure is labelled with the currency it is actually in, instead of the trip's current currency, when the trip's currency has been changed since those entries were logged.
- Fix (Android): the spending-trend tooltip is cleared when the series changes, instead of pinning a stale date and figure to the left edge of the following month.
- Fix (Android): a new automation defaults to the first category that exists rather than a hard-coded "food" that the user may have deleted.
- Fix (Android): the wallpaper is decoded off the composition thread and downsampled, instead of blocking the first frame of every cold start on a full-resolution bitmap.
- Fix (Android): a widget refresh can no longer take the process down if the datastore read fails — all five widgets now use the guard the shared path already had.
- Fix (Android): **the dashboard no longer captures its whole scrolling list into a layer when the edge blur is off.** `Modifier.layerBackdrop` re-records the content into an offscreen layer *every frame* — that is what the edge blur samples — but the dashboard took that cost whenever the *device* supported the blur, regardless of the setting, so the main screen kept stuttering with "Edge blur & fade" turned off. v0.1.5 never captured anything. The dashboard and the History card now both capture only while the effect is actually on.
- Fix (Android): the edge blur now reaches only a short way in. The colour fade still spans the full band so the look is unchanged, but the per-frame RenderEffect — the expensive half, and the part that re-records the content into a layer — covers a fraction of the pixels it did (the dashboard's 140dp band now blurs over 72dp, the history card's 32dp over 18dp).
- Fix (web): the history rows' end fades no longer use a `backdrop-filter`. A filter over a scrolling list re-samples that list every frame, and it sat inside the card's own glass blur, so the nesting made the dashboard stutter while scrolling; the fade carries the effect on its own, and reads stronger for it.
- Fix (Android): the History list's dial clicks are paced to at most one per 60 ms. A fling crosses several 44dp detents per frame and every click is an IPC to the system vibrator, so a burst of them cost the frames the ticking was meant to accent.
- Fix (Android): the notification permission prompt is now requested from `onResume` — once per launch, only while the screen is actually in front and unlocked, and skipped when the permission is already granted. It used to be requested from composition, which runs before the activity is resumed and raced the lock screen taking focus; Android drops or instantly dismisses a dialog in that state, which is why the prompt appeared with nothing to press.
- Fix (Android): the notification prompt no longer fires before the stored prefs have loaded, where it read the default `notificationsEnabled = true` and so asked even users who had switched notifications off.
- Perf (Android): `history()` now filters and totals the ledger in a single pass instead of about eight (a map, four filters, two sum passes and then a per-group sum), taking 20–30 ms of main-thread work off every rebuild — opening History, logging a spend, or changing a filter. Output is unchanged, checked against the seeded 602-entry ledger: 602 entries / RM 36,864.00 unfiltered, 100 / RM 6,232.00 for Food, and 200 / RM 12,452.00 for Food + Transport.
- Build (Android): the app module now writes Compose compiler reports on every build (class stability and composable skippability, quantified in `app/build/compose_reports`), so render-performance questions can be answered from the build output instead of guesswork.
- Dev (Android): measure render performance on a *release* APK, never a debug one. A debug build installs as `run-from-apk` and ART refuses to AOT-compile a debuggable app (`cmd package compile -m speed` leaves it at `verify`), so the same code reports several times the jank; on an emulator the frame budget is also dominated by guest kernel timekeeping and the translated GPU, so only large relative differences between two runs on the same setup are meaningful.

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
