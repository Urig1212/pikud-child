package com.oref.alert

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

enum class AlertType {
    PRE_ALERT,      // cat 4  — warning (אזהרה כללית / התרעה מקדימה)
    ENTER_SHELTER,  // cat 1  — missilealert | cat 2 uav | cat 3 nonconventional | cat 9 cbrne | cat 10 terrorattack
    ALL_CLEAR,      // cat 13 — update | cat 14 flash  (עדכון/ניתן לצאת)
    DRILL,          // cat 15-28 — תרגילים (מוצגים בצורה עדינה, לא מפחידים)
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
        "4"                                  -> AlertType.PRE_ALERT
        "1","2","3","6","7","8","9","10","11","12" -> AlertType.ENTER_SHELTER
        "13","14"                            -> AlertType.ALL_CLEAR
        "15","16","17","18","19","20",
        "21","22","23","24","25","26","27","28" -> AlertType.DRILL
        else                                 -> AlertType.OTHER
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
