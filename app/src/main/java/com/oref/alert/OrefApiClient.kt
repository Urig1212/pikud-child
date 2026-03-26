package com.oref.alert

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

enum class AlertType {
    PRE_ALERT,       // cat 14 — הנחיה מקדימה
    ENTER_SHELTER,   // cat 1-12 — כנס לממד
    ALL_CLEAR,       // cat 13 — ניתן לצאת
    OTHER
}

data class OrefAlert(
    val id: String = "",
    val cat: String = "",
    val title: String = "",
    val data: List<String> = emptyList(),
    val desc: String = ""
) {
    val citiesText: String get() = data.joinToString(" | ")

    val alertType: AlertType get() = when (cat) {
        "14"                                              -> AlertType.PRE_ALERT
        "13"                                              -> AlertType.ALL_CLEAR
        "1","2","3","4","6","7","8","9","10","11","12"    -> AlertType.ENTER_SHELTER
        else                                              -> AlertType.OTHER
    }

    /** True if the configured home city is in this alert */
    fun isHomeCity(homeCity: String): Boolean =
        homeCity.isNotBlank() && data.any { it.contains(homeCity) }
}

// ── City list from oref API ────────────────────────────────────────────────
data class CityInfo(val label: String = "", val value: String = "")

class OrefApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    private fun baseRequest(url: String) = Request.Builder()
        .url(url)
        .header("X-Requested-With", "XMLHttpRequest")
        .header("Referer", "https://www.oref.org.il/")
        .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; TV) AppleWebKit/537.36 Chrome/120.0.0.0")
        .build()

    fun getActiveAlerts(): OrefAlert? {
        return try {
            val body = client.newCall(baseRequest(
                "https://www.oref.org.il/WarningMessages/alert/alerts.json"
            )).execute().body?.string()?.trim() ?: return null

            if (body.isEmpty() || body == "{}" || body == "[]" || body.isBlank()) return null
            val alert = gson.fromJson(body, OrefAlert::class.java) ?: return null
            if (alert.data.isEmpty() && alert.alertType != AlertType.ALL_CLEAR) null else alert
        } catch (_: Exception) { null }
    }

    /** Returns sorted list of city names for the picker */
    fun getCities(): List<String> {
        return try {
            val body = client.newCall(baseRequest(
                "https://www.oref.org.il/Shared/Ajax/GetCitiesMix.aspx?lang=he"
            )).execute().body?.string()?.trim() ?: return emptyList()

            val type = object : TypeToken<List<CityInfo>>() {}.type
            val cities: List<CityInfo> = gson.fromJson(body, type) ?: return emptyList()
            cities.map { it.value }.filter { it.isNotBlank() }.sorted()
        } catch (_: Exception) { emptyList() }
    }
}
