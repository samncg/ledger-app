package com.ledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.data.dashboardCardOrder
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.BudgetDrawer
import com.ledger.app.ui.components.ConfirmDialog
import com.ledger.app.ui.components.CustomizeDrawer
import com.ledger.app.ui.components.HealthBadge
import com.ledger.app.ui.components.Hero
import com.ledger.app.ui.components.MoneyDrawer
import com.ledger.app.ui.components.ToastOverlay
import com.ledger.app.ui.components.TopBar
import com.ledger.app.ui.components.cards.AutoCard
import com.ledger.app.ui.components.cards.BackupCard
import com.ledger.app.ui.components.cards.BreakdownCard
import com.ledger.app.ui.components.cards.PiggyCard
import com.ledger.app.ui.components.cards.TrendCard
import com.ledger.app.ui.parseColor
import com.ledger.app.util.fmt

/* ═══════════════════════════════════════════
   DASHBOARD — hero + reorderable cards + overlays
   ═══════════════════════════════════════════ */

@Composable
fun DashboardScreen(vm: LedgerViewModel, s: LedgerState) {
    val cs = MaterialTheme.colorScheme
    val myr: (Double) -> String = { fmt(it, s.cur) }

    var showMoney by remember { mutableStateOf(false) }
    var moneyMode by remember { mutableStateOf("budget") }
    var showBudget by remember { mutableStateOf(false) }
    var showCustomize by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val cardOrder = dashboardCardOrder(s.prefs.cardOrder, s.balancesOn)
    val cardLabels = mapOf(
        "breakdown" to "Category breakdown", "trend" to "Spending trend",
        "auto" to "Automations", "piggy" to "Piggy bank", "backup" to "Data & backup",
    )

    Box(Modifier.fillMaxSize().background(cs.background)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                onLogSpend = { showLog = true },
                onOpenHistory = { showHistory = true },
                onOpenDrawer = { showCustomize = true },
            )
            LazyColumn(
                Modifier.fillMaxSize().navigationBarsPadding(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                item(key = "hero") {
                    Hero(
                        s,
                        myr,
                        onMoveMoney = { moneyMode = "budget"; showMoney = true }) { HealthBadge(s) }
                }
                items(count = cardOrder.size, key = { cardOrder[it] }) { index ->
                    val id = cardOrder[index]
                    Column {
                        /* reorder bar */
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                cardLabels[id] ?: id,
                                fontSize = 10.sp,
                                color = cs.onSurfaceVariant,
                                letterSpacing = 0.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row {
                                IconButton(
                                    onClick = { vm.moveCard(id, -1) },
                                    enabled = index > 0,
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.KeyboardArrowUp,
                                        "Move up",
                                        Modifier.size(16.dp),
                                        tint = if (index > 0) cs.onSurfaceVariant else cs.outlineVariant
                                    )
                                }
                                IconButton(
                                    onClick = { vm.moveCard(id, 1) },
                                    enabled = index < cardOrder.size - 1,
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.KeyboardArrowDown,
                                        "Move down",
                                        Modifier.size(16.dp),
                                        tint = if (index < cardOrder.size - 1) cs.onSurfaceVariant else cs.outlineVariant
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        when (id) {
                            "breakdown" -> BreakdownCard(vm, s)
                            "trend" -> TrendCard(vm, s)
                            "auto" -> AutoCard(vm, s)
                            "piggy" -> PiggyCard(vm, s)
                            "backup" -> BackupCard(
                                vm,
                                s,
                                onEditBudget = { showBudget = true },
                                onMoveMoney = { moneyMode = "budget"; showMoney = true })
                        }
                    }
                }
            }
        }

        /* ── Overlays ── */
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

        if (showMoney) {
            MoneyDrawer(vm, s, moneyMode, { moneyMode = it }, onClose = { showMoney = false })
        }
        if (showBudget) {
            BudgetDrawer(vm, s, onClose = { showBudget = false })
        }
        if (showCustomize) {
            CustomizeDrawer(vm, s, onClose = { showCustomize = false })
        }
        if (showLog) {
            LogScreen(vm, s, onClose = { showLog = false })
        }
        if (showHistory) {
            HistoryScreen(
                vm, s,
                onClose = { showHistory = false },
                onEditEntry = { entry ->
                    vm.startEdit(entry)
                    showHistory = false
                    showLog = true
                },
            )
        }
    }
}
