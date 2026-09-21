package com.ledger.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.ledger.app.LedgerWidget
import com.ledger.app.WidgetRefresher
import com.ledger.app.ui.FONT_OPTIONS
import com.ledger.app.ui.HEAT_LEVELS
import com.ledger.app.ui.HEAT_PRESETS
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.PRESETS
import com.ledger.app.ui.Strings
import com.ledger.app.ui.activePresetKey
import com.ledger.app.ui.charts.PieChart
import com.ledger.app.ui.parseColor
import com.ledger.app.ui.t
import com.ledger.app.util.CURRENCIES
import com.ledger.app.util.fmt
import com.ledger.app.util.rateDecimals
import com.ledger.app.util.relativeDate

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/* ═══════════════════════════════════════════
   OVERLAYS — drawers, dialogs
   ═══════════════════════════════════════════ */

/* ─── Budget settings drawer ─── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDrawer(vm: LedgerViewModel, s: LedgerState, onClose: () -> Unit) {
    var budget by remember { mutableStateOf(if (s.settings != null) s.settings.monthlyBudget.toString() else "") }
    var days by remember { mutableStateOf(if (s.settings != null) s.settings.periodDays.toString() else "") }
    var startDate by remember { mutableStateOf(s.settings?.startDate ?: "") }
    var balance by remember { mutableStateOf(s.balance.start.toString()) }

    DrawerSheet(onClose) {
        DrawerHeader(t("drawer.budgetSettings"), onClose)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                FieldLabel(t("setup.monthlyBudget"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectField(
                        value = s.cur, modifier = Modifier.weight(1f),
                        options = CURRENCIES.toList().map { (k, v) -> k to "${v.symbol} $k" },
                        onChange = { newCur -> vm.updatePrefs { p -> p.copy(currency = newCur) } },
                    )
                    AppTextField(
                        value = budget,
                        onChange = { budget = it },
                        modifier = Modifier.weight(1.5f),
                        mono = true,
                        numeric = true
                    )
                }
            }
            if (s.balancesOn) {
                Column {
                    FieldLabel(t("drawer.bankBalance"))
                    AppTextField(
                        value = balance,
                        onChange = { balance = it },
                        modifier = Modifier.fillMaxWidth(),
                        mono = true,
                        numeric = true
                    )
                    Spacer(Modifier.height(4.dp))
                    SectionDesc(t("drawer.bankBalanceDesc"))
                }
                ToggleRow(
                    t("drawer.overspendFromBalance"),
                    t("drawer.overspendFromBalanceDesc"),
                    s.prefs.overspendFromBalance
                ) {
                    vm.updatePrefs { p -> p.copy(overspendFromBalance = it) }
                }
            }
            Column {
                FieldLabel(t("setup.periodDays"))
                AppTextField(
                    value = days,
                    onChange = { days = it },
                    modifier = Modifier.fillMaxWidth(),
                    mono = true,
                    numeric = true
                )
            }
            Column {
                FieldLabel(t("setup.startDate"))
                DateField(value = startDate, onChange = { startDate = it }, maxDate = s.today)
                TextButton(onClick = { startDate = com.ledger.app.util.firstOfMonthKey() }) {
                    Text(t("drawer.realignFirst"), fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(t("drawer.saveChanges"), onClick = {
                vm.saveSetup(budget, days, startDate, s.cur, balance)
                onClose()
            }, modifier = Modifier.weight(1f))
            Btn(t("app.cancel"), onClick = onClose, variant = "ghost")
        }
    }
}

/* ─── Move money / top up drawer ─── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyDrawer(vm: LedgerViewModel, s: LedgerState, mode: String, setMode: (String) -> Unit, onClose: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    DrawerSheet(onClose) {
        DrawerHeader(
            if (s.balancesOn) t("drawer.moneyTitle") else t("drawer.topUpBudgetTitle"),
            onClose,
            icon = if (s.balancesOn) Icons.Outlined.Wallet else Icons.Outlined.Bolt,
        )
        if (s.balancesOn) {
            RangeTabs(
                options = listOf(
                    "budget" to t("drawer.toBudget"),
                    "return" to t("drawer.toBalance"),
                    "add" to t("drawer.addBalance"),
                    "withdraw" to t("drawer.withdrawBalance")
                ),
                selected = mode,
                onSelect = setMode,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(t("drawer.balance"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    fmt(s.bankBalance, s.cur),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(t("drawer.after"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val delta = amount.toDoubleOrNull() ?: 0.0
                val after = if (mode == "return" || mode == "add") s.bankBalance + delta else s.bankBalance - delta
                Text(
                    fmt(after, s.cur),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column {
                FieldLabel(
                    when {
                        !s.balancesOn -> t("drawer.amountToAdd")
                        mode == "budget" -> t("drawer.amountToMoveToBudget")
                        mode == "return" -> t("drawer.amountToReturn")
                        mode == "withdraw" -> t("drawer.amountToWithdraw")
                        else -> t("drawer.amountToAdd")
                    },
                )
                AppTextField(
                    value = amount,
                    onChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    mono = true,
                    numeric = true,
                    placeholder = "50"
                )
            }
            if (!s.balancesOn || mode == "budget" || mode == "return") {
                Column {
                    FieldLabel(t("log.notePlaceholder"))
                    AppTextField(
                        value = note, onChange = { note = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = when {
                            !s.balancesOn -> t("drawer.noteExampleBonus")
                            mode == "return" -> t("drawer.noteExampleReturn")
                            else -> t("drawer.noteExampleExtra")
                        },
                    )
                }
            }
            SectionDesc(
                when {
                    !s.balancesOn -> t("drawer.descAddFunds")
                    mode == "budget" -> t("drawer.descToBudget")
                    mode == "return" -> t("drawer.descToBalance")
                    mode == "withdraw" -> t("drawer.descWithdraw")
                    else -> t("drawer.descAddBalance")
                },
            )
        }

        if ((!s.balancesOn || mode == "budget" || mode == "return") && s.topUps.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            FieldLabel(if (s.balancesOn) t("drawer.recentTransfers") else t("drawer.recentTopUps"))
            Spacer(Modifier.height(4.dp))
            s.topUps.asReversed().take(8).forEach { top ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (top.amount >= 0) (if (s.balancesOn) Icons.Outlined.Wallet else Icons.Outlined.Bolt) else Icons.Outlined.Wallet,
                            null,
                            Modifier.size(13.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (top.amount >= 0) fmt(top.amount, s.cur) else "-${fmt(Math.abs(top.amount), s.cur)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (top.note.isNotEmpty()) Text(
                            top.note,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        relativeDate(top.date, s.today),
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = { vm.removeTopUp(top.id) }) {
                        Icon(
                            Icons.Outlined.Delete,
                            t("log.remove"),
                            Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(
                when {
                    !s.balancesOn -> t("drawer.addFunds")
                    mode == "budget" -> t("history.moveToBudget")
                    mode == "return" -> t("history.returnToBalance")
                    mode == "withdraw" -> t("drawer.withdraw")
                    else -> t("drawer.addToBalance")
                },
                onClick = {
                    vm.submitMoney(mode, amount, note)
                    onClose()
                },
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Add,
            )
            Btn(t("app.cancel"), onClick = onClose, variant = "ghost")
        }
    }
}

/* ─── Customize drawer ─── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeDrawer(vm: LedgerViewModel, s: LedgerState, onClose: () -> Unit) {
    var tab by remember { mutableStateOf("theme") }

    DrawerSheet(onClose, contentHeight = 560.dp) {
        DrawerHeader(t("top.customize"), onClose)
        RangeTabs(
            options = listOf(
                "theme" to t("tab.theme"),
                "chart" to t("tab.chart"),
                "cats" to t("tab.cats"),
                "travel" to t("tab.travel"),
                "prefs" to t("tab.prefs")
            ),
            selected = tab,
            onSelect = { tab = it },
        )
        Spacer(Modifier.height(12.dp))

        when (tab) {
            "theme" -> ThemeTab(vm, s)
            "chart" -> ChartTab(vm, s)
            "cats" -> CatsTab(vm, s)
            "travel" -> TravelTab(vm, s)
            "prefs" -> PrefsTab(vm, s)
        }
    }
}

/**
 * A settings group hidden behind its own button. Collapsed by default so the drawer reads
 * as a short index instead of one long scroll; the open state is remembered per title.
 */
