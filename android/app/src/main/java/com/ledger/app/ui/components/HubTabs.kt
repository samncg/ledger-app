package com.ledger.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.highlight.Highlight
import com.kashif_e.backdrop.shadow.Shadow
import com.ledger.app.ui.HistoryEntry
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.cards.AutoCard
import com.ledger.app.ui.components.cards.BackupCard
import com.ledger.app.ui.components.cards.BreakdownCard
import com.ledger.app.ui.components.cards.HistoryCard
import com.ledger.app.ui.components.cards.InsightsCard
import com.ledger.app.ui.components.cards.PiggyCard
import com.ledger.app.ui.components.cards.StreakCard
import com.ledger.app.ui.components.cards.TrendCard
import com.ledger.app.ui.t
import com.ledger.app.util.fmt

/* ═══════════════════════════════════════════
   HUB TABS — the experimental tabbed navigation
   Opt-in from Theme → Experimental. Four destinations — Today · Spending · Budget · History —
   on a glass pill, with the Log action and Settings as their own round glass bubbles. The
   per-stat cards are reused inside each destination; the navigation is what changed.
   ═══════════════════════════════════════════ */

/* ─── Shared liquid-glass recipe for the bottom bars ───
   The pill, the bubbles and the nav bar are the same material: the backdrop bends through the
   shape (refraction + optional dispersion) over a blur, with a faint light-projected rim.
   Composable so the container tint can be read through state in the draw lambda — the backdrop
   node's update path never invalidates its draw, so without that a theme toggle left the bar
   on the previous theme's tint until something else redrew it. */
@Composable
internal fun Modifier.glassBar(
    backdrop: Backdrop,
    shape: Shape,
    blurDp: Float,
    lensOn: Boolean,
    chromatic: Float,
    refraction: Float,
    refractionHeight: Float,
    containerColor: Color,
    shadowRadius: Dp = 8.dp,
): Modifier {
    val tint = rememberUpdatedState(containerColor)
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            blur(blurDp.dp.toPx())
            if (lensOn) {
                lens(
                    (refractionHeight * (1f + chromatic * 0.5f)).dp.toPx(),
                    (refraction * (1f + chromatic * 0.5f)).dp.toPx(),
                    depthEffect = true,
                    chromaticAberration = chromatic > 0f,
                )
            }
        },
        highlight = { Highlight.Default.copy(width = 0.75.dp, blurRadius = 3.dp, alpha = 0.30f) },
        shadow = { Shadow(radius = shadowRadius, color = Color.Black.copy(alpha = 0.22f)) },
        onDrawSurface = { drawRect(tint.value) },
    )
}

/* ─── Destinations ───
   Today · Spending · Budget · History. Log and Settings are actions, carried by their bubbles. */
const val HUB_TODAY = 0
const val HUB_SPENDING = 1
const val HUB_BUDGET = 2
const val HUB_HISTORY = 3
const val HUB_TAB_COUNT = 4

/** Clearance kept at the top of a page for the floating Settings bubble. */
val HUB_TOP_CLEARANCE = 64.dp

/** The pages themselves start much closer to the top — the bubble is meant to float over their
corner, which is why the tappable things in those cards sit at their foot. */
val HUB_PAGE_CLEARANCE = 16.dp

/** The gap the bar and its bubbles leave at the bottom of a page. */
val HUB_BOTTOM_INSET = 132.dp

/** The round bubbles are the same size as the pill's height. */
private val BUBBLE_SIZE = 58.dp

