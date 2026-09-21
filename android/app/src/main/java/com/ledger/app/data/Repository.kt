package com.ledger.app.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.ledgerDataStore by preferencesDataStore(name = "ledger")

/**
 * Persistence layer — JSON blobs in DataStore, keyed exactly like the web app's
 * localStorage (`ledger-*`), so backups are byte-compatible between platforms.
 */
class Repository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        coerceInputValues = true
        isLenient = true
    }

    data class StoredData(
        val theme: AppTheme? = null,
        val savedTheme: AppTheme? = null,
        val prefs: Prefs? = null,
        val settings: Settings? = null,
        val expenses: List<Expense>? = null,
        val categories: List<Category>? = null,
        val catBudgets: Map<String, Double>? = null,
        val topUps: List<TopUp>? = null,
        val balance: Balance? = null,
        val piggy: Piggy? = null,
        val piggies: List<Piggy>? = null,
        val recurring: List<Rule>? = null,
        /** Keys whose blob exists but could not be decoded — never treat them as absent. */
        val corruptKeys: Set<String> = emptySet(),
    )

    private suspend fun readRaw(key: String): String? {
        val k = stringPreferencesKey(key)
        return context.ledgerDataStore.data.map { it[k] }.first()
    }

    private suspend inline fun <reified T> write(key: String, value: T) {
        // Store null slices as absent, not the JSON literal "null" — otherwise the loader
        // can't tell a null value apart from an unreadable blob and flags a false corruption.
        if (value == null) {
            context.ledgerDataStore.edit { it.remove(stringPreferencesKey(key)) }
            return
        }
        val k = stringPreferencesKey(key)
        context.ledgerDataStore.edit { it[k] = json.encodeToString(value) }
    }

    private suspend fun remove(key: String) {
        val k = stringPreferencesKey(key)
        context.ledgerDataStore.edit { it.remove(k) }
    }

    suspend fun load(): StoredData {
        val corrupt = mutableSetOf<String>()

        /** Decode a present blob; distinguish "missing" (null) from "corrupt". */
        suspend fun <T> read(key: String, decode: (String) -> T?): T? {
            val raw = readRaw(key) ?: return null
            // A nullable slice persisted as the JSON literal "null" (older builds) is absent.
            if (raw == "null" || raw.isBlank()) return null
            val t = runCatching { decode(raw) }
                .onFailure { Log.w(TAG, "Couldn't decode $key — keeping raw value", it) }
                .getOrNull()
            if (t == null) corrupt.add(key)
            return t
        }

        val piggiesList: List<Piggy>? = read("ledger-piggies") { json.decodeFromString(it) }
        val oldPiggy: Piggy? = read("ledger-piggy") { json.decodeFromString(it) }
        val resolvedPiggies = piggiesList ?: oldPiggy?.let { listOf(it) } ?: listOf(Piggy())
        return StoredData(
            theme = read("ledger-theme") { json.decodeFromString(it) },
            savedTheme = read("ledger-theme-saved") { json.decodeFromString(it) },
            prefs = read("ledger-prefs") { json.decodeFromString(it) },
            settings = read("ledger-settings") { json.decodeFromString(it) },
            expenses = read("ledger-expenses") { json.decodeFromString(it) },
            categories = read("ledger-cats") { json.decodeFromString(it) },
            catBudgets = read("ledger-catbudgets") { json.decodeFromString(it) },
            topUps = read("ledger-topups") { json.decodeFromString(it) },
            balance = read("ledger-balance") { json.decodeFromString(it) },
            piggy = resolvedPiggies.firstOrNull(),
            piggies = resolvedPiggies,
            recurring = read("ledger-recurring") { json.decodeFromString(it) },
            corruptKeys = corrupt,
        )
    }

    suspend fun saveTheme(v: AppTheme) = write("ledger-theme", v)
    suspend fun saveSavedTheme(v: AppTheme?) = write("ledger-theme-saved", v)
    suspend fun savePrefs(v: Prefs) = write("ledger-prefs", v)
    suspend fun saveSettings(v: Settings?) = write("ledger-settings", v)
    suspend fun saveExpenses(v: List<Expense>) = write("ledger-expenses", v)
    suspend fun saveCategories(v: List<Category>) = write("ledger-cats", v)
    suspend fun saveCatBudgets(v: Map<String, Double>) = write("ledger-catbudgets", v)
    suspend fun saveTopUps(v: List<TopUp>) = write("ledger-topups", v)
    suspend fun saveBalance(v: Balance) = write("ledger-balance", v)
    suspend fun savePiggies(v: List<Piggy>) {
        write("ledger-piggies", v)
        v.firstOrNull()?.let { write("ledger-piggy", it) }
    }
    suspend fun savePiggy(v: Piggy) = savePiggies(listOf(v))
    suspend fun saveRecurring(v: List<Rule>) = write("ledger-recurring", v)

    val appContext: Context get() = context

    suspend fun getLastSync(uid: String): Long {
        val k = stringPreferencesKey("ledger-synclast2")
        var result = 0L
        context.ledgerDataStore.edit { prefs ->
            val map = prefs[k]?.let { runCatching { json.decodeFromString<Map<String, Long>>(it) }.getOrNull() }
                ?: emptyMap()
            result = map[uid] ?: 0L
            if (result > System.currentTimeMillis() + 60000L) {
                prefs[k] = json.encodeToString(map.toMutableMap().apply { remove(uid) })
                result = 0L
            }
        }
        return result
    }

    suspend fun setLastSync(uid: String, t: Long) {
        val k = stringPreferencesKey("ledger-synclast2")
        context.ledgerDataStore.edit { prefs ->
            val map = prefs[k]?.let { runCatching { json.decodeFromString<Map<String, Long>>(it) }.getOrNull() }
                ?: emptyMap()
            prefs[k] = json.encodeToString(map.toMutableMap().apply { put(uid, t) })
        }
    }

    suspend fun getBudgetAlerts(): BudgetAlertState =
        readRaw("ledger-budgetalerts")?.let {
            runCatching { json.decodeFromString<BudgetAlertState>(it) }.getOrNull()
        } ?: BudgetAlertState()

    suspend fun saveBudgetAlerts(v: BudgetAlertState) = write("ledger-budgetalerts", v)

    suspend fun clearAll() {
        listOf(
            "ledger-theme", "ledger-theme-saved", "ledger-prefs", "ledger-settings",
            "ledger-expenses", "ledger-cats", "ledger-catbudgets", "ledger-topups",
            "ledger-balance", "ledger-piggy", "ledger-piggies", "ledger-recurring",
            "ledger-budgetalerts",
        ).forEach { remove(it) }
    }

    private companion object {
        const val TAG = "Repository"
    }
}
