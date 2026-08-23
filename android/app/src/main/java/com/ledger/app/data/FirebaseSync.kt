package com.ledger.app.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Date

/* ═══════════════════════════════════════════
   FIREBASE CONFIG & CLOUD SYNC HELPERS
   ═══════════════════════════════════════════ */

object FirebaseConfig {
    const val apiKey: String = "AIzaSyCtfTPJ2-5OJIjc7hgbFqciSlsSpvqv43w"
    const val authDomain: String = "ledger-df5a2.firebaseapp.com"
    const val projectId: String = "ledger-df5a2"
    const val storageBucket: String = "ledger-df5a2.firebasestorage.app"
    const val messagingSenderId: String = "674114677747"
    const val webAppId: String = "1:674114677747:web:054ffcc2110b887a040086"
    const val measurementId: String = "G-SFKZ27MNPS"

    /**
     * Android App ID from Firebase Console (mobilesdk_app_id in google-services.json)
     */
    const val androidAppId: String = "1:674114677747:android:ed9d80c047d8ef99040086"

    /**
     * OAuth 2.0 Web Client ID from Firebase Console -> Authentication -> Google -> Web SDK configuration
     */
    const val webClientId: String = "674114677747-0a05kfn4shcph67knn4jgugf694k73la.apps.googleusercontent.com"

    val appId: String
        get() = androidAppId.ifBlank { webAppId }

    val isConfigured: Boolean
        get() = !listOf(apiKey, authDomain, projectId, appId).any { it.startsWith("PASTE_") || it.isBlank() }
}

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var initialized = false

    fun init(context: Context): Boolean {
        if (!FirebaseConfig.isConfigured) return false
        if (initialized) return true

        return try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(FirebaseConfig.apiKey)
                    .setApplicationId(FirebaseConfig.appId)
                    .setProjectId(FirebaseConfig.projectId)
                    .setGcmSenderId(FirebaseConfig.messagingSenderId)
                    .setStorageBucket(FirebaseConfig.storageBucket)
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
            } else {
                FirebaseApp.getInstance()
            }
            initialized = true
            true
        } catch (e: Exception) {
            Log.w(TAG, "Firebase init failed", e)
            false
        }
    }

    fun getAuth(context: Context): FirebaseAuth? {
        if (!init(context)) return null
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth getInstance failed", e)
            null
        }
    }

    fun getFirestore(context: Context): FirebaseFirestore? {
        if (!init(context)) return null
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore getInstance failed", e)
            null
        }
    }
}

/**
 * Result of parsing a remote Firestore document.
 */
data class RemoteSyncResult(
    val expenses: List<Expense>? = null,
    val topUps: List<TopUp>? = null,
    val balance: Balance? = null,
    val piggy: Piggy? = null,
    val piggies: List<Piggy>? = null,
    val recurring: List<Rule>? = null,
    val settings: Settings? = null,
    val categories: List<Category>? = null,
    val catBudgets: Map<String, Double>? = null,
    val prefs: Prefs? = null,
    val theme: AppTheme? = null,
    val savedTheme: AppTheme? = null,
    val hasSavedThemeKey: Boolean = false,
)

object FirebaseSyncSerializer {

