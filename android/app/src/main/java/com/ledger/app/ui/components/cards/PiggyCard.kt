package com.ledger.app.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Wallet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.AppTextField
import com.ledger.app.ui.components.Btn
import com.ledger.app.ui.components.CardContainer
import com.ledger.app.ui.components.FieldLabel
import com.ledger.app.util.fmt
import kotlin.math.min

/* Piggy bank — savings goals with progress and multiple banks support */
@Composable
fun PiggyCard(vm: LedgerViewModel, s: LedgerState) {
    val cs = MaterialTheme.colorScheme
    val piggies = if (s.piggies.isNotEmpty()) s.piggies else listOf(com.ledger.app.data.Piggy())
    val activePiggy = s.activePiggy
    val activeId = activePiggy.id

    var isCreating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newTarget by remember { mutableStateOf("") }

    var isRenaming by remember { mutableStateOf(false) }
    var renameDraft by remember { mutableStateOf("") }

    var editingGoal by remember { mutableStateOf(false) }
    var goalDraft by remember { mutableStateOf("") }

    var addOpen by remember { mutableStateOf(false) }
    var piggyAmount by remember { mutableStateOf("") }

    var showCustomize by remember { mutableStateOf(false) }

    val saved = activePiggy.saved
    val target = activePiggy.target
    val pct = if (target > 0) min(100.0, saved / target * 100) else 0.0

    CardContainer(
        title = "Piggy banks",
        icon = Icons.Outlined.Wallet,
        count = if (piggies.size > 1) "${piggies.size}" else if (target > 0) "${Math.round(pct)}%" else null,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!editingGoal) {
                    TextButton(onClick = {
                        goalDraft = if (target > 0) target.toString() else ""
                        editingGoal = true
                    }) {
                        Text(if (target > 0) "Edit goal" else "Set goal", fontSize = 12.5.sp)
                    }
                }
                IconButton(
                    onClick = { showCustomize = !showCustomize },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.Palette,
                        contentDescription = "Customize",
                        modifier = Modifier.size(16.dp),
                        tint = if (showCustomize) cs.primary else cs.onSurfaceVariant
                    )
                }
            }
        },
    ) {
        /* ── Multiple Piggy Tabs ── */
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            piggies.forEach { p ->
                val isSelected = p.id == activeId
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) cs.primary else cs.surfaceVariant)
                        .clickable {
                            vm.selectPiggy(p.id)
                            isRenaming = false
                            editingGoal = false
                            addOpen = false
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            p.name.ifBlank { "Piggy bank" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) cs.onPrimary else cs.onSurface
                        )
                        Text(
                            fmt(p.saved, s.cur),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) cs.onPrimary.copy(alpha = 0.85f) else cs.onSurfaceVariant
                        )
                    }
                }
            }
            if (!isCreating) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .border(1.dp, cs.outlineVariant, CircleShape)
                        .clickable { isCreating = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(13.dp), tint = cs.onSurfaceVariant)
                        Text("New", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurfaceVariant)
                    }
                }
            }
        }

        /* ── Create New Piggy Form ── */
        if (isCreating) {
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceVariant)
                    .padding(12.dp)
            ) {
                Text("Create new piggy bank", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = newName,
                    onChange = { newName = it },
                    placeholder = "Name (e.g. Vacation fund)",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                AppTextField(
                    value = newTarget,
                    onChange = { newTarget = it },
                    placeholder = "Target goal (e.g. 500)",
                    mono = true,
                    numeric = true,
                    modifier = Modifier.fillMaxWidth(),
                    onDone = {
                        val tVal = newTarget.toDoubleOrNull() ?: 0.0
                        vm.addPiggy(newName, tVal)
                        newName = ""
                        newTarget = ""
                        isCreating = false
                    }
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Btn("Cancel", onClick = { isCreating = false; newName = ""; newTarget = "" }, variant = "ghost", small = true)
                    Spacer(Modifier.width(8.dp))
                    Btn("Create", onClick = {
                        val tVal = newTarget.toDoubleOrNull() ?: 0.0
                        vm.addPiggy(newName, tVal)
                        newName = ""
                        newTarget = ""
                        isCreating = false
                    }, small = true)
                }
            }
        }

        /* ── Piggy Header & Name ── */
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRenaming) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    AppTextField(
                        value = renameDraft,
                        onChange = { renameDraft = it },
                        placeholder = "Name",
                        modifier = Modifier.weight(1f),
                        onDone = {
                            if (renameDraft.isNotBlank()) vm.renamePiggy(activeId, renameDraft)
                            isRenaming = false
                        }
                    )
                    Spacer(Modifier.width(4.dp))
                    Btn("", onClick = {
                        if (renameDraft.isNotBlank()) vm.renamePiggy(activeId, renameDraft)
                        isRenaming = false
                    }, small = true, icon = Icons.Outlined.Check)
                    Btn("", onClick = { isRenaming = false }, variant = "ghost", small = true, icon = Icons.Outlined.Close)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        activePiggy.name.ifBlank { "Piggy bank" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                    IconButton(
                        onClick = { renameDraft = activePiggy.name; isRenaming = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, "Rename", modifier = Modifier.size(13.dp), tint = cs.onSurfaceVariant)
                    }
                }
            }
            if (piggies.size > 1 && !isRenaming) {
                IconButton(
                    onClick = { vm.deletePiggy(activeId) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Outlined.Delete, "Delete", modifier = Modifier.size(15.dp), tint = cs.error)
                }
            }
        }

        /* ── Piggy Display Stage ── */
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 96.dp, height = 80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(activePiggy.texture ?: "🐷", fontSize = 42.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                if (editingGoal) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTextField(
                            value = goalDraft,
                            onChange = { goalDraft = it },
                            mono = true,
                            numeric = true,
                            placeholder = "200",
                            modifier = Modifier.weight(1f),
                            onDone = { vm.savePiggyTarget(activeId, goalDraft); editingGoal = false },
                        )
                        Btn("", onClick = { vm.savePiggyTarget(activeId, goalDraft); editingGoal = false }, small = true, icon = Icons.Outlined.Check)
                        Btn("", onClick = { editingGoal = false }, variant = "ghost", small = true, icon = Icons.Outlined.Close)
                    }
                } else {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Saved",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.onSurfaceVariant
                            )
                            if (target > 0) {
                                Text(
                                    "Goal: ${fmt(target, s.cur)}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            fmt(saved, s.cur),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = cs.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(cs.surfaceVariant)) {
                    Box(
                        Modifier
                            .fillMaxWidth((pct / 100).toFloat().coerceIn(0f, 1f))
                            .height(7.dp)
                            .background(cs.primary, CircleShape),
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    if (target > 0) {
                        if (saved >= target) "Goal complete! 🎉" else "${fmt(target - saved, s.cur)} to go"
                    } else "Set a goal to track progress",
                    fontSize = 11.sp,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                if (addOpen) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTextField(
                            value = piggyAmount,
                            onChange = { piggyAmount = it },
                            mono = true,
                            numeric = true,
                            placeholder = "20",
                            modifier = Modifier.weight(1f),
                            onDone = { vm.depositPiggy(activeId, piggyAmount); piggyAmount = ""; addOpen = false },
                        )
                        Btn("Add", onClick = { vm.depositPiggy(activeId, piggyAmount); piggyAmount = ""; addOpen = false }, small = true)
                        Btn("", onClick = { addOpen = false }, variant = "ghost", small = true, icon = Icons.Outlined.Close)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Btn("Add", onClick = { addOpen = true }, variant = "secondary", small = true, icon = Icons.Outlined.Add)
                        Btn("Break", onClick = { vm.breakPiggy(activeId) }, variant = "ghost", small = true, enabled = saved > 0)
                    }
                }
            }
        }

        /* ── Customize Picture & Sound Section ── */
        if (showCustomize) {
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceVariant)
                    .padding(12.dp)
            ) {
                Text(
                    "Customize ${activePiggy.name.ifBlank { "Piggy" }}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FieldLabel("Picture / Icon")
                val emojis = listOf("🐷", "🏖️", "✈️", "🚗", "🏠", "💻", "🎮", "🎁", "🎓", "🏝️")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emojis.forEach { emo ->
                        val isEmoSelected = (activePiggy.texture ?: "🐷") == emo
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isEmoSelected) cs.primary.copy(alpha = 0.2f) else cs.surface)
                                .border(if (isEmoSelected) 2.dp else 1.dp, if (isEmoSelected) cs.primary else cs.outlineVariant, CircleShape)
                                .clickable { vm.updatePiggyTexture(activeId, if (emo == "🐷") null else emo) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emo, fontSize = 18.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                FieldLabel("Deposit sound")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val sounds = listOf("coin" to "Coin", "chime" to "Chime", "none" to "Mute")
                    sounds.forEach { (id, label) ->
                        val isSoundSelected = (activePiggy.soundId ?: "coin") == id
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSoundSelected) cs.primary else cs.surface)
                                .clickable { vm.updatePiggySound(activeId, id) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                label,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSoundSelected) cs.onPrimary else cs.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