/** Room at the top of a page for the floating Settings bubble. */
@Composable
fun hubTopInset(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + HUB_PAGE_CLEARANCE

/** Everything the glass bars read from the shared style, resolved once per bar. */
private class BarGlass(
    val backdrop: Backdrop?,
    val blurDp: Float,
    val lensOn: Boolean,
    val chromatic: Float,
    val refraction: Float,
    val refractionHeight: Float,
    val containerColor: Color,
    /* Fallback fill for when the bar's glass is switched off: the tinted container is almost
       fully transparent by default, so an opaque surface is needed or the bar would vanish. */
    val solidColor: Color,
)

@Composable
private fun rememberBarGlass(sceneBackdrop: Backdrop?): BarGlass {
    val cs = MaterialTheme.colorScheme
    val glass = LocalGlassStyle.current
    return BarGlass(
        backdrop = if (glass.barGlass) sceneBackdrop ?: LocalGlassBackdrop.current else null,
        /* The bars and bubbles have their own blur, so tuning the cards doesn't drag them along. */
        blurDp = glass.barBlur.coerceIn(0, 24).toFloat(),
        /* They always refract: the bar's material should not depend on whether the cards have theirs on. */
        lensOn = true,
        chromatic = glass.chromaticAberration.coerceIn(0f, 1f),
        refraction = glass.refraction.coerceIn(0, 40).toFloat(),
        refractionHeight = glass.refractionHeight.coerceIn(0, 40).toFloat(),
        containerColor = cs.surface.copy(alpha = (glass.barOpacity.coerceIn(0, 100) / 100f) * 0.5f),
        solidColor = cs.surface,
    )
}

@Composable
private fun Modifier.barGlass(g: BarGlass, shape: Shape): Modifier =
    if (g.backdrop != null) {
        glassBar(
            backdrop = g.backdrop,
            shape = shape,
            blurDp = g.blurDp,
            lensOn = g.lensOn,
            chromatic = g.chromatic,
            refraction = g.refraction,
            refractionHeight = g.refractionHeight,
            containerColor = g.containerColor,
        )
    } else {
        clip(shape).background(g.solidColor)
    }

/* ─── The bottom bar: a four-tab pill with the Log bubble beside it ─── */
@Composable
fun HubBottomBar(
    activeTab: Int,
    onSelectTab: (Int) -> Unit,
    onLogSpend: () -> Unit,
    leftHanded: Boolean = false,
    sceneBackdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
) {
    val g = rememberBarGlass(sceneBackdrop)
    val pillShape = RoundedCornerShape(100.dp)
    val round = RoundedCornerShape(50)
    val barH = 58.dp
    val gap = 10.dp

    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        /* Four tabs plus the Log bubble have to fit the narrowest phone, so the slot width is
           measured against the room actually available — and kept tight, so the whole group
           stays compact and lands dead centre. */
        val segW = minOf(64.dp, (maxWidth - BUBBLE_SIZE - gap) / HUB_TAB_COUNT)

        val pill: @Composable () -> Unit = {
            Box(
                Modifier
                    .width(segW * HUB_TAB_COUNT)
                    .height(barH)
                    .barGlass(g, pillShape)
                    .clip(pillShape),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    HubSlot(Icons.Outlined.Home, t("hub.today"), activeTab == HUB_TODAY, segW) {
                        onSelectTab(HUB_TODAY)
                    }
                    HubSlot(Icons.Outlined.PieChart, t("hub.spending"), activeTab == HUB_SPENDING, segW) {
                        onSelectTab(HUB_SPENDING)
                    }
                    HubSlot(Icons.Outlined.Savings, t("hub.budget"), activeTab == HUB_BUDGET, segW) {
                        onSelectTab(HUB_BUDGET)
                    }
                    HubSlot(Icons.Outlined.History, t("history.title"), activeTab == HUB_HISTORY, segW) {
                        onSelectTab(HUB_HISTORY)
                    }
                }
            }
        }

        val logBubble: @Composable () -> Unit = {
            HubRoundBubble(
                icon = Icons.Outlined.Add,
                label = t("app.nav.logSpend"),
                tint = MaterialTheme.colorScheme.primary,
                size = BUBBLE_SIZE,
                glass = g,
                shape = round,
                onClick = onLogSpend,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            /* Left-handed mode mirrors the group so the Log bubble sits within reach of the left thumb. */
            if (leftHanded) {
                logBubble()
                pill()
            } else {
                pill()
                logBubble()
            }
        }
    }
}

/* ─── The Settings bubble (top right). It becomes a back arrow while a screen is open. ─── */
@Composable
fun HubSettingsBubble(
    isBack: Boolean,
    onClick: () -> Unit,
    sceneBackdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
) {
    val g = rememberBarGlass(sceneBackdrop)
    HubRoundBubble(
        icon = if (isBack) Icons.AutoMirrored.Outlined.ArrowBack else Icons.Outlined.Settings,
        label = if (isBack) t("app.close") else t("app.nav.settings"),
        tint = MaterialTheme.colorScheme.onSurface,
        /* Same size and same glass as the Log bubble. */
        size = BUBBLE_SIZE,
        glass = g,
        shape = RoundedCornerShape(50),
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun HubRoundBubble(
    icon: ImageVector,
    label: String,
    tint: Color,
    size: Dp,
    glass: BarGlass,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    Box(
        modifier
            .size(size)
            .barGlass(glass, shape)
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { tick(); onClick() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/* ─── A tab ───
   No selection plate: the active destination's icon and label simply keep the theme's text
   colour at full strength while the others fade back, so in a dark theme the active tab reads
   lighter and its neighbours darker — and the reverse in a light theme. */
@Composable
private fun HubSlot(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    slotWidth: Dp,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val tick = rememberHapticTick()
    val color = if (selected) cs.onSurface else cs.onSurface.copy(alpha = 0.55f)
    Column(
        Modifier
            .width(slotWidth)
            .height(58.dp)
            .clip(RoundedCornerShape(100.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { tick(); onClick() },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ─── Destination bodies ─── */
@Composable
fun HubTabBody(
    tab: Int,
    vm: LedgerViewModel,
    s: LedgerState,
    listState: LazyListState,
    progressFrozen: Boolean,
    onMoveMoney: () -> Unit,
    onEditBudget: () -> Unit,
    onEditEntry: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (tab) {
        HUB_SPENDING -> HubPage(listState, modifier) {
            item { BreakdownCard(vm, s) }
            item { TrendCard(vm, s) }
            item { InsightsCard(vm, s) }
        }

        HUB_BUDGET -> HubPage(listState, modifier) {
            item { AutoCard(vm, s) }
            item { PiggyCard(vm, s) }
            item { BackupCard(vm, s, onEditBudget, onMoveMoney) }
        }

        HUB_HISTORY -> {
            /* The list owns its own scroll, so it fills the page rather than sitting in one. */
            val top = hubTopInset()
            Column(
                modifier
                    .fillMaxSize()
                    .padding(top = top)
                    .padding(bottom = HUB_BOTTOM_INSET)
                    .padding(horizontal = 16.dp),
            ) {
                HistoryCard(vm, s, expand = true, onEditEntry = onEditEntry)
            }
        }

        else -> HubPage(listState, modifier) {
            if (s.travelActive) {
                /* Travel mode swaps the whole Today page for the trip's own. */
                item { TravelHero(s, onEnd = { vm.endTravel() }) }
                item { TravelListCard(vm, s) }
            } else {
                item { Hero(s, { fmt(it, s.cur) }, onMoveMoney, progressFrozen) }
                item { StreakCard(s) }
            }
        }
    }
}

@Composable
private fun HubPage(
    listState: LazyListState,
    modifier: Modifier,
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = hubTopInset(),
            bottom = HUB_BOTTOM_INSET,
        ),
        content = content,
    )
}
