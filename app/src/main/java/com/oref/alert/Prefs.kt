package com.oref.alert

import android.content.Context

object Prefs {
    private const val FILE = "oref_prefs"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // General alerts toggle (default: on)
    fun showGeneralAlerts(ctx: Context) = prefs(ctx).getBoolean("show_general_alerts", true)
    fun setShowGeneralAlerts(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean("show_general_alerts", v).apply()

    // Home city — default ירושלים so the Glikman family works out-of-the-box
    fun homeCity(ctx: Context): String = prefs(ctx).getString("home_city", "ירושלים") ?: "ירושלים"
    fun setHomeCity(ctx: Context, city: String) = prefs(ctx).edit().putString("home_city", city).apply()

    // Personal message shown during home-city alert (optional)
    fun personalMessage(ctx: Context): String = prefs(ctx).getString("personal_message", "") ?: ""
    fun setPersonalMessage(ctx: Context, msg: String) = prefs(ctx).edit().putString("personal_message", msg).apply()
}
