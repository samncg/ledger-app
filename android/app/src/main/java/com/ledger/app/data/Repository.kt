package com.ledger.app.data

import android.content.Context
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
    )

    private suspend inline fun <reified T> read(key: String): T? {
        val k = stringPreferencesKey(key)
        val raw = context.ledgerDataStore.data.map { it[k] }.first() ?: return null
        return runCatching { json.decodeFromString<T>(raw) }.getOrNull()
    }

    private suspend inline fun <reified T> write(key: String, value: T) {
        val k = stringPreferencesKey(key)
        context.ledgerDataStore.edit { it[k] = json.encodeToString(value) }
    }

    private suspend fun remove(key: String) {
        val k = stringPreferencesKey(key)
        context.ledgerDataStore.edit { it.remove(k) }
    }

    suspend fun load(): StoredData {
        val piggiesList: List<Piggy>? = read("ledger-piggies")
        val oldPiggy: Piggy? = read("ledger-piggy")
        val resolvedPiggies = piggiesList ?: oldPiggy?.let { listOf(it) } ?: listOf(Piggy())
        return StoredData(
            theme = read("ledger-theme"),
            savedTheme = read("ledger-theme-saved"),
            prefs = read("ledger-prefs"),
            settings = read("ledger-settings"),
            expenses = read("ledger-expenses"),
            categories = read("ledger-cats"),
            catBudgets = read("ledger-catbudgets"),
            topUps = read("ledger-topups"),
            balance = read("ledger-balance"),
            piggy = resolvedPiggies.firstOrNull(),
            piggies = resolvedPiggies,
            recurring = read("ledger-recurring"),
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

    suspend fun clearAll() {
        listOf(
            "ledger-theme", "ledger-theme-saved", "ledger-prefs", "ledger-settings",
            "ledger-expenses", "ledger-cats", "ledger-catbudgets", "ledger-topups",
            "ledger-balance", "ledger-piggy", "ledger-piggies", "ledger-recurring",
        ).forEach { remove(it) }
    }
}
