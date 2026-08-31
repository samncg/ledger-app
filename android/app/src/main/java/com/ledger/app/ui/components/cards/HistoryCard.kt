package com.ledger.app.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.Bolt
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.HistoryData
import com.ledger.app.ui.HistoryEntry
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.AppTextField
import com.ledger.app.ui.components.CardContainer
import com.ledger.app.ui.components.CatChip
import com.ledger.app.ui.components.ChipFlow
import com.ledger.app.ui.components.DateField
import com.ledger.app.ui.components.EmptyState
import com.ledger.app.ui.components.FieldLabel
import com.ledger.app.ui.components.RangeTabs
import com.ledger.app.ui.parseColor
import com.ledger.app.util.fmt
import com.ledger.app.util.relativeDate

/* History — filterable, searchable, sortable transaction list */
@Composable
fun HistoryCard(
    vm: LedgerViewModel,
    s: LedgerState,
    expand: Boolean = false,
    onEditEntry: (HistoryEntry) -> Unit = { vm.startEdit(it) },
) {
    val cs = MaterialTheme.colorScheme
    var showFilters by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("date-desc") }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var filterCats by remember { mutableStateOf(listOf<String>()) }

    val data: HistoryData = remember(s, filterCats, search, dateFrom, dateTo, sort, s.prefs.groupHistory) {
        vm.history(s, filterCats, search, dateFrom, dateTo, sort, s.prefs.groupHistory)
    }
    val totalCount = s.expenses.size + s.topUps.size
    val positive = parseColor(s.theme.positive) ?: cs.primary
    val warning = parseColor(s.theme.warning) ?: cs.primary

    CardContainer(
        title = "History",
        icon = Icons.Outlined.History,
        count = if (totalCount > 0) "($totalCount)" else null,
        trailing = if (totalCount > 0) {
            {
                Text(
                    "Reset filters",
                    color = cs.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            filterCats = emptyList(); search = ""; dateFrom = ""; dateTo = ""; sort = "date-desc"
                        }
                        .padding(4.dp))
            }
        } else null,
    ) {
        ChipFlow {
            CatChip("All", null, filterCats.isEmpty()) { filterCats = emptyList() }
            s.cats.forEach { c ->
                CatChip(c.label, c.color, filterCats.contains(c.id)) { toggleCat(filterCats, c.id) { filterCats = it } }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (showFilters) "Hide" else "Sort & filter" + if (!showFilters && data.activeFilterCount > 0) " (${data.activeFilterCount})" else "",
                color = cs.primary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { showFilters = !showFilters }
                    .padding(4.dp),
            )
            Text("▾", color = cs.onSurfaceVariant, fontSize = 11.sp)
        }

        if (showFilters) {
            Spacer(Modifier.height(8.dp))
            AppTextField(
                value = search,
                onChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Search notes or categories…"
            )
            Spacer(Modifier.height(8.dp))
            RangeTabs(
                options = listOf(
                    "date-desc" to "Newest first",
                    "date-asc" to "Oldest first",
                    "amount-desc" to "Amount ↓",
                    "amount-asc" to "Amount ↑"
                ),
                selected = sort,
                onSelect = { sort = it },
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("From")
                    DateField(value = dateFrom, onChange = { dateFrom = it }, maxDate = dateTo.ifEmpty { s.today }, placeholder = "Start date")
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel("To")
                    DateField(value = dateTo, onChange = { dateTo = it }, maxDate = s.today, placeholder = "Max date")
                }
            }
            if (filterCats.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Transfers are hidden while a category filter is active.",
                    fontSize = 11.sp,
                    color = cs.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "${data.entries.size} ${if (data.entries.size == 1) "entry" else "entries"} · ${
                fmt(
                    data.spentTotal,
                    s.cur
                )
            } spent" +
                    if (data.toppedTotal != 0.0) " · ${if (data.toppedTotal > 0) "+" else ""}${
                        fmt(
                            data.toppedTotal,
                            s.cur
                        )
                    } ${if (s.balancesOn) "moved to budget" else "topped up"}" else "",
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(10.dp))
        if (data.entries.isEmpty()) {
            EmptyState(
                if (totalCount == 0) "◌" else "∅",
                if (totalCount == 0) "No spends yet. Add your first one above!" else "No entries match your filters.",
                if (totalCount == 0) "Data stays on your device." else "Try adjusting search or dates.",
            )
        } else {
            LazyColumn(if (expand) Modifier.fillMaxHeight() else Modifier.heightIn(max = 420.dp)) {
                data.groups.forEach { group ->
                    if (group.label != null) {
                        item(key = "h-${group.label}") {
                            Row(
                                Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    group.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = cs.onSurfaceVariant,
                                    letterSpacing = 0.3.sp
                                )
                                Text(
                                    fmt(group.total, s.cur),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = cs.onSurfaceVariant
                                )
                            }
                        }
                    }
                    items(group.items, key = { "${it.type}-${it.id}" }) { e ->
                        HistoryRow(
                            s,
                            e,
                            positive,
                            warning,
                            onEdit = { onEditEntry(e) },
                            onDuplicate = { vm.duplicateExpense(e.id) },
                            onDelete = { vm.removeExpense(e.id) },
                            onRemoveTopUp = { vm.removeTopUp(e.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    s: LedgerState,
    e: HistoryEntry,
    positive: Color,
    warning: Color,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onRemoveTopUp: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        if (e.type == "topup") {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (e.amount >= 0) positive.copy(alpha = 0.15f) else warning.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (e.amount >= 0) (if (s.balancesOn) Icons.Outlined.Wallet else Icons.Outlined.Bolt) else Icons.Outlined.Wallet,
                    null, Modifier.size(15.dp),
                    tint = if (e.amount >= 0) positive else warning,
                )
            }
        } else {
            val cat = s.cats.find { it.id == (e.categories.firstOrNull() ?: e.category) }
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
                    .background((parseColor(cat?.color) ?: cs.primary).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    cat?.glyph ?: "·",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = parseColor(cat?.color) ?: cs.primary
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (e.type == "topup")
                    if (e.amount >= 0) (if (s.balancesOn) "Move to budget" else "Top up") else "Return to balance"
                else (s.cats.find { it.id == (e.categories.firstOrNull() ?: e.category) }?.label ?: e.category ?: "—") +
                        if (e.categories.size > 1) " +${e.categories.size - 1}" else "",
                fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (e.note.isNotEmpty()) {
                Text(
                    e.note,
                    fontSize = 11.sp,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(relativeDate(e.date, s.today), fontSize = 10.5.sp, color = cs.onSurfaceVariant)
            Text(
                (if (e.type == "topup" && e.amount >= 0) "+" else "") + fmt(e.amount, s.cur),
                fontSize = 12.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
                color = if (e.type == "topup") (if (e.amount >= 0) positive else warning) else cs.onSurface,
            )
        }
        if (e.type == "topup") {
            IconButton(onClick = onRemoveTopUp, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Outlined.Delete, "Remove transfer", Modifier.size(15.dp), tint = cs.onSurfaceVariant)
            }
        } else {
            IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Outlined.Edit, "Edit", Modifier.size(14.dp), tint = cs.onSurfaceVariant)
            }
            IconButton(onClick = onDuplicate, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Outlined.ContentCopy, "Duplicate", Modifier.size(14.dp), tint = cs.onSurfaceVariant)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Outlined.Delete, "Delete", Modifier.size(15.dp), tint = cs.onSurfaceVariant)
            }
        }
    }
}

private fun toggleCat(current: List<String>, id: String, set: (List<String>) -> Unit) {
    set(if (current.contains(id)) current.filter { it != id } else current + id)
}
