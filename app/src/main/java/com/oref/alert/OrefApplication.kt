package com.oref.alert

import android.app.Application

class OrefApplication : Application() {
    // Shared overlay manager instance, accessible from Activity and Service
    val overlayManager: AlertOverlayManager by lazy { AlertOverlayManager(this) }
}