    /**
     * Builds the Firestore map payload matching the web app shape.
     */
    fun buildPayload(
        expenses: List<Expense>,
        topUps: List<TopUp>,
        balance: Balance,
        piggies: List<Piggy>,
        recurring: List<Rule>,
        settings: Settings?,
        categories: List<Category>,
        catBudgets: Map<String, Double>,
        prefs: Prefs,
        theme: AppTheme,
        savedTheme: AppTheme?,
    ): Map<String, Any?> {
        val firstPiggy = piggies.firstOrNull() ?: Piggy()

        val payload = mutableMapOf<String, Any?>()

        payload["expenses"] = expenses.map { e ->
            val cat = (e.categories.firstOrNull() ?: e.category) ?: "food"
            mapOf(
                "id" to e.id,
                "date" to e.date,
                "amount" to e.amount,
                "categories" to listOf(cat),
                "category" to cat,
                "note" to e.note,
            )
        }

        payload["topUps"] = topUps.map { t ->
            mapOf(
                "id" to t.id,
                "amount" to t.amount,
                "date" to t.date,
                "note" to t.note,
            )
        }

        payload["balance"] = mapOf("start" to balance.start)

        payload["piggy"] = mapOf(
            "target" to firstPiggy.target,
            "saved" to firstPiggy.saved,
        )

        payload["piggies"] = piggies.map { p ->
            mapOf(
                "id" to p.id,
                "name" to p.name,
                "target" to p.target,
                "saved" to p.saved,
            )
        }

        payload["recurring"] = recurring.map { r ->
            mapOf(
                "id" to r.id,
                "type" to r.type,
                "amount" to r.amount,
                "category" to r.category,
                "note" to r.note,
                "freq" to r.freq,
                "start" to r.start,
                "last" to r.last,
                "active" to r.active,
            )
        }

        payload["settings"] = settings?.let { s ->
            mapOf(
                "monthlyBudget" to s.monthlyBudget,
                "periodDays" to s.periodDays,
                "startDate" to s.startDate,
            )
        }

        payload["cats"] = categories.map { c ->
            mapOf(
                "id" to c.id,
                "label" to c.label,
                "glyph" to c.glyph,
            )
        }

        payload["catBudgets"] = catBudgets

        // Clean prefs for sync (wallpaper/texture/sounds stay device-local)
        payload["prefs"] = mapOf(
            "currency" to prefs.currency,
            "compact" to prefs.compact,
            "pieThickness" to prefs.pieThickness.toDouble(),
            "pieGap" to prefs.pieGap.toDouble(),
            "groupHistory" to prefs.groupHistory,
            "trendStyle" to prefs.trendStyle,
            "heatColors" to prefs.heatColors,
            "font" to prefs.font,
            "cardOrder" to prefs.cardOrder,
            "balancesEnabled" to prefs.balancesEnabled,
            "heroMode" to prefs.heroMode,
        )

        payload["theme"] = mapOf(
            "bg" to theme.bg,
            "surface" to theme.surface,
            "surface2" to theme.surface2,
            "text" to theme.text,
            "textDim" to theme.textDim,
            "textMuted" to theme.textMuted,
            "border" to theme.border,
            "borderStrong" to theme.borderStrong,
            "accent" to theme.accent,
            "accentFg" to theme.accentFg,
            "negative" to theme.negative,
            "warning" to theme.warning,
            "positive" to theme.positive,
            "catColors" to theme.catColors,
        )

        payload["savedTheme"] = savedTheme?.let { st ->
            mapOf(
                "bg" to st.bg,
                "surface" to st.surface,
                "surface2" to st.surface2,
                "text" to st.text,
                "textDim" to st.textDim,
                "textMuted" to st.textMuted,
                "border" to st.border,
                "borderStrong" to st.borderStrong,
                "accent" to st.accent,
                "accentFg" to st.accentFg,
                "negative" to st.negative,
                "warning" to st.warning,
                "positive" to st.positive,
                "catColors" to st.catColors,
            )
        }

        return payload
    }

