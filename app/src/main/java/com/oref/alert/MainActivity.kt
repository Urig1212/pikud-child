package com.oref.alert

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {

    companion object {
        private const val REQUEST_OVERLAY = 100
    }

    private val overlayManager get() = (application as OrefApplication).overlayManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvStatus = findViewById<TextView>(R.id.tv_status)
        val btnPermission = findViewById<Button>(R.id.btn_permission)
        val btnService = findViewById<Button>(R.id.btn_service)
        val btnSettings = findViewById<Button>(R.id.btn_settings)
        val btnTestGeneral = findViewById<Button>(R.id.btn_test_general)
        val btnTestJerusalem = findViewById<Button>(R.id.btn_test_jerusalem)
        val btnTestHide = findViewById<Button>(R.id.btn_test_hide)

        btnPermission.setOnClickListener { requestOverlayPermission() }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnService.setOnClickListener {
            AlertService.start(this)
            tvStatus.text = "השירות פועל - מנטר התרעות"
        }

        // Test buttons — 6 combinations: 3 types × general/Jerusalem
        btnTestGeneral.setOnClickListener {
            overlayManager.showAlert(OrefAlert(id="t1", cat="1",
                data = listOf("תל אביב - מזרח", "חיפה - כרמל", "אשקלון")))
            tvStatus.text = "TEST: כניסה לממד — כללי"
        }
        btnTestJerusalem.setOnClickListener {
            overlayManager.showAlert(OrefAlert(id="t2", cat="1",
                data = listOf("ירושלים", "בית שמש")))
            tvStatus.text = "TEST: כניסה לממד — ירושלים"
        }
        findViewById<Button>(R.id.btn_test_pre_general).setOnClickListener {
            overlayManager.showAlert(OrefAlert(id="t3", cat="14",
                data = listOf("תל אביב", "רמת גן")))
            tvStatus.text = "TEST: התרעה מוקדמת — כללי"
        }
        findViewById<Button>(R.id.btn_test_pre_jerusalem).setOnClickListener {
            overlayManager.showAlert(OrefAlert(id="t4", cat="14",
                data = listOf("ירושלים")))
            tvStatus.text = "TEST: התרעה מוקדמת — ירושלים"
        }
        findViewById<Button>(R.id.btn_test_clear_general).setOnClickListener {
            overlayManager.showAlert(OrefAlert(id="t5", cat="13",
                data = listOf("תל אביב")))
            tvStatus.text = "TEST: סיום — כללי"
        }
        findViewById<Button>(R.id.btn_test_clear_jerusalem).setOnClickListener {
            overlayManager.showAlert(OrefAlert(id="t6", cat="13",
                data = listOf("ירושלים")))
            tvStatus.text = "TEST: סיום — ירושלים"
        }
        btnTestHide.setOnClickListener {
            overlayManager.hideAlert()
            tvStatus.text = "ההתרעה הוסתרה"
        }

        // Auto-start if overlay permission already granted
        if (hasOverlayPermission()) {
            btnPermission.isEnabled = false
            btnPermission.text = "הרשאת שכבת-על: מאושרת"
            AlertService.start(this)
            tvStatus.text = "השירות פועל - מנטר התרעות"
        } else {
            tvStatus.text = "נדרשת הרשאת שכבת-על"
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasOverlayPermission()) {
            findViewById<Button>(R.id.btn_permission).apply {
                isEnabled = false
                text = "הרשאת שכבת-על: מאושרת"
            }
            AlertService.start(this)
            findViewById<TextView>(R.id.tv_status).text = "השירות פועל - מנטר התרעות"
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                REQUEST_OVERLAY
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}
