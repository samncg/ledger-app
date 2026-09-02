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
import com.ledger.app.ui.FONT_OPTIONS
import com.ledger.app.ui.HEAT_LEVELS
import com.ledger.app.ui.HEAT_PRESETS
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.PRESETS
import com.ledger.app.ui.activePresetKey
import com.ledger.app.ui.charts.PieChart
import com.ledger.app.ui.parseColor
import com.ledger.app.util.CURRENCIES
import com.ledger.app.util.fmt
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
        DrawerHeader("Budget settings", onClose)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                FieldLabel("Monthly budget")
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
                    FieldLabel("Bank balance")
                    AppTextField(
                        value = balance,
                        onChange = { balance = it },
                        modifier = Modifier.fillMaxWidth(),
                        mono = true,
                        numeric = true
                    )
                    Spacer(Modifier.height(4.dp))
                    SectionDesc("Your bank balance. Transfers to the budget come out of this; leftover allowance is banked back into it.")
                }
                ToggleRow(
                    "Overspends come from balance",
                    "When you spend more than a day's allowance, take it out of your bank balance. Off = the overspend is covered by the monthly budget.",
                    s.prefs.overspendFromBalance
                ) {
                    vm.updatePrefs { p -> p.copy(overspendFromBalance = it) }
                }
            }
            Column {
                FieldLabel("Period length (days)")
                AppTextField(
                    value = days,
                    onChange = { days = it },
                    modifier = Modifier.fillMaxWidth(),
                    mono = true,
                    numeric = true
                )
            }
            Column {
                FieldLabel("Start date")
                DateField(value = startDate, onChange = { startDate = it }, maxDate = s.today)
                TextButton(onClick = { startDate = com.ledger.app.util.firstOfMonthKey() }) {
                    Text("Realign to 1st of this month", fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Save changes", onClick = {
                vm.saveSetup(budget, days, startDate, s.cur, balance)
                onClose()
            }, modifier = Modifier.weight(1f))
            Btn("Cancel", onClick = onClose, variant = "ghost")
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
            if (s.balancesOn) "Money" else "Top up budget",
            onClose,
            icon = if (s.balancesOn) Icons.Outlined.Wallet else Icons.Outlined.Bolt,
        )
        if (s.balancesOn) {
            RangeTabs(
                options = listOf(
                    "budget" to "To budget",
                    "return" to "To balance",
                    "add" to "Add balance",
                    "withdraw" to "Withdraw balance"
                ),
                selected = mode,
                onSelect = setMode,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Balance", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    fmt(s.bankBalance, s.cur),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("After", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        !s.balancesOn -> "Amount to add"
                        mode == "budget" -> "Amount to move to budget"
                        mode == "return" -> "Amount to return to balance"
                        mode == "withdraw" -> "Amount to withdraw"
                        else -> "Amount to add"
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
                    FieldLabel("Note (optional)")
                    AppTextField(
                        value = note, onChange = { note = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = when {
                            !s.balancesOn -> "e.g. bonus, birthday money"
                            mode == "return" -> "e.g. took out the extra food money"
                            else -> "e.g. extra cash for food"
                        },
                    )
                }
            }
            SectionDesc(
                when {
                    !s.balancesOn -> "Added to your total monthly budget — your daily allowance rises for the rest of the period."
                    mode == "budget" -> "Moved out of your balance into this month's budget — your daily allowance rises for the rest of the period."
                    mode == "return" -> "Moves money from your budget back to your balance — you can only take back what you moved in."
                    mode == "withdraw" -> "Removes money from your balance, e.g. to spend it elsewhere — it stays gone even if you stay under budget."
                    else -> "Money you add from outside the app — it raises your balance and is protected from spending."
                },
            )
        }

        if ((!s.balancesOn || mode == "budget" || mode == "return") && s.topUps.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            FieldLabel("Recent ${if (s.balancesOn) "transfers" else "top-ups"}")
            Spacer(Modifier.height(4.dp))
            s.topUps.asReversed().take(8).forEach { t ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (t.amount >= 0) (if (s.balancesOn) Icons.Outlined.Wallet else Icons.Outlined.Bolt) else Icons.Outlined.Wallet,
                            null,
                            Modifier.size(13.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (t.amount >= 0) fmt(t.amount, s.cur) else "-${fmt(Math.abs(t.amount), s.cur)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (t.note.isNotEmpty()) Text(
                            t.note,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        relativeDate(t.date, s.today),
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = { vm.removeTopUp(t.id) }) {
                        Icon(
                            Icons.Outlined.Delete,
                            "Remove",
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
                    !s.balancesOn -> "Add funds"
                    mode == "budget" -> "Move to budget"
                    mode == "return" -> "Return to balance"
                    mode == "withdraw" -> "Withdraw"
                    else -> "Add to balance"
                },
                onClick = {
                    vm.submitMoney(mode, amount, note)
                    onClose()
                },
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Add,
            )
            Btn("Cancel", onClick = onClose, variant = "ghost")
        }
    }
}

