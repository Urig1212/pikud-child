package com.oref.alert

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.TextView

class AlertOverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var blinkAnimator: ObjectAnimator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoDismiss = Runnable { hideAlert() }

    // Colors
    private val colorPre         = 0xCC_F9A825.toInt()
    private val colorPreHome     = 0xFF_FF8F00.toInt()
    private val colorPreHomePulse= 0xFF_E65100.toInt()
    private val colorEnterGen    = 0xAA_F48FB1.toInt()
    private val colorEnterHome   = 0xFF_E91E8C.toInt()
    private val colorEnterHomePulse = 0xFF_9C27B0.toInt()
    private val colorClear       = 0xCC_2E7D32.toInt()
    private val colorClearHome   = 0xFF_1B5E20.toInt()

    fun showAlert(alert: OrefAlert) {
        handler.removeCallbacks(autoDismiss)
        val homeCity = Prefs.homeCity(context)
        val isHome   = alert.isHomeCity(homeCity)

        if (overlayView == null) createOverlay(alert, isHome, homeCity)
        else                     updateView(overlayView!!, alert, isHome, homeCity)

        // Auto-dismiss schedule
        val ms: Long? = when {
            alert.alertType == AlertType.ALL_CLEAR                         -> 6_000L
            alert.alertType == AlertType.PRE_ALERT    && !isHome           -> 8_000L
            alert.alertType == AlertType.ENTER_SHELTER && !isHome          -> 3_000L
            else -> null  // home-city alerts stay until API clears them
        }
        ms?.let { handler.postDelayed(autoDismiss, it) }
    }

    fun hideAlert() {
        handler.removeCallbacks(autoDismiss)
        blinkAnimator?.cancel(); blinkAnimator = null
        overlayView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        overlayView = null
    }

    private fun createOverlay(alert: OrefAlert, isHome: Boolean, homeCity: String) {
        val view = LayoutInflater.from(context).inflate(R.layout.overlay_alert, null)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).also { it.gravity = Gravity.TOP }

        updateView(view, alert, isHome, homeCity)
        try { windowManager.addView(view, params); overlayView = view } catch (_: Exception) {}
    }

    private fun updateView(view: View, alert: OrefAlert, isHome: Boolean, homeCity: String) {
        val container = view.findViewById<LinearLayout>(R.id.overlay_container)
        val tvWarning = view.findViewById<TextView>(R.id.tv_jerusalem_warning)
        val tvFamily  = view.findViewById<TextView>(R.id.tv_family_message)

        view.findViewById<TextView>(R.id.tv_title).visibility  = View.GONE
        view.findViewById<TextView>(R.id.tv_cities).visibility = View.GONE

        blinkAnimator?.cancel(); blinkAnimator = null

        val personalMsg = Prefs.personalMessage(context)

        when (alert.alertType) {

            AlertType.PRE_ALERT -> if (isHome) {
                style(container, colorPreHome, 32, 18)
                tvWarning.show("🌟 התרעה מוקדמת — $homeCity 🌟", 24f, 0xFFFFFFFF.toInt())
                tvFamily.show(
                    personalMsg.ifBlank { "כדאי להתקרב לממד. אולי עוד מעט צריך להיכנס 🌟" },
                    20f, 0xFFFFE4A0.toInt()
                )
                blink(container, colorPreHome, colorPreHomePulse)
            } else {
                style(container, colorPre, 32, 20)
                tvWarning.visibility = View.GONE
                tvFamily.show("🌟 יש התרעה מוקדמת — לא אצלנו. תמשיכו לראות בנחת 🌟", 20f, 0xFF4E342E.toInt())
            }

            AlertType.ENTER_SHELTER -> if (isHome) {
                style(container, colorEnterHome, 32, 18)
                tvWarning.show("🦄 זמן ממד! $homeCity 🦄", 28f, 0xFFFFFFFF.toInt())
                tvFamily.show(
                    personalMsg.ifBlank { "כולם לממד עכשיו! 🌟" },
                    22f, 0xFFFFE4F7.toInt()
                )
                blink(container, colorEnterHome, colorEnterHomePulse)
            } else {
                style(container, colorEnterGen, 32, 20)
                tvWarning.visibility = View.GONE
                tvFamily.show("🌈 הכל טוב! יש התרעות אבל לא אצלנו. תמשיכו לראות בנחת 🌈", 22f, 0xFF7B1FA2.toInt())
            }

            AlertType.ALL_CLEAR -> if (isHome) {
                style(container, colorClearHome, 32, 18)
                tvWarning.show("✅ ניתן לצאת מהממד!", 26f, 0xFF98FF98.toInt())
                tvFamily.show(
                    personalMsg.ifBlank { "אפשר לחזור לסלון! 🎉🌈" },
                    20f, 0xFFCCFFCC.toInt()
                )
            } else {
                style(container, colorClear, 32, 20)
                tvWarning.visibility = View.GONE
                tvFamily.show("✅ הכל בסדר! ניתן לחזור לנורמלי 🌈", 22f, 0xFFCCFFCC.toInt())
            }

            AlertType.OTHER -> {
                style(container, colorEnterGen, 32, 20)
                tvWarning.visibility = View.GONE
                tvFamily.show("🌈 יש התרעה — לא אצלנו. תמשיכו לראות 🌈", 20f, 0xFF7B1FA2.toInt())
            }
        }
    }

    private fun style(container: LinearLayout, color: Int, hPad: Int, vPad: Int) {
        container.setBackgroundColor(color)
        container.setPadding(hPad, vPad, hPad, vPad)
    }

    private fun TextView.show(text: String, size: Float, color: Int) {
        this.text = text; this.textSize = size; this.setTextColor(color)
        this.visibility = View.VISIBLE
    }

    private fun blink(container: LinearLayout, from: Int, to: Int) {
        blinkAnimator = ObjectAnimator.ofArgb(container, "backgroundColor", from, to).apply {
            duration = 700; repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator(); start()
        }
    }
}