    /**
     * Parses the remote Firestore data into strongly-typed objects with defensive validation.
     */
    @Suppress("UNCHECKED_CAST")
    fun parseRemote(data: Map<String, Any?>, currentPrefs: Prefs, currentTheme: AppTheme): RemoteSyncResult {
        // Expenses (normalized to single category)
        val expenses = (data["expenses"] as? List<*>)?.mapNotNull { item ->
            val m = item as? Map<String, Any?> ?: return@mapNotNull null
            val id = (m["id"] ?: m["id'"])?.toString() ?: return@mapNotNull null
            val date = m["date"]?.toString() ?: return@mapNotNull null
            val amount = (m["amount"] as? Number)?.toDouble() ?: return@mapNotNull null
            val rawCats = m["categories"] as? List<*>
            val categories = rawCats?.mapNotNull { it?.toString() } ?: emptyList()
            val singleCat = (categories.firstOrNull() ?: m["category"]?.toString()) ?: "food"
            val note = m["note"]?.toString() ?: ""
            Expense(id, date, amount, listOf(singleCat), singleCat, note)
        }

        // Top-ups
        val topUps = (data["topUps"] as? List<*>)?.mapNotNull { item ->
            val m = item as? Map<String, Any?> ?: return@mapNotNull null
            val id = m["id"]?.toString() ?: return@mapNotNull null
            val date = m["date"]?.toString() ?: return@mapNotNull null
            val amount = (m["amount"] as? Number)?.toDouble() ?: return@mapNotNull null
            val note = m["note"]?.toString() ?: ""
            TopUp(id, amount, date, note)
        }

        // Balance
        val balance = (data["balance"] as? Map<String, Any?>)?.let { m ->
            val start = (m["start"] as? Number)?.toDouble() ?: 0.0
            Balance(start)
        }

        // Piggies
        val piggiesList = (data["piggies"] as? List<*>)?.mapNotNull { item ->
            val m = item as? Map<String, Any?> ?: return@mapNotNull null
            val id = m["id"]?.toString() ?: return@mapNotNull null
            val name = m["name"]?.toString() ?: "Piggy bank"
            val target = (m["target"] as? Number)?.toDouble() ?: 0.0
            val saved = (m["saved"] as? Number)?.toDouble() ?: 0.0
            val texture = m["texture"]?.toString()
            val soundId = m["soundId"]?.toString() ?: "coin"
            val soundCustom = m["soundCustom"]?.toString()
            Piggy(id, name, target, saved, texture, soundId, soundCustom)
        }
        val singlePiggy = (data["piggy"] as? Map<String, Any?>)?.let { m ->
            val target = (m["target"] as? Number)?.toDouble() ?: 0.0
            val saved = (m["saved"] as? Number)?.toDouble() ?: 0.0
            Piggy(id = "default", name = "Piggy bank", target = target, saved = saved)
        }
        val resolvedPiggies = if (!piggiesList.isNullOrEmpty()) piggiesList else singlePiggy?.let { listOf(it) }

        // Recurring
        val recurring = (data["recurring"] as? List<*>)?.mapNotNull { item ->
            val m = item as? Map<String, Any?> ?: return@mapNotNull null
            val id = m["id"]?.toString() ?: return@mapNotNull null
            val type = m["type"]?.toString() ?: "expense"
            val amount = (m["amount"] as? Number)?.toDouble() ?: return@mapNotNull null
            val category = m["category"]?.toString() ?: ""
            val note = m["note"]?.toString() ?: ""
            val freq = m["freq"]?.toString() ?: "monthly"
            val start = m["start"]?.toString() ?: ""
            val last = m["last"]?.toString()
            val active = m["active"] as? Boolean ?: true
            Rule(id, type, amount, category, note, freq, start, last, active)
        }

        // Settings
        val settings = (data["settings"] as? Map<String, Any?>)?.let { m ->
            val monthlyBudget = (m["monthlyBudget"] as? Number)?.toDouble() ?: return@let null
            val periodDays = (m["periodDays"] as? Number)?.toInt() ?: return@let null
            val startDate = m["startDate"]?.toString() ?: return@let null
            Settings(monthlyBudget, periodDays, startDate)
        }

        // Categories (web uses 'cats', backups use 'categories')
        val rawCatsList = (data["cats"] as? List<*>) ?: (data["categories"] as? List<*>)
        val categories = rawCatsList?.mapNotNull { item ->
            val m = item as? Map<String, Any?> ?: return@mapNotNull null
            val id = m["id"]?.toString() ?: return@mapNotNull null
            val label = m["label"]?.toString() ?: return@mapNotNull null
            val glyph = m["glyph"]?.toString() ?: "★"
            Category(id, label, glyph)
        }

        // Category budgets
        val catBudgets = (data["catBudgets"] as? Map<String, Any?>)?.mapNotNull { (k, v) ->
            val d = (v as? Number)?.toDouble() ?: return@mapNotNull null
            k to d
        }?.toMap()

        // Prefs
        val prefs = (data["prefs"] as? Map<String, Any?>)?.let { m ->
            var p = currentPrefs
            m["currency"]?.toString()?.let { p = p.copy(currency = it) }
            (m["compact"] as? Boolean)?.let { p = p.copy(compact = it) }
            (m["pieThickness"] as? Number)?.toFloat()?.let { p = p.copy(pieThickness = it) }
            (m["pieGap"] as? Number)?.toFloat()?.let { p = p.copy(pieGap = it) }
            (m["groupHistory"] as? Boolean)?.let { p = p.copy(groupHistory = it) }
            m["trendStyle"]?.toString()?.let { p = p.copy(trendStyle = it) }
            (m["heatColors"] as? Map<String, Any?>)?.let { hc ->
                p = p.copy(heatColors = hc.mapNotNull { (k, v) -> v?.toString()?.let { k to it } }.toMap())
            }
            m["font"]?.toString()?.let { p = p.copy(font = it) }
            (m["cardOrder"] as? List<*>)?.let { co ->
                val list = co.mapNotNull { it?.toString() }
                if (list.isNotEmpty()) p = p.copy(cardOrder = list)
            }
            (m["balancesEnabled"] as? Boolean)?.let { p = p.copy(balancesEnabled = it) }
            m["heroMode"]?.toString()?.let { p = p.copy(heroMode = if (it == "balance") "balance" else "daily") }
            p
        }

        // Theme
        val theme = (data["theme"] as? Map<String, Any?>)?.let { m ->
            val bg = m["bg"]?.toString() ?: currentTheme.bg
            val surface = m["surface"]?.toString() ?: currentTheme.surface
            val surface2 = m["surface2"]?.toString() ?: currentTheme.surface2
            val text = m["text"]?.toString() ?: currentTheme.text
            val textDim = m["textDim"]?.toString() ?: currentTheme.textDim
            val textMuted = m["textMuted"]?.toString() ?: currentTheme.textMuted
            val border = m["border"]?.toString() ?: currentTheme.border
            val borderStrong = m["borderStrong"]?.toString() ?: currentTheme.borderStrong
            val accent = m["accent"]?.toString() ?: currentTheme.accent
            val accentFg = m["accentFg"]?.toString() ?: currentTheme.accentFg
            val negative = m["negative"]?.toString() ?: currentTheme.negative
            val warning = m["warning"]?.toString() ?: currentTheme.warning
            val positive = m["positive"]?.toString() ?: currentTheme.positive
            val catColors = (m["catColors"] as? Map<String, Any?>)?.mapNotNull { (k, v) ->
                v?.toString()?.let { k to it }
            }?.toMap() ?: currentTheme.catColors
            AppTheme(bg, surface, surface2, text, textDim, textMuted, border, borderStrong, accent, accentFg, negative, warning, positive, catColors)
        }

        // Saved theme
        val hasSavedThemeKey = data.containsKey("savedTheme")
        val savedTheme = (data["savedTheme"] as? Map<String, Any?>)?.let { m ->
            val bg = m["bg"]?.toString() ?: return@let null
            val surface = m["surface"]?.toString() ?: return@let null
            val surface2 = m["surface2"]?.toString() ?: return@let null
            val text = m["text"]?.toString() ?: return@let null
            val textDim = m["textDim"]?.toString() ?: return@let null
            val textMuted = m["textMuted"]?.toString() ?: return@let null
            val border = m["border"]?.toString() ?: return@let null
            val borderStrong = m["borderStrong"]?.toString() ?: return@let null
            val accent = m["accent"]?.toString() ?: return@let null
            val accentFg = m["accentFg"]?.toString() ?: return@let null
            val negative = m["negative"]?.toString() ?: return@let null
            val warning = m["warning"]?.toString() ?: return@let null
            val positive = m["positive"]?.toString() ?: return@let null
            val catColors = (m["catColors"] as? Map<String, Any?>)?.mapNotNull { (k, v) ->
                v?.toString()?.let { k to it }
            }?.toMap() ?: emptyMap()
            AppTheme(bg, surface, surface2, text, textDim, textMuted, border, borderStrong, accent, accentFg, negative, warning, positive, catColors)
        }

        return RemoteSyncResult(
            expenses = expenses,
            topUps = topUps,
            balance = balance,
            piggy = resolvedPiggies?.firstOrNull(),
            piggies = resolvedPiggies,
            recurring = recurring,
            settings = settings,
            categories = categories,
            catBudgets = catBudgets,
            prefs = prefs,
            theme = theme,
            savedTheme = savedTheme,
            hasSavedThemeKey = hasSavedThemeKey,
        )
    }
}
