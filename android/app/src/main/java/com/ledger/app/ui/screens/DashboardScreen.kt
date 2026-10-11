package com.ledger.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.BudgetDrawer
import com.ledger.app.ui.components.ConfirmDialog
import com.ledger.app.ui.components.DrawerScrim
import com.ledger.app.ui.components.HUB_BUDGET
import com.ledger.app.ui.components.HUB_TODAY
import com.ledger.app.ui.components.HUB_SPENDING
import com.ledger.app.ui.components.HUB_TAB_COUNT
import com.ledger.app.ui.components.HubBottomBar
import com.ledger.app.ui.components.HubSettingsBubble
import com.ledger.app.ui.components.HubTabBody
import com.ledger.app.ui.components.MoneyDrawer
import com.ledger.app.ui.components.ScreenEdgeBlur
import com.ledger.app.ui.components.SettingsScreen
import com.ledger.app.ui.components.ToastOverlay
import com.ledger.app.ui.components.hubTopInset
import com.ledger.app.ui.components.progressiveEdgeBlurSupported
import com.ledger.app.ui.parseColor
import kotlinx.coroutines.launch

/* ═══════════════════════════════════════════
   DASHBOARD — the tabbed hub: Today · Spending · Budget · History,
   with the Log and Settings bubbles floating over it, plus the overlays.
   ═══════════════════════════════════════════ */

