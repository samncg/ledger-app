package com.ledger.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.parseColor
import com.ledger.app.util.dateKeyFromMillis
import com.ledger.app.util.millisFromDateKey

/* ═══════════════════════════════════════════
   SHARED COMPONENTS
   ═══════════════════════════════════════════ */

/** Light tap-style haptic feedback, reused across interactive controls. */
@Composable
fun rememberHapticTick(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return remember(haptics) { { haptics.performHapticFeedback(HapticFeedbackType.LongPress) } }
}

/* ─── Buttons ─── */

@Composable
fun Btn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: String = "primary", // primary | secondary | ghost | danger
    icon: ImageVector? = null,
    enabled: Boolean = true,
    small: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val tick = rememberHapticTick()
    val colors = when (variant) {
        "secondary" -> ButtonDefaults.buttonColors(
            containerColor = cs.surfaceVariant,
            contentColor = cs.onSurfaceVariant
        )

        "ghost" -> ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = cs.onSurface)
        "danger" -> ButtonDefaults.buttonColors(containerColor = cs.error, contentColor = cs.onError)
        else -> ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary)
    }
    val content: @Composable () -> Unit = {
        if (icon != null) {
            Icon(icon, null, Modifier.size(if (small) 13.dp else 15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            fontSize = if (small) 12.5.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    val shape = RoundedCornerShape(10.dp)
    val padding = if (small) PaddingValues(horizontal = 16.dp, vertical = 4.dp) else ButtonDefaults.ContentPadding
    if (variant == "ghost") {
        OutlinedButton(
            onClick = { tick(); onClick() }, modifier, enabled = enabled, shape = shape, contentPadding = padding,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onSurface),
            border = BorderStroke(1.dp, cs.outline)
        ) { content() }
    } else {
        Button(
            onClick = { tick(); onClick() },
            modifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            contentPadding = padding
        ) { content() }
    }
}

/* ─── Text field ─── */

@Composable
fun AppTextField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    mono: Boolean = false,
    numeric: Boolean = false,
    onDone: (() -> Unit)? = null,
    readOnly: Boolean = false,
    label: String? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.height(48.dp),
        label = label?.let { { Text(it) } },
        placeholder = placeholder.ifEmpty { null }?.let { { Text(it) } },
        singleLine = true,
        readOnly = readOnly,
        trailingIcon = trailingIcon,
        textStyle = if (mono) TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.5.sp
        ) else TextStyle(fontSize = 13.sp),
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        keyboardActions = if (onDone != null) KeyboardActions(onDone = { onDone() }) else KeyboardActions.Default,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = cs.primary,
            unfocusedBorderColor = cs.outline,
            cursorColor = cs.primary,
        ),
    )
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier,
        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/* ─── Chips ─── */

@Composable
fun CatChip(label: String, dotColor: String?, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val dot = parseColor(dotColor) ?: MaterialTheme.colorScheme.primary
    val tick = rememberHapticTick()
    Surface(
        onClick = { tick(); onClick() }, modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (selected) dot.copy(alpha = 0.18f) else Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, if (selected) dot else MaterialTheme.colorScheme.outline),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(8.dp).background(dot, CircleShape))
            Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

/* ─── Sections ─── */

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
fun SectionDesc(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun ToggleRow(label: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val tick = rememberHapticTick()
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(desc, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = { tick(); onChange(it) })
    }
}

@Composable
fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp)
            Text(
                valueText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
    }
}