/* ─── Customize drawer ─── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeDrawer(vm: LedgerViewModel, s: LedgerState, onClose: () -> Unit) {
    var tab by remember { mutableStateOf("theme") }

    DrawerSheet(onClose, contentHeight = 560.dp) {
        DrawerHeader("Customize", onClose)
        RangeTabs(
            options = listOf("theme" to "Theme", "chart" to "Chart", "cats" to "Categories", "prefs" to "Prefs"),
            selected = tab,
            onSelect = { tab = it },
        )
        Spacer(Modifier.height(12.dp))

        when (tab) {
            "theme" -> ThemeTab(vm, s)
            "chart" -> ChartTab(vm, s)
            "cats" -> CatsTab(vm, s)
            "prefs" -> PrefsTab(vm, s)
        }
    }
}

@Composable
private fun ThemeTab(vm: LedgerViewModel, s: LedgerState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Btn("Toggle light / dark", onClick = vm::toggleLightDark, variant = "ghost", modifier = Modifier.fillMaxWidth())
        SectionTitle("Typography")
        SelectField(
            value = s.prefs.font, modifier = Modifier.fillMaxWidth(),
            options = FONT_OPTIONS.map { it.id to it.name },
            onChange = { vm.updatePrefs { p -> p.copy(font = it) } },
        )
        SectionTitle("Presets")
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
        SectionTitle("Interface")
        listOf(
            "bg" to "Background",
            "surface" to "Surface",
            "accent" to "Accent",
            "accentFg" to "Accent text",
            "text" to "Text"
        ).forEach { (k, l) ->
            ColorRow(l, themeField(s, k)) { vm.updateColor(k, it) }
        }
        SectionTitle("Status")
        listOf(
            "positive" to "Positive / Under",
            "warning" to "Warning / Near",
            "negative" to "Negative / Over"
        ).forEach { (k, l) ->
            ColorRow(l, themeField(s, k)) { vm.updateColor(k, it) }
        }

        SectionTitle("Wallpaper")
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
                        contentDescription = "Wallpaper preview",
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
                label = "Background dim",
                valueText = "${s.prefs.wallpaperDim}%",
                value = s.prefs.wallpaperDim.toFloat(),
                range = 0f..90f,
                steps = 17,
                onValueChange = { vm.updateWallpaperDim(it.toInt()) }
            )
            SliderRow(
                label = "Blur intensity",
                valueText = "${s.prefs.wallBlur}dp",
                value = s.prefs.wallBlur.toFloat(),
                range = 0f..20f,
                steps = 19,
                onValueChange = { vm.updateWallBlur(it.toInt()) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Btn(
                    "Replace photo",
                    onClick = { wallpaperPicker.launch("image/*") },
                    variant = "secondary",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Upload
                )
                Btn(
                    "Remove",
                    onClick = { vm.clearWallpaper(context) },
                    variant = "ghost",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Delete
                )
            }
        } else {
            Btn(
                "Set photo wallpaper",
                onClick = { wallpaperPicker.launch("image/*") },
                variant = "secondary",
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.Image
            )
            SectionDesc("Upload a custom photo for your dashboard background.")
        }

        SectionTitle("Liquid glass")
        ToggleRow(
            "Liquid glass cards",
            "Give the dashboard cards a frosted, translucent glass look over your wallpaper.",
            s.prefs.glassEnabled
        ) {
            vm.toggleGlass(it)
        }
        ToggleRow(
            "Liquid glass screens",
            "Give the Log a spend and History drawers a frosted glass backdrop.",
            s.prefs.glassScreens
        ) {
            vm.updatePrefs { p -> p.copy(glassScreens = it) }
        }
        ToggleRow(
            "Glass the inside cards",
            "Instead of the backdrop, make the Log a spend and History cards themselves liquid glass (on a flat light background).",
            s.prefs.glassScreensInside
        ) {
            vm.updatePrefs { p -> p.copy(glassScreensInside = it) }
        }
        if (s.prefs.glassEnabled || s.prefs.glassScreens) {
            SliderRow(
                label = "Gaussian blur",
                valueText = "${s.prefs.glassBlur}dp",
                value = s.prefs.glassBlur.toFloat(),
                range = 0f..24f,
                steps = 23,
                onValueChange = { vm.updateGlassBlur(it.toInt()) }
            )
            SliderRow(
                label = "Transparency",
                valueText = "${s.prefs.glassOpacity}%",
                value = s.prefs.glassOpacity.toFloat(),
                range = 20f..100f,
                steps = 15,
                onValueChange = { vm.updateGlassOpacity(it.toInt()) }
            )
            SliderRow(
                label = "Refraction height",
                valueText = "${s.prefs.glassRefractionHeight}dp",
                value = s.prefs.glassRefractionHeight.toFloat(),
                range = 0f..40f,
                steps = 19,
                onValueChange = { vm.updateGlassRefractionHeight(it.toInt()) }
            )
            SliderRow(
                label = "Refraction amount",
                valueText = "${s.prefs.glassRefraction}dp",
                value = s.prefs.glassRefraction.toFloat(),
                range = 0f..40f,
                steps = 19,
                onValueChange = { vm.updateGlassRefraction(it.toInt()) }
            )
            SliderRow(
                label = "Chromatic aberration amount",
                valueText = "${s.prefs.glassChromaticAmount}%",
                value = s.prefs.glassChromaticAmount.toFloat(),
                range = 0f..100f,
                steps = 19,
                onValueChange = { vm.updateGlassChromaticAberration(it.toInt()) }
            )
            SectionDesc("Chrom. aberration creates a prismatic RGB edge on the refraction; 0% = off. Applied to the bottom navigation pill and the dashboard cards. Lower transparency = more frosted.")
        }

        SectionTitle("Card layout")
        SectionDesc("Use the arrow buttons on each card to rearrange the order.")
        Btn("Reset card order", onClick = vm::resetCardOrder, variant = "ghost", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ChartTab(vm: LedgerViewModel, s: LedgerState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Preview")
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
        SliderRow("Ring thickness", String.format("%.1f", s.prefs.pieThickness), s.prefs.pieThickness, 1f..8f) {
            vm.updatePrefs { p -> p.copy(pieThickness = it) }
        }
        SliderRow("Segment gap", String.format("%.1f", s.prefs.pieGap), s.prefs.pieGap, 0f..4f) {
            vm.updatePrefs { p -> p.copy(pieGap = it) }
        }
        SectionTitle("Trend style")
        RangeTabs(
            options = listOf("line" to "Line chart", "heatmap" to "Heatmap"),
            selected = s.prefs.trendStyle,
            onSelect = { vm.updatePrefs { p -> p.copy(trendStyle = it) } },
        )
        if (s.prefs.trendStyle == "heatmap") {
            SectionTitle("Heatmap colors")
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
                    if (k == "l0") "Empty days" else "Level ${k.removePrefix("l")}",
                    if (current == "transparent") "none" else current,
                ) { vm.updatePrefs { p -> p.copy(heatColors = p.heatColors + (k to it)) } }
            }
            SectionDesc("\"Empty days\" is the base cell color — set to none for the default transparent look.")
        }
        SectionTitle("Category colors")
        SectionDesc("Controls pie chart, bar chart, and badges.")
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
        SectionTitle("Your categories")
        s.categories.forEach { c ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(c.glyph, fontSize = 15.sp, modifier = Modifier.width(28.dp))
                Text(c.label, Modifier.weight(1f), fontSize = 13.5.sp)
                if (s.categories.size > 1) {
                    IconButton(onClick = { vm.removeCategory(c.id) }) {
                        Icon(
                            Icons.Outlined.Delete,
                            "Remove",
                            Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        SectionTitle("Add category")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(
                value = name,
                onChange = { name = it },
                modifier = Modifier.weight(1f),
                placeholder = "Name (e.g. Health)"
            )
            AppTextField(value = glyph, onChange = { glyph = it }, modifier = Modifier.width(56.dp), placeholder = "★")
            Btn("", onClick = { vm.addCategory(name, glyph); name = ""; glyph = "★" }, icon = Icons.Outlined.Add)
        }
        SectionDesc("Use short symbols (◇ ★ ♥ ● ▲ ◐) for the icon.")
    }
}

@Composable
private fun PrefsTab(vm: LedgerViewModel, s: LedgerState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Preferences")
        ToggleRow("Compact density", "Tighter spacing throughout the app.", s.prefs.compact) {
            vm.updatePrefs { p -> p.copy(compact = it) }
        }
        ToggleRow(
            "Group history by date",
            "Show Today, Yesterday, This week, and monthly headers.",
            s.prefs.groupHistory
        ) {
            vm.updatePrefs { p -> p.copy(groupHistory = it) }
        }
        SectionTitle("Balance")
        ToggleRow(
            "Bank balance system",
            if (s.balancesOn) "On — keep a balance, move money to your budget, and bank leftover allowance at the end of each day."
            else "Off — plain budgeting without a balance or transfers.",
            s.balancesOn,
        ) { vm.updatePrefs { p -> p.copy(balancesEnabled = it) } }
        if (s.balancesOn) {
            FieldLabel("Hero shows")
            Spacer(Modifier.height(4.dp))
            RangeTabs(
                options = listOf("daily" to "Daily allowance", "balance" to "Balance"),
                selected = s.heroMode,
                onSelect = { vm.updatePrefs { p -> p.copy(heroMode = it) } },
            )
        }
        SectionTitle("Budget")
        ToggleRow(
            "Overspends come from balance",
            "When you spend more than a day's allowance, take it out of your bank balance. Off = the overspend is covered by the monthly budget.",
            s.prefs.overspendFromBalance
        ) {
            vm.updatePrefs { p -> p.copy(overspendFromBalance = it) }
        }

        SectionTitle("Currency")
        SelectField(
            value = s.cur, modifier = Modifier.fillMaxWidth(),
            options = CURRENCIES.toList().map { (k, v) -> k to "${v.symbol} $k — ${v.label}" },
            onChange = { vm.updatePrefs { p -> p.copy(currency = it) } },
        )

        SectionTitle("Notifications & reminders")
        val context = LocalContext.current
        ToggleRow(
            "Daily spend reminder",
            "Get an evening reminder to log your daily expenses.",
            s.prefs.notificationsEnabled
        ) {
            vm.toggleNotifications(it, context)
        }

        if (s.prefs.notificationsEnabled) {
            FieldLabel("Reminder time")
            Spacer(Modifier.height(4.dp))
            val timeKey = "${s.prefs.reminderHour}:${s.prefs.reminderMinute}"
            val timeOptions = listOf(
                "19:0" to "7:00 PM (19:00)",
                "20:0" to "8:00 PM (20:00)",
                "21:0" to "9:00 PM (21:00)",
                "22:0" to "10:00 PM (22:00)",
                "12:0" to "12:00 PM (12:00)",
                "18:0" to "6:00 PM (18:00)",
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
                "Budget alert warnings",
                "Notify when spending exceeds your daily allowance.",
                s.prefs.budgetAlertsEnabled
            ) {
                vm.toggleBudgetAlerts(it)
            }

            Btn(
                "Send test reminder",
                onClick = { vm.testReminderNotification(context) },
                variant = "ghost",
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.Notifications
            )
        }

        SectionTitle("Cloud sync")
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
                            text = authUser.name ?: authUser.email ?: "Signed in",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        val subText = if (s.syncError) {
                            if (s.syncErrorMsg.isNotEmpty()) s.syncErrorMsg else "Sync error — will retry automatically"
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
                                text = "Account ID ${authUser.uid.take(8)}…",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!s.syncError && s.lastSyncedAt > 0L) {
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(s.lastSyncedAt))
                            Text(
                                text = "Last synced $timeStr",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Btn("Sign out", onClick = vm::signOutGoogle, variant = "ghost", small = true)
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
                            vm.showToast("Couldn't retrieve Google ID token.", "error")
                        }
                    } catch (e: ApiException) {
                        if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                            vm.showToast("Sign-in cancelled.", "info")
                        } else {
                            vm.showToast("Google sign-in error (${e.statusCode}): ${e.localizedMessage ?: ""}", "error")
                        }
                    }
                }

                Btn(
                    "Sign in with Google",
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
                    "Your budget, balance, expenses, transfers, categories, theme and preferences sync automatically. The newest copy wins — edits from any device appear here."
                else
                    "Sign in to back up and sync your budget across devices with your Google account. Your data stays private — only you can read your copy."
            )
        } else {
            SectionDesc(
                "Sync is ready but needs a Firebase project. Open FirebaseConfig.kt, find the FIREBASE_CONFIG values, and paste your Firebase project config. Then enable Google sign-in and create a Firestore database with security rules."
            )
        }
        SectionTitle("Keyboard shortcuts")
        listOf(
            "Log spend" to "Tap the Log spend button",
            "History" to "Tap the History button",
            "Light / dark" to "Themes tab → Toggle light / dark",
        ).forEach { (k, v) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(k, fontSize = 13.sp)
                Text(v, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        Btn("Reset theme to default", onClick = vm::resetTheme, variant = "ghost", modifier = Modifier.fillMaxWidth())
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
                    if (contentHeight != null) Modifier.height(contentHeight)
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