@Composable
fun DashboardScreen(
    vm: LedgerViewModel,
    s: LedgerState,
    initialShowLog: Boolean = false,
    /* The Haze state the bars sample — the whole scene: wallpaper, page then overlays. */
    sceneHaze: HazeState? = null,
) {
    val cs = MaterialTheme.colorScheme

    var showMoney by remember { mutableStateOf(false) }
    var moneyMode by remember { mutableStateOf("budget") }
    var showBudget by remember { mutableStateOf(false) }
    var showCustomize by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(initialShowLog) }
    /* One list per destination, so switching tabs returns you to where you were rather than
       to the top. */
    val todayList = rememberLazyListState()
    val spendingList = rememberLazyListState()
    val budgetList = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = HUB_TODAY) { HUB_TAB_COUNT }
    /* The tab highlight is its own state so a press brightens the icon the instant it lands.
       During a swipe the pager drives it, so the highlight tracks the finger; during a
       programmatic tab slide the pager passes through the pages it crosses, so those are ignored
       via [tabDriven] (the press already set the highlight). */
    var activeTab by remember { mutableStateOf(HUB_TODAY) }
    var tabDriven by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (!tabDriven && page != activeTab) activeTab = page
        }
    }

    /* Slide the pager to a tab, flagging it so the highlight isn't dragged through the pages the
       slide passes over. The flag is cleared in `finally`, so a swipe that interrupts the slide
       takes the highlight over cleanly. */
    fun goToTab(tab: Int) {
        activeTab = tab
        tabDriven = true
        scope.launch {
            try {
                pagerState.animateScrollToPage(tab)
            } finally {
                tabDriven = false
            }
        }
    }
    /* True while any screen or drawer is up. The Settings bubble becomes a back arrow and the
       glass refracts what is actually behind it. */
    val anyOverlayOpen = showLog || showCustomize || showBudget || showMoney
    /* Whether the bars (and the cards) are in a liquid-glass mode — the same condition the glass
       uses to decide whether to run the refraction shader at all. */
    val pillGlass = s.prefs.glassEnabled || s.prefs.glassScreens

    // Back closes the open screen/drawer; with none open it walks back to Today.
    BackHandler(enabled = anyOverlayOpen || activeTab != HUB_TODAY) {
        if (anyOverlayOpen) {
            showLog = false
            showCustomize = false
            showBudget = false
            showMoney = false
        } else {
            goToTab(HUB_TODAY)
        }
    }

    /* Keep the trip's exchange rate current. Only affects entries logged from now on. */
    LaunchedEffect(s.travelActive, s.travel.currency, s.cur, s.travel.rateAuto) {
        if (s.travelActive && s.travel.rateAuto) vm.refreshTravelRate(auto = true)
    }

    val bottomFade = 152.dp
    /* The (costlier) blur stays in the shorter band nearest the edge; only the colour fade
       reaches higher, so the darkening reads as a gradient above the floating pill. */
    val bottomBlurBand = 72.dp
    /* A full-screen view (Log a spend / Settings) covers the page entirely, so the page source
       is dropped while one is open — that removes a whole full-screen capture per frame, which is
       what made scrolling the glass Log screen stutter. The bar then samples the overlay it
       actually sits on. */
    val fullScreenOpen = showLog || showCustomize
    /* The page and overlays are captured as Haze sources so the bars (and the edge blur) sample
       the scene actually behind them, not just the wallpaper. Only registered when something
       reads them, so a plain solid-UI setup pays nothing for the capture. */
    val sceneSources = progressiveEdgeBlurSupported && (s.prefs.edgeBlur || pillGlass) && !fullScreenOpen
    val pageSource =
        if (sceneSources && sceneHaze != null) Modifier.hazeSource(sceneHaze, zIndex = 1f) else Modifier
    val overlaySource =
        if (pillGlass && sceneHaze != null) Modifier.hazeSource(sceneHaze, zIndex = 2f) else Modifier

    Box(Modifier.fillMaxSize()) {
        /* One full-height page per destination. The pages are full-bleed, so their cards scroll
           right up under the edge blur instead of being clipped by a fixed header. */
        Box(
            Modifier
                .fillMaxSize()
                .then(pageSource)
        ) {
            /* A pager instead of AnimatedContent. It keeps the neighbouring page composed, so the
               incoming destination is already built when you switch — the slide costs nothing in
               composition and stays smooth on slower devices. Swiping the pages drives the tab
               directly through [pagerState]. */
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = true,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                HubTabBody(
                    tab = page,
                    vm = vm,
                    s = s,
                    listState = when (page) {
                        HUB_SPENDING -> spendingList
                        HUB_BUDGET -> budgetList
                        else -> todayList
                    },
                    progressFrozen = anyOverlayOpen,
                    onMoveMoney = { moneyMode = "budget"; showMoney = true },
                    onEditBudget = { showBudget = true },
                    onEditEntry = { entry ->
                        vm.startEdit(entry)
                        showLog = true
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        /* ── Progressive blur over the page edges — after the page, before the bar ──
           Switched off entirely from Theme → Glass for users who prefer a clean edge. */
        if (s.prefs.edgeBlur) {
            /* The bottom fade is tall — the space the floating bar sits over — so the darkening
               contrasts against the pill. The top band is the status-bar inset plus the page's own
               clearance, since it also serves as that page's top padding. Keeping the *blur* short
               leaves a minimum of the page under a blur that trails the sharp content by a frame,
               which is what reads as a flicker. */
            ScreenEdgeBlur(
                hazeState = if (sceneSources) sceneHaze else null,
                topHeight = hubTopInset(),
                bottomHeight = bottomFade,
                blurHeight = bottomBlurBand,
            )
        }

        /* ── In-window overlays (drawn before the bar so the bar always floats above) ──
           Captured as one layer, so the bar's glass can refract the Settings and Log screens
           themselves and not just the page behind them. */
        Box(
            Modifier
                .fillMaxSize()
                .then(overlaySource)
        ) {
            ToastOverlay(
                toast = vm.toast,
                dotColor = when (vm.toast?.type) {
                    "success" -> parseColor(s.theme.positive) ?: cs.primary
                    "error" -> parseColor(s.theme.negative) ?: cs.error
                    else -> parseColor(s.theme.accent) ?: cs.primary
                },
                onDismiss = vm::dismissToast,
            )

            ConfirmDialog(vm.confirm)

            /* The dimming backdrop fades in place; only the sheet slides, so the scrim never
               sweeps up the screen as a hard-edged black band behind it. */
            AnimatedVisibility(visible = showMoney, enter = fadeIn(), exit = fadeOut()) {
                DrawerScrim { showMoney = false }
            }
            AnimatedVisibility(
                visible = showMoney,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                MoneyDrawer(vm, s, moneyMode, { moneyMode = it }, onClose = { showMoney = false })
            }
            AnimatedVisibility(visible = showBudget, enter = fadeIn(), exit = fadeOut()) {
                DrawerScrim { showBudget = false }
            }
            AnimatedVisibility(
                visible = showBudget,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                BudgetDrawer(vm, s, onClose = { showBudget = false })
            }
            AnimatedVisibility(
                visible = showCustomize,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                SettingsScreen(vm, s)
            }
            AnimatedVisibility(
                visible = showLog,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                LogScreen(vm, s, onClose = { showLog = false })
            }
        }   /* end of the captured overlay layer */

        /* Bottom bar, drawn above every overlay: a four-tab pill with the Log bubble beside it,
           plus a Settings bubble floating in a top corner. It samples [sceneBackdrop], which
           carries the overlays too, so the glass bends whatever it actually covers. */
        val closeAll = {
            showLog = false
            showCustomize = false
            showBudget = false
            showMoney = false
        }
        val leftHanded = s.prefs.leftHanded
        HubSettingsBubble(
            isBack = anyOverlayOpen,
            onClick = { if (anyOverlayOpen) closeAll() else showCustomize = true },
            sceneHaze = sceneHaze,
            modifier = Modifier
                /* Left-handed mode swaps the corner the bubble floats in. */
                .align(if (leftHanded) Alignment.TopStart else Alignment.TopEnd)
                .statusBarsPadding()
                .then(
                    if (leftHanded) Modifier.padding(start = 16.dp, top = 10.dp)
                    else Modifier.padding(end = 16.dp, top = 10.dp)
                ),
        )
        HubBottomBar(
            activeTab = activeTab,
            onSelectTab = { tab ->
                /* Switching destination leaves any open screen or drawer behind. */
                closeAll()
                goToTab(tab)
            },
            onLogSpend = { showLog = true },
            leftHanded = leftHanded,
            sceneHaze = sceneHaze,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp),
        )
    }
}