@Composable
fun ColorRow(label: String, hex: String, onChange: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val tick = rememberHapticTick()
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 13.sp)
        Text(
            hex,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.5.sp,
            color = cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))
        var open by remember { mutableStateOf(false) }
        Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(8.dp))
                .background(parseColor(hex) ?: Color.Transparent)
                .border(1.dp, cs.outline, RoundedCornerShape(8.dp))
                .clickable { tick(); open = true },
        )
        if (open) ColorPickerDialog(hex, onChange = { onChange(it); open = false }, onDismiss = { open = false })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(initial: String, onChange: (String) -> Unit, onDismiss: () -> Unit) {
    var hex by remember { mutableStateOf(initial) }
    val tick = rememberHapticTick()
    val swatches = listOf(
        "#000000",
        "#ffffff",
        "#ff5c5c",
        "#e8c15a",
        "#5bd488",
        "#5b9fd4",
        "#a68bfa",
        "#ff8a3d",
        "#ff5c93",
        "#7ec6d1",
        "#4a5568",
        "#c04a30"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick a color") },
        text = {
            Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    swatches.forEach { c ->
                        val selected = c.equals(hex, ignoreCase = true)
                        Box(
                            Modifier.size(30.dp).clip(CircleShape)
                                .background(parseColor(c) ?: Color.Transparent)
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    CircleShape
                                )
                                .clickable { tick(); hex = c },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                AppTextField(value = hex, onChange = { hex = it }, mono = true, placeholder = "#rrggbb")
            }
        },
        confirmButton = { TextButton(onClick = { if (parseColor(hex) != null) onChange(hex) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/* ─── Tabs & selects ─── */

@Composable
fun RangeTabs(
    options: List<Pair<String, String>>,
    selected: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val tick = rememberHapticTick()
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (k, l) ->
            val active = k == selected
            Surface(
                onClick = { tick(); onSelect(k) }, shape = RoundedCornerShape(8.dp),
                color = if (active) cs.primary else Color.Transparent,
                contentColor = if (active) cs.onPrimary else cs.onSurfaceVariant,
                border = if (active) null else BorderStroke(1.dp, cs.outline),
            ) {
                Text(
                    l,
                    Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun SelectField(
    value: String,
    options: List<Pair<String, String>>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false
) {
    var open by remember { mutableStateOf(false) }
    val tick = rememberHapticTick()
    Box(modifier) {
        AppTextField(
            value = options.firstOrNull { it.first == value }?.second ?: value,
            onChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, null, Modifier.size(18.dp)) },
        )
        if (!disabled) {
            Box(Modifier.matchParentSize().clickable { tick(); open = true })
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (k, l) ->
                    DropdownMenuItem(
                        text = { Text(l, fontSize = 12.5.sp) },
                        onClick = { tick(); onChange(k); open = false })
                }
            }
        }
    }
}

/* ─── Date field ─── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, maxDate: String? = null) {
    var open by remember { mutableStateOf(false) }
    val tick = rememberHapticTick()
    val maxMillis = maxDate?.let { millisFromDateKey(it) }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = millisFromDateKey(value),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    if (maxMillis != null && utcTimeMillis > maxMillis) return false
                    return true
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(dateKeyFromMillis(state.selectedDateMillis)); open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
    Box(modifier) {
        AppTextField(value = value, onChange = {}, modifier = Modifier.fillMaxWidth(), readOnly = true)
        Box(Modifier.matchParentSize().clickable { tick(); open = true })
    }
}

@Composable
fun SmallChip(text: String) {
    val cs = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(50), color = cs.surfaceVariant, contentColor = cs.onSurfaceVariant) {
        Text(
            text,
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/* ─── Card wrapper ─── */

@Composable
fun CardContainer(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    count: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cs.surface)
            .border(1.dp, cs.outline, RoundedCornerShape(18.dp))
            .padding(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) Icon(icon, null, Modifier.size(17.dp), tint = cs.primary)
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (count != null) Text(count, fontSize = 11.5.sp, color = cs.onSurfaceVariant)
            }
            trailing?.invoke()
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

/* ─── Toast overlay ─── */

@Composable
fun ToastOverlay(toast: com.ledger.app.ui.ToastMsg?, dotColor: Color, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = toast != null,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
    ) {
        toast?.let { t ->
            Surface(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = cs.surface,
                border = BorderStroke(1.dp, cs.outline),
                shadowElevation = 6.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).background(dotColor, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Text(t.msg, Modifier.weight(1f), fontSize = 13.sp)
                    if (t.action != null) {
                        Spacer(Modifier.width(10.dp))
                        TextButton(onClick = onDismiss) {
                            Text(
                                t.action.label,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ─── Confirm dialog ─── */

@Composable
fun ConfirmDialog(req: com.ledger.app.ui.ConfirmReq?) {
    if (req == null) return
    AlertDialog(
        onDismissRequest = req.onCancel,
        title = { Text(req.title, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
        text = { Text(req.msg, fontSize = 13.5.sp) },
        confirmButton = {
            TextButton(onClick = req.onConfirm) {
                Text(
                    "Confirm",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = { TextButton(onClick = req.onCancel) { Text("Cancel") } },
    )
}

/* ─── Empty state ─── */

@Composable
fun EmptyState(glyph: String, title: String, sub: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(glyph, fontSize = 34.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(title, fontSize = 13.sp)
        Text(sub, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
