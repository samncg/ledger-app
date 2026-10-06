package com.ledger.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kashif_e.backdrop.backdrops.emptyBackdrop
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberCombinedBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.BudgetDrawer
import com.ledger.app.ui.components.ConfirmDialog
import com.ledger.app.ui.components.DrawerScrim
import com.ledger.app.ui.components.HUB_BUDGET
import com.ledger.app.ui.components.HUB_TODAY
import com.ledger.app.ui.components.HUB_SPENDING
import com.ledger.app.ui.components.HubBottomBar
import com.ledger.app.ui.components.HubSettingsBubble
import com.ledger.app.ui.components.HubTabBody
import com.ledger.app.ui.components.LocalGlassBackdrop
import com.ledger.app.ui.components.MoneyDrawer
import com.ledger.app.ui.components.ScreenEdgeBlur
import com.ledger.app.ui.components.SettingsScreen
import com.ledger.app.ui.components.ToastOverlay
import com.ledger.app.ui.components.hubTopInset
import com.ledger.app.ui.components.progressiveEdgeBlurSupported
import com.ledger.app.ui.parseColor

/* ═══════════════════════════════════════════
   DASHBOARD — the tabbed hub: Today · Spending · Budget · History,
   with the Log and Settings bubbles floating over it, plus the overlays.
   ═══════════════════════════════════════════ */

@Composable
fun DashboardScreen(vm: LedgerViewModel, s: LedgerState, initialShowLog: Boolean = false) {
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
    var activeTab by remember { mutableStateOf(HUB_TODAY) }
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
            activeTab = HUB_TODAY
        }
    }

    /* Keep the trip's exchange rate current. Only affects entries logged from now on. */
    LaunchedEffect(s.travelActive, s.travel.currency, s.cur, s.travel.rateAuto) {
        if (s.travelActive && s.travel.rateAuto) vm.refreshTravelRate(auto = true)
    }

    /* The pages are captured into this layer so the top/bottom edges can be blurred over them. */
    val contentBackdrop = rememberLayerBackdrop()
    /* Everything drawn above the page and below the bar — the Settings and Log screens, the
       drawers, toasts — is captured here too, so the bar's glass bends whatever it actually
       covers rather than only the wallpaper. */
    val overlayBackdrop = rememberLayerBackdrop()
    val bottomBlur = 72.dp
    /* The scene behind the bar, in paint order: the wallpaper, then the page, then the overlays. */
    val sceneBackdrop = rememberCombinedBackdrop(
        LocalGlassBackdrop.current ?: emptyBackdrop(),
        contentBackdrop,
        overlayBackdrop,
    )

    Box(Modifier.fillMaxSize()) {
        /* One full-height page per destination. The pages are full-bleed, so their cards scroll
           right up under the edge blur instead of being clipped by a fixed header. */
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (progressiveEdgeBlurSupported && (s.prefs.edgeBlur || pillGlass))
                        Modifier.layerBackdrop(contentBackdrop)
                    else Modifier
                )
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    /* Slide in the direction of travel while fading across — no bounce. */
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { full -> dir * full / 5 } + fadeIn(tween(200))) togetherWith
                            (slideOutHorizontally { full -> -dir * full / 5 } + fadeOut(tween(160)))
                },
                label = "hub-page",
            ) { tab ->
                HubTabBody(
                    tab = tab,
                    vm = vm,
                    s = s,
                    listState = when (tab) {
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
            /* The bottom band is 72dp — the space the floating bar sits over. The top band is the
               status-bar inset plus the page's own clearance, since it also serves as that page's
               top padding. Keeping both short leaves a minimum of the page under a blur that trails
               the sharp content by a frame, which is what reads as a flicker. */
            ScreenEdgeBlur(
                backdrop = contentBackdrop,
                topHeight = hubTopInset(),
                bottomHeight = bottomBlur,
            )
        }

        /* ── In-window overlays (drawn before the bar so the bar always floats above) ──
           Captured as one layer, so the bar's glass can refract the Settings and Log screens
           themselves and not just the page behind them. */
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (progressiveEdgeBlurSupported) Modifier.layerBackdrop(overlayBackdrop)
                    else Modifier
                )
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
            sceneBackdrop = sceneBackdrop,
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
                activeTab = tab
            },
            onLogSpend = { showLog = true },
            leftHanded = leftHanded,
            sceneBackdrop = sceneBackdrop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp),
        )
    }
}
