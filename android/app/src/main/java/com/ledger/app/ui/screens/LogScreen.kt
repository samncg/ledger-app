package com.ledger.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.app.ui.HistoryEntry
import com.ledger.app.ui.LedgerState
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.components.cards.HistoryCard
import com.ledger.app.ui.components.cards.LogCard

/* ─── Log a spend — full-screen view behind the top-bar button ─── */
@Composable
fun LogScreen(vm: LedgerViewModel, s: LedgerState, onClose: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(cs.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader("Log a spend", onClose)
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                LogCard(vm, s)
            }
        }
    }
}

/* ─── Spending history — full-screen view behind the top-bar button ─── */
@Composable
fun HistoryScreen(
    vm: LedgerViewModel,
    s: LedgerState,
    onClose: () -> Unit,
    onEditEntry: (HistoryEntry) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(cs.background)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader("History", onClose)
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).weight(1f),
            ) {
                HistoryCard(vm, s, expand = true, onEditEntry = onEditEntry)
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f).padding(start = 8.dp), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, "Close", Modifier.size(18.dp))
        }
    }
}
