package com.ledger.app.ui

import com.ledger.app.data.Expense

/* ═══════════════════════════════════════════
   SMART CATEGORY SUGGESTIONS
   Built-in keyword heuristics, improved by a
   memory learned from the user's own history.
   ═══════════════════════════════════════════ */

/** Category id → lowercase keywords. First match wins (top to bottom). */
val CATEGORY_KEYWORDS: List<Pair<String, List<String>>> = listOf(
    "food" to listOf(
        "rice", "latte", "milk", "bread", "coffee", "matcha", "curry", "poke", "subway", "candy", "water",
    ),
    "transport" to listOf(
        "erl", "mrt", "lrt", "grab", "ride", "bus", "toll", "petrol",
    ),
    "shopping" to listOf(
        "cable", "usb", "mr diy", "stationary", "paper", "print", "drive",
    ),
    "other" to listOf(
        "laundry", "deepseek", "subscription", "bill",
    ),
)

/** Learned note→category (exact) and token→category maps from the user's expenses. */
data class CatMemory(
    val notes: Map<String, String> = emptyMap(),
    val tokens: Map<String, String> = emptyMap(),
)

private val TOKEN_SPLIT = Regex("[^a-z0-9]+")
private val WS = Regex("\\s+")

private fun tokenize(note: String): List<String> = note.split(TOKEN_SPLIT).filter { it.isNotEmpty() }

private class Vote(var count: Int = 0, var last: String = "")

private fun voteInto(
    map: MutableMap<String, MutableMap<String, Vote>>,
    key: String,
    cat: String,
    date: String,
) {
    if (key.isEmpty() || cat.isEmpty()) return
    val slot = map.getOrPut(key) { mutableMapOf() }
    val v = slot.getOrPut(cat) { Vote() }
    v.count++
    if (date > v.last) v.last = date
}

/** Most-used category per key; ties broken by most recent date. */
private fun resolve(map: Map<String, Map<String, Vote>>): Map<String, String> {
    val out = mutableMapOf<String, String>()
    for ((key, slot) in map) {
        var bestCat: String? = null
        var bestCount = -1
        var bestLast = ""
        for ((cat, v) in slot) {
            if (v.count > bestCount || (v.count == bestCount && v.last > bestLast)) {
                bestCount = v.count
                bestLast = v.last
                bestCat = cat
            }
        }
        if (bestCat != null) out[key] = bestCat
    }
    return out
}

/** Build the note/token → category memory from past expenses. */
fun buildCategoryMemory(expenses: List<Expense>): CatMemory {
    val notes = mutableMapOf<String, MutableMap<String, Vote>>()
    val tokens = mutableMapOf<String, MutableMap<String, Vote>>()
    for (e in expenses) {
        val cat = (e.categories.firstOrNull() ?: e.category) ?: continue
        val n = e.note.trim().lowercase().replace(WS, " ")
        if (n.isEmpty()) continue
        voteInto(notes, n, cat, e.date)
        for (t in tokenize(n)) voteInto(tokens, t, cat, e.date)
    }
    return CatMemory(notes = resolve(notes), tokens = resolve(tokens))
}

/**
 * Suggest a category from a note: a note you've logged exactly before wins, then
 * vocabulary learned from your history, then the built-in keywords. Only ever returns
 * a category you actually have, so a deleted category is never suggested.
 */
fun suggestCategory(note: String, available: Set<String>, memory: CatMemory? = null): String? {
    val n = note.trim().lowercase()
    if (n.isEmpty()) return null
    fun ok(id: String) = available.isEmpty() || available.contains(id)

    memory?.notes?.get(n)?.let { if (ok(it)) return it }

    memory?.tokens?.let { learned ->
        val hits = mutableMapOf<String, Int>()
        for (t in tokenize(n)) learned[t]?.let { c -> if (ok(c)) hits[c] = (hits[c] ?: 0) + 1 }
        hits.maxByOrNull { it.value }?.let { return it.key }
    }

    for ((cat, keywords) in CATEGORY_KEYWORDS) {
        if (keywords.any { n.contains(it) } && ok(cat)) return cat
    }
    return null
}
