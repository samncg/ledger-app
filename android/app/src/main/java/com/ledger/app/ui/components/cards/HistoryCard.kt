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
import com.ledger.app.ui.components.ReceiptPreviewDialog
import com.ledger.app.ui.components.ReceiptThumbnail
import com.ledger.app.ui.parseColor
import com.ledger.app.ui.t
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
    var filterTags by remember { mutableStateOf(listOf<String>()) }
    var previewReceipt by remember { mutableStateOf<String?>(null) }
    val allTags = remember(s.expenses) { s.expenses.flatMap { it.tags }.distinct().sorted() }

    val data: HistoryData = remember(s, filterCats, filterTags, search, dateFrom, dateTo, sort, s.prefs.groupHistory) {
        vm.history(s, filterCats, filterTags, search, dateFrom, dateTo, sort, s.prefs.groupHistory)
    }
    val totalCount = s.expenses.size + s.topUps.size
    val positive = parseColor(s.theme.positive) ?: cs.primary
    val warning = parseColor(s.theme.warning) ?: cs.primary

    CardContainer(
        title = t("history.title"),
        icon = Icons.Outlined.History,
        count = if (totalCount > 0) "($totalCount)" else null,
        trailing = if (totalCount > 0) {
            {
                Text(
                    t("history.resetFilters"),
                    color = cs.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            filterCats = emptyList(); filterTags = emptyList(); search = ""; dateFrom = ""; dateTo =
                            ""; sort = "date-desc"
                        }
                        .padding(4.dp))
            }
        } else null,
    ) {
        ChipFlow {
            CatChip(t("history.all"), null, filterCats.isEmpty()) { filterCats = emptyList() }
            s.cats.forEach { c ->
                CatChip(c.label, c.color, filterCats.contains(c.id)) { toggleCat(filterCats, c.id) { filterCats = it } }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (showFilters) t("history.hide") else t("history.sortFilter") + if (!showFilters && data.activeFilterCount > 0) " (${data.activeFilterCount})" else "",
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
                placeholder = t("history.search")
            )
            Spacer(Modifier.height(8.dp))
            RangeTabs(
                options = listOf(
                    "date-desc" to t("history.newest"),
                    "date-asc" to t("history.oldest"),
                    "amount-desc" to t("history.amountDown"),
                    "amount-asc" to t("history.amountUp")
                ),
                selected = sort,
                onSelect = { sort = it },
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel(t("history.from"))
                    DateField(
                        value = dateFrom,
                        onChange = { dateFrom = it },
                        maxDate = dateTo.ifEmpty { s.today },
                        placeholder = t("history.startDate")
                    )
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel(t("history.to"))
                    DateField(
                        value = dateTo,
                        onChange = { dateTo = it },
                        maxDate = s.today,
                        placeholder = t("history.maxDate")
                    )
                }
            }
            if (allTags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FieldLabel(t("history.tags"))
                ChipFlow {
                    CatChip(t("history.any"), null, filterTags.isEmpty()) { filterTags = emptyList() }
                    allTags.forEach { tg ->
                        CatChip("#$tg", null, filterTags.contains(tg)) {
                            filterTags = if (filterTags.contains(tg)) filterTags - tg else filterTags + tg
                        }
                    }
                }
            }
            if (filterCats.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    t("history.transfersHidden"),
                    fontSize = 11.sp,
                    color = cs.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "${data.entries.size} ${if (data.entries.size == 1) t("history.entry") else t("history.entries")} · ${
                fmt(
                    data.spentTotal,
                    s.cur
                )
            } ${t("history.spent")}" +
                    if (data.toppedTotal != 0.0) " · ${if (data.toppedTotal > 0) "+" else ""}${
                        fmt(
                            data.toppedTotal,
                            s.cur
                        )
                    } ${if (s.balancesOn) t("history.movedToBudget") else t("history.toppedUp")}" else "",
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(10.dp))
        if (data.entries.isEmpty()) {
            EmptyState(
                if (totalCount == 0) "◌" else "∅",
                if (totalCount == 0) t("history.noSpends") else t("history.noMatch"),
                if (totalCount == 0) t("history.dataStays") else t("history.tryAdjust"),
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
                            onRemoveTopUp = { vm.removeTopUp(e.id) },
                            onPreviewReceipt = { previewReceipt = e.receipt })
                    }
                }
            }
        }
    }

    ReceiptPreviewDialog(previewReceipt) { previewReceipt = null }
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
    onPreviewReceipt: () -> Unit,
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
                    if (e.amount >= 0) (if (s.balancesOn) t("history.moveToBudget") else t("history.topUp")) else t("history.returnToBalance")
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
            if (e.tags.isNotEmpty()) {
                Text(
                    e.tags.joinToString("  ") { "#$it" },
                    fontSize = 10.sp,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (e.receipt != null) {
            Spacer(Modifier.width(6.dp))
            ReceiptThumbnail(e.receipt, size = 30, onClick = onPreviewReceipt)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(relativeDate(e.date, s.today), fontSize = 10.5.sp, color = cs.onSurfaceVariant)
            if (e.currency != null && e.foreignAmount != null) {
                /* Logged abroad: the foreign figure it was entered in, over the home equivalent. */
                Text(
                    fmt(e.foreignAmount, e.currency),
                    fontSize = 12.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                )
                Text(
                    "≈ ${fmt(e.amount, s.cur)}",
                    fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, color = cs.onSurfaceVariant,
                )
            } else {
                Text(
                    (if (e.type == "topup" && e.amount >= 0) "+" else "") + fmt(e.amount, s.cur),
                    fontSize = 12.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold,
                    color = if (e.type == "topup") (if (e.amount >= 0) positive else warning) else cs.onSurface,
                )
            }
        }
        if (e.type == "topup") {
            IconButton(onClick = onRemoveTopUp, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Outlined.Delete,
                    t("common.removeTransfer"),
                    Modifier.size(15.dp),
                    tint = cs.onSurfaceVariant
                )
            }
        } else {
            IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Outlined.Edit, t("common.edit"), Modifier.size(14.dp), tint = cs.onSurfaceVariant)
            }
            IconButton(onClick = onDuplicate, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    t("common.duplicate"),
                    Modifier.size(14.dp),
                    tint = cs.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Outlined.Delete, t("common.delete"), Modifier.size(15.dp), tint = cs.onSurfaceVariant)
            }
        }
    }
}

private fun toggleCat(current: List<String>, id: String, set: (List<String>) -> Unit) {
    set(if (current.contains(id)) current.filter { it != id } else current + id)
}