@Composable
private fun CollapsibleSection(
    title: String,
    initiallyOpen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by remember(title) { mutableStateOf(initiallyOpen) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .clickable { open = !open }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (open) "▴" else "▾",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (open) {
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
        }
    }
}

@Composable
private fun ThemeTab(vm: LedgerViewModel, s: LedgerState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Btn(
            t("drawer.toggleLightDark"),
            onClick = vm::toggleLightDark,
            variant = "ghost",
            modifier = Modifier.fillMaxWidth()
        )
        CollapsibleSection(t("sec.typography")) {
            SelectField(
                value = s.prefs.font, modifier = Modifier.fillMaxWidth(),
                options = FONT_OPTIONS.map { it.id to it.name },
                onChange = { vm.updatePrefs { p -> p.copy(font = it) } },
            )
        }
        CollapsibleSection(t("sec.presets")) {
            val activeKey = activePresetKey(s.theme, s.categories)
            val tick = rememberHapticTick()
            FlowRow2(spacedBy = 8.dp) {
                PRESETS.toList().forEach { (key, preset) ->
                    val active = key == activeKey
                    Column(
                        Modifier
                            .width(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { tick(); vm.applyPreset(key) }
                            .padding(10.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            listOf(preset.bg, preset.surface, preset.accent).forEach { c ->
                                Box(
                                    Modifier.size(18.dp).clip(RoundedCornerShape(4.dp))
                                        .background(parseColor(c) ?: Color.Transparent)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            key.replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
        CollapsibleSection(t("sec.interface")) {
            listOf(
                "bg" to t("drawer.colorBackground"),
                "surface" to t("drawer.colorSurface"),
                "accent" to t("drawer.colorAccent"),
                "accentFg" to t("drawer.colorAccentText"),
                "text" to t("drawer.colorText")
            ).forEach { (k, l) ->
                ColorRow(l, themeField(s, k)) { vm.updateColor(k, it) }
            }
        }
        CollapsibleSection(t("sec.status")) {
            listOf(
                "positive" to t("drawer.positiveUnder"),
                "warning" to t("drawer.warningNear"),
                "negative" to t("drawer.negativeOver")
            ).forEach { (k, l) ->
                ColorRow(l, themeField(s, k)) { vm.updateColor(k, it) }
            }

        }
        CollapsibleSection(t("drawer.wallpaper")) {
            val context = LocalContext.current
            val wallpaperPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let { vm.setWallpaperFromUri(context, it) }
            }

            if (!s.prefs.wallpaper.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    val bitmap = remember(s.prefs.wallpaper) {
                        try {
                            BitmapFactory.decodeFile(s.prefs.wallpaper)?.asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = t("drawer.wallpaperPreview"),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (s.prefs.wallBlur > 0) Modifier.blur(s.prefs.wallBlur.dp) else Modifier)
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                (parseColor(s.theme.bg) ?: Color.Black).copy(alpha = s.prefs.wallpaperDim / 100f)
                            )
                    )
                }

                SliderRow(
                    label = t("drawer.backgroundDim"),
                    valueText = "${s.prefs.wallpaperDim}%",
                    value = s.prefs.wallpaperDim.toFloat(),
                    range = 0f..90f,
                    steps = 17,
                    onValueChange = { vm.updateWallpaperDim(it.toInt()) }
                )
                SliderRow(
                    label = t("drawer.blurIntensity"),
                    valueText = "${s.prefs.wallBlur}dp",
                    value = s.prefs.wallBlur.toFloat(),
                    range = 0f..20f,
                    steps = 19,
                    onValueChange = { vm.updateWallBlur(it.toInt()) }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Btn(
                        t("drawer.replacePhoto"),
                        onClick = { wallpaperPicker.launch("image/*") },
                        variant = "secondary",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Upload
                    )
                    Btn(
                        t("log.remove"),
                        onClick = { vm.clearWallpaper(context) },
                        variant = "ghost",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Delete
                    )
                }
            } else {
                Btn(
                    t("drawer.setPhotoWallpaper"),
                    onClick = { wallpaperPicker.launch("image/*") },
                    variant = "secondary",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.Image
                )
                SectionDesc(t("drawer.wallpaperDesc"))
            }

        }
        CollapsibleSection(t("drawer.liquidGlass")) {
            ToggleRow(
                t("drawer.glassCards"),
                t("drawer.glassCardsDesc"),
                s.prefs.glassEnabled
            ) {
                vm.toggleGlass(it)
            }
            ToggleRow(
                t("drawer.glassScreens"),
                t("drawer.glassScreensDesc"),
                s.prefs.glassScreens
            ) {
                vm.updatePrefs { p -> p.copy(glassScreens = it) }
            }
            ToggleRow(
                t("drawer.glassInside"),
                t("drawer.glassInsideDesc"),
                s.prefs.glassScreensInside
            ) {
                vm.updatePrefs { p -> p.copy(glassScreensInside = it) }
            }
            if (s.prefs.glassEnabled || s.prefs.glassScreens) {
                SliderRow(
                    label = t("drawer.gaussianBlur"),
                    valueText = "${s.prefs.glassBlur}dp",
                    value = s.prefs.glassBlur.toFloat(),
                    range = 0f..24f,
                    steps = 23,
                    onValueChange = { vm.updateGlassBlur(it.toInt()) }
                )
                SliderRow(
                    label = t("drawer.transparency"),
                    valueText = "${s.prefs.glassOpacity}%",
                    value = s.prefs.glassOpacity.toFloat(),
                    range = 20f..100f,
                    steps = 15,
                    onValueChange = { vm.updateGlassOpacity(it.toInt()) }
                )
                SliderRow(
                    label = t("drawer.subCardOpacity"),
                    valueText = "${s.prefs.glassInnerOpacity}%",
                    value = s.prefs.glassInnerOpacity.toFloat(),
                    range = 0f..100f,
                    steps = 19,
                    onValueChange = { vm.updateGlassInnerOpacity(it.toInt()) }
                )
                SliderRow(
                    label = t("drawer.refractionHeight"),
                    valueText = "${s.prefs.glassRefractionHeight}dp",
                    value = s.prefs.glassRefractionHeight.toFloat(),
                    range = 0f..40f,
                    steps = 19,
                    onValueChange = { vm.updateGlassRefractionHeight(it.toInt()) }
                )
                SliderRow(
                    label = t("drawer.refractionAmount"),
                    valueText = "${s.prefs.glassRefraction}dp",
                    value = s.prefs.glassRefraction.toFloat(),
                    range = 0f..40f,
                    steps = 19,
                    onValueChange = { vm.updateGlassRefraction(it.toInt()) }
                )
                SliderRow(
                    label = t("drawer.chromaticAmount"),
                    valueText = "${s.prefs.glassChromaticAmount}%",
                    value = s.prefs.glassChromaticAmount.toFloat(),
                    range = 0f..100f,
                    steps = 19,
                    onValueChange = { vm.updateGlassChromaticAberration(it.toInt()) }
                )
                SectionDesc(t("drawer.chromaDesc"))
            }

        }

        CollapsibleSection(t("drawer.widgets.title")) {
            val context = LocalContext.current
            SectionDesc(t("drawer.widgets.desc"))
            FieldLabel(t("drawer.widgets.theme"))
            Spacer(Modifier.height(4.dp))
            RangeTabs(
                options = listOf("dark" to t("drawer.widgets.dark"), "light" to t("drawer.widgets.light")),
                selected = if (s.prefs.widgetDark) "dark" else "light",
                onSelect = { vm.updatePrefs { p -> p.copy(widgetDark = it == "dark") } },
            )
            // Re-render placed widgets as soon as the theme changes; both calls no-op
            // safely when nothing is on the home screen.
            LaunchedEffect(s.prefs.widgetDark) {
                LedgerWidget.refresh(context)
                WidgetRefresher.refreshNew(context)
            }
        }

        CollapsibleSection(t("drawer.screenEdges")) {
            ToggleRow(
                t("pref.edgeBlur"),
                t("drawer.edgeBlurDesc"),
                s.prefs.edgeBlur,
            ) { vm.updatePrefs { p -> p.copy(edgeBlur = it) } }
        }

        CollapsibleSection(t("sec.cardLayout")) {
            SectionDesc(t("drawer.cardLayoutDesc"))
            Btn(
                t("drawer.resetCardOrder"),
                onClick = vm::resetCardOrder,
                variant = "ghost",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ChartTab(vm: LedgerViewModel, s: LedgerState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(t("drawer.preview"))
        val previewSlices = remember(s.cats) {
            val pcts = listOf(35.0, 25.0, 25.0, 15.0)
            var cum = 0.0
            s.cats.take(4).mapIndexed { i, c ->
                val pct = pcts.getOrElse(i) { 10.0 }
                val slice = com.ledger.app.ui.PieSlice(
                    c.id,
                    c.label,
                    c.color,
                    pct,
                    pct,
                    pct,
                    cum
                )
                cum += pct
                slice
            }
        }
        PieChart(
            slices = previewSlices, thickness = s.prefs.pieThickness, gap = s.prefs.pieGap,
            centerValue = "", centerSub = "",
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        SliderRow(
            t("drawer.ringThickness"),
            String.format("%.1f", s.prefs.pieThickness),
            s.prefs.pieThickness,
            1f..8f
        ) {
            vm.updatePrefs { p -> p.copy(pieThickness = it) }
        }
        SliderRow(t("drawer.segmentGap"), String.format("%.1f", s.prefs.pieGap), s.prefs.pieGap, 0f..4f) {
            vm.updatePrefs { p -> p.copy(pieGap = it) }
        }
        SectionTitle(t("drawer.trendStyle"))
        RangeTabs(
            options = listOf("line" to t("drawer.lineChart"), "heatmap" to t("drawer.heatmap")),
            selected = s.prefs.trendStyle,
            onSelect = { vm.updatePrefs { p -> p.copy(trendStyle = it) } },
        )
        if (s.prefs.trendStyle == "heatmap") {
            SectionTitle(t("drawer.heatmapColors"))
            val tick = rememberHapticTick()
            FlowRow2(spacedBy = 8.dp) {
                HEAT_PRESETS.toList().forEach { (key, entry) ->
                    val (name, colors) = entry
                    val active = HEAT_LEVELS.all { k -> (s.prefs.heatColors[k] ?: "") == colors[k] }
                    Column(
                        Modifier
                            .width(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { tick(); vm.updatePrefs { p -> p.copy(heatColors = colors) } }
                            .padding(10.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            HEAT_LEVELS.forEach { k ->
                                Box(
                                    Modifier.size(13.dp).clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (colors[k] == "transparent") MaterialTheme.colorScheme.surfaceVariant else parseColor(
                                                colors[k]
                                            ) ?: Color.Transparent
                                        ),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(name, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            HEAT_LEVELS.forEach { k ->
                val current = s.prefs.heatColors[k] ?: "transparent"
                ColorRow(
                    if (k == "l0") t("drawer.emptyDays") else t("drawer.level", "n" to k.removePrefix("l")),
                    if (current == "transparent") t("drawer.none") else current,
                ) { vm.updatePrefs { p -> p.copy(heatColors = p.heatColors + (k to it)) } }
            }
            SectionDesc(t("drawer.emptyDaysDesc"))
        }
        SectionTitle(t("drawer.categoryColors"))
        SectionDesc(t("drawer.categoryColorsDesc"))
        s.cats.forEach { c ->
            ColorRow("${c.glyph} ${c.label}", c.color) { vm.updateCatColor(c.id, it) }
        }
    }
}

@Composable
private fun CatsTab(vm: LedgerViewModel, s: LedgerState) {
    var name by remember { mutableStateOf("") }
    var glyph by remember { mutableStateOf("★") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(t("drawer.yourCategories"))
        s.categories.forEach { c ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(c.glyph, fontSize = 15.sp, modifier = Modifier.width(28.dp))
                Text(c.label, Modifier.weight(1f), fontSize = 13.5.sp)
                if (s.categories.size > 1) {
                    IconButton(onClick = { vm.removeCategory(c.id) }) {
                        Icon(
                            Icons.Outlined.Delete,
                            t("log.remove"),
                            Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        SectionTitle(t("drawer.addCategory"))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(
                value = name,
                onChange = { name = it },
                modifier = Modifier.weight(1f),
                placeholder = t("drawer.categoryNamePlaceholder")
            )
            AppTextField(value = glyph, onChange = { glyph = it }, modifier = Modifier.width(56.dp), placeholder = "★")
            Btn("", onClick = { vm.addCategory(name, glyph); name = ""; glyph = "★" }, icon = Icons.Outlined.Add)
        }
        SectionDesc(t("drawer.categoryGlyphDesc"))
    }
}

@Composable
private fun TravelTab(vm: LedgerViewModel, s: LedgerState) {
    val tr = s.prefs.travel
    /* Entries keep the currency they were logged in, so a trip can hold several. */
    val rows = s.travelList
        .groupBy { it.currency ?: tr.currency }
        .map { (code, list) ->
            TravelCurrencyRow(code, list.sumOf { it.foreignAmount ?: it.amount }, list.sumOf { it.amount }, list.size)
        }
        .sortedByDescending { it.home }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        /* Travel mode sits at the top so the toggle stays reachable while a trip is inactive. */
        SectionDesc(t("travel.desc"))
        ToggleRow(
            t("travel.toggle"),
            if (s.travelActive) t("drawer.travelOn") else t("drawer.travelOff"),
            tr.active,
        ) { on -> vm.updatePrefs { p -> p.copy(travel = p.travel.copy(active = on)) } }
        if (tr.active) {
            FieldLabel(t("travel.tripName"))
            AppTextField(
                value = tr.name,
                onChange = { v -> vm.updatePrefs { p -> p.copy(travel = p.travel.copy(name = v)) } },
                placeholder = t("travel.tripNamePlaceholder"),
                modifier = Modifier.fillMaxWidth(),
            )
            FieldLabel(t("travel.currency"))
            SelectField(
                value = tr.currency, modifier = Modifier.fillMaxWidth(),
                options = CURRENCIES.toList().map { (k, v) -> k to "${v.symbol} $k — ${v.label}" },
                onChange = { v -> vm.updatePrefs { p -> p.copy(travel = p.travel.copy(currency = v)) } },
            )
            FieldLabel(t("travel.rate"))
            AppTextField(
                value = fmtRate(tr.rate),
                onChange = { v ->
                    val n = v.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                    vm.updatePrefs { p -> p.copy(travel = p.travel.copy(rate = n)) }
                },
                placeholder = t("drawer.ratePlaceholder"),
                mono = true, numeric = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SectionDesc(t("travel.rateNote", "foreign" to tr.currency))
            ToggleRow(t("travel.rateAuto"), t("travel.rateAutoDesc"), tr.rateAuto) { on ->
                vm.updatePrefs { p -> p.copy(travel = p.travel.copy(rateAuto = on)) }
            }
            Btn(
                if (vm.rateStatus == "loading") t("travel.rateFetching") else t("travel.refreshRate"),
                onClick = { vm.refreshTravelRate() },
                variant = "secondary",
                icon = Icons.Outlined.Bolt,
                modifier = Modifier.fillMaxWidth(),
                enabled = vm.rateStatus != "loading",
            )
            if (vm.rateStatus.isNotEmpty() && vm.rateStatus != "loading") {
                SectionDesc(
                    when {
                        vm.rateStatus == "error" -> t("travel.rateFailed")
                        vm.rateStatus.startsWith("ok:") ->
                            t("travel.rateAsOf", "date" to vm.rateStatus.removePrefix("ok:"))

                        else -> t("travel.rateUpdated")
                    }
                )
            }
            SectionDesc(t("travel.ratePrivacy"))
            FieldLabel(t("setup.startDate"))
            DateField(
                value = tr.start,
                onChange = { v -> vm.updatePrefs { p -> p.copy(travel = p.travel.copy(start = v)) } },
                maxDate = "",
            )
            Btn(t("travel.end"), onClick = vm::endTravel, variant = "ghost", modifier = Modifier.fillMaxWidth())
        }

        /* Home currency is every conversion's reference. */
        CollapsibleSection(t("travel.homeCurrency"), initiallyOpen = true) {
            SelectField(
                value = s.cur, modifier = Modifier.fillMaxWidth(),
                options = CURRENCIES.toList().map { (k, v) -> k to "${v.symbol} $k — ${v.label}" },
                onChange = { vm.updatePrefs { p -> p.copy(currency = it) } },
            )
            SectionDesc(t("travel.homeCurrencyDesc"))
        }

        CollapsibleSection(t("travel.currenciesUsed")) {
            SectionDesc(t("travel.currenciesUsedDesc"))
            if (rows.isEmpty()) SectionDesc(t("travel.noCurrencies"))
            rows.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        r.code, Modifier.width(46.dp), fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        fmt(r.foreign, r.code),
                        Modifier.weight(1f),
                        fontSize = 12.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "≈ ${fmt(r.home, s.cur)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("${r.count}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** One row of the per-currency breakdown shown in the Travel tab. */
private data class TravelCurrencyRow(val code: String, val foreign: Double, val home: Double, val count: Int)

/** Shows a rate with enough decimals that a small one doesn't collapse to "0.03". */
private fun fmtRate(rate: Double): String = String.format("%.${rateDecimals(rate)}f", rate)

@Composable
private fun PrefsTab(vm: LedgerViewModel, s: LedgerState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CollapsibleSection(t("sec.language"), initiallyOpen = true) {
            SelectField(
                value = s.prefs.lang, modifier = Modifier.fillMaxWidth(),
                options = Strings.LANGS,
                onChange = { vm.setLanguage(it) },
            )
            SectionDesc(t("drawer.languageDesc"))
        }

        CollapsibleSection(t("sec.streaks"), initiallyOpen = true) {
            SectionDesc(t("drawer.streaksDesc"))
            RangeTabs(
                options = listOf("0" to t("pref.graceOff"), "1" to t("drawer.oneDay"), "2" to t("drawer.twoDays")),
                selected = s.prefs.streakGrace.toString(),
                onSelect = { vm.updatePrefs { p -> p.copy(streakGrace = it.toIntOrNull() ?: 0) } },
            )
        }

        SectionTitle(t("sec.preferences"))
        ToggleRow(t("pref.compact"), t("pref.compactDesc"), s.prefs.compact) {
            vm.updatePrefs { p -> p.copy(compact = it) }
        }
        ToggleRow(
            t("pref.groupHistory"),
            t("pref.groupHistoryDesc"),
            s.prefs.groupHistory
        ) {
            vm.updatePrefs { p -> p.copy(groupHistory = it) }
        }
        SectionTitle(t("sec.balance"))
        ToggleRow(
            t("pref.balances"),
            if (s.balancesOn) t("pref.balancesOn")
            else t("pref.balancesOff"),
            s.balancesOn,
        ) { vm.updatePrefs { p -> p.copy(balancesEnabled = it) } }
        if (s.balancesOn) {
            FieldLabel(t("pref.heroShows"))
            Spacer(Modifier.height(4.dp))
            RangeTabs(
                options = listOf("daily" to t("pref.heroDaily"), "balance" to t("pref.heroBalance")),
                selected = s.heroMode,
                onSelect = { vm.updatePrefs { p -> p.copy(heroMode = it) } },
            )
        }
        SectionTitle(t("drawer.budget"))
        ToggleRow(
            t("drawer.overspendFromBalance"),
            t("drawer.overspendFromBalanceDesc"),
            s.prefs.overspendFromBalance
        ) {
            vm.updatePrefs { p -> p.copy(overspendFromBalance = it) }
        }

        SectionTitle(t("drawer.security"))
        ToggleRow(
            t("sec.appLock"),
            t("drawer.appLockDesc"),
            s.prefs.appLockEnabled,
        ) {
            vm.updatePrefs { p -> p.copy(appLockEnabled = it) }
        }

        SectionTitle(t("drawer.notifications"))
        val context = LocalContext.current
        ToggleRow(
            t("drawer.dailyReminder"),
            t("drawer.dailyReminderDesc"),
            s.prefs.notificationsEnabled
        ) {
            vm.toggleNotifications(it, context)
        }

        if (s.prefs.notificationsEnabled) {
            FieldLabel(t("drawer.reminderTime"))
            Spacer(Modifier.height(4.dp))
            val timeKey = "${s.prefs.reminderHour}:${s.prefs.reminderMinute}"
            val timeOptions = listOf(
                "19:0" to t("drawer.time1900"),
                "20:0" to t("drawer.time2000"),
                "21:0" to t("drawer.time2100"),
                "22:0" to t("drawer.time2200"),
                "12:0" to t("drawer.time1200"),
                "18:0" to t("drawer.time1800"),
            )
            SelectField(
                value = timeKey,
                options = timeOptions,
                onChange = { key ->
                    val parts = key.split(":")
                    val h = parts.getOrNull(0)?.toIntOrNull() ?: 20
                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    vm.setReminderTime(h, m, context)
                },
                modifier = Modifier.fillMaxWidth()
            )

            ToggleRow(
                t("drawer.budgetAlertWarnings"),
                t("drawer.budgetAlertWarningsDesc"),
                s.prefs.budgetAlertsEnabled
            ) {
                vm.toggleBudgetAlerts(it)
            }

            Btn(
                t("drawer.sendTestReminder"),
                onClick = { vm.testReminderNotification(context) },
                variant = "ghost",
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.Notifications
            )
        }

        SectionTitle(t("sec.cloudSync"))
        if (s.isFirebaseConfigured) {
            val authUser = s.authUser
            if (authUser != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        val initial =
                            (authUser.name?.firstOrNull() ?: authUser.email?.firstOrNull() ?: 'G').uppercaseChar()
                        Text(
                            text = initial.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = authUser.name ?: authUser.email ?: t("drawer.signedIn"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        val subText = if (s.syncError) {
                            if (s.syncErrorMsg.isNotEmpty()) s.syncErrorMsg else t("drawer.syncErrorRetry")
                        } else {
                            authUser.email ?: ""
                        }
                        Text(
                            text = subText,
                            fontSize = 11.5.sp,
                            color = if (s.syncError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        if (!s.syncError && authUser.uid.isNotEmpty()) {
                            Text(
                                text = t("drawer.accountId", "id" to authUser.uid.take(8)),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!s.syncError && s.lastSyncedAt > 0L) {
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(s.lastSyncedAt))
                            Text(
                                text = t("drawer.lastSynced", "time" to timeStr),
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Btn(t("drawer.signOut"), onClick = vm::signOutGoogle, variant = "ghost", small = true)
                }
            } else {
                val context = LocalContext.current
                val googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        val idToken = account?.idToken
                        if (idToken != null) {
                            vm.signInWithGoogleToken(idToken)
                        } else {
                            vm.showToast(t("app.googleIdTokenError"), "error")
                        }
                    } catch (e: ApiException) {
                        if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                            vm.showToast(t("app.signInCancelled"), "info")
                        } else {
                            vm.showToast(
                                t(
                                    "app.googleSignInError",
                                    "code" to e.statusCode,
                                    "message" to (e.localizedMessage ?: "")
                                ), "error"
                            )
                        }
                    }
                }

                Btn(
                    t("app.signInWithGoogle"),
                    onClick = {
                        findActivity(context)?.let { vm.signInGoogle(it, googleSignInLauncher) }
                    },
                    variant = "secondary",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.Cloud
                )
            }
            SectionDesc(
                if (authUser != null)
                    t("drawer.syncDescSignedIn")
                else
                    t("drawer.syncDescSignIn")
            )
        } else {
            SectionDesc(
                t("drawer.syncNeedsFirebase")
            )
        }
        SectionTitle(t("sec.shortcuts"))
        listOf(
            t("app.nav.logSpend") to t("drawer.shortcutLogSpendDesc"),
            t("history.title") to t("drawer.shortcutHistoryDesc"),
            t("drawer.lightDark") to t("drawer.shortcutLightDarkDesc"),
        ).forEach { (k, v) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(k, fontSize = 13.sp)
                Text(v, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        Btn(t("drawer.resetTheme"), onClick = vm::resetTheme, variant = "ghost", modifier = Modifier.fillMaxWidth())
    }
}

/* ─── Drawer scaffolding ─── */

@Composable
private fun DrawerSheet(
    onClose: () -> Unit,
    contentHeight: androidx.compose.ui.unit.Dp? = null,
    content: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    // Solid panel — liquid glass was laggy when scrolling here, so the drawer stays opaque.
    val panelModifier = Modifier.background(cs.surface, shape)
    Box(Modifier.fillMaxSize()) {
        // Dismiss scrim — tapping outside the panel closes the sheet.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose,
                )
        )
        // Bottom panel — consumes its own taps (no-op) so the scrim can't dismiss it.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .then(
                    if (contentHeight != null) Modifier.heightIn(min = 460.dp, max = contentHeight)
                    else Modifier.heightIn(min = 460.dp)
                )
                .clip(shape)
                .then(panelModifier)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {},
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 18.dp)
                    .padding(top = 14.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun DrawerHeader(
    title: String,
    onClose: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
        }
        Text(title, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowRow2(spacedBy: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacedBy),
        verticalArrangement = Arrangement.spacedBy(spacedBy),
    ) { content() }
}

private fun themeField(s: LedgerState, k: String): String = when (k) {
    "bg" -> s.theme.bg; "surface" -> s.theme.surface; "accent" -> s.theme.accent
    "accentFg" -> s.theme.accentFg; "text" -> s.theme.text
    "positive" -> s.theme.positive; "warning" -> s.theme.warning; "negative" -> s.theme.negative
    else -> ""
}

private fun findActivity(context: Context): Activity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
