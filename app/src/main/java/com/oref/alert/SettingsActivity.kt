package com.oref.alert

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.FragmentActivity

class SettingsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val switchGeneral  = findViewById<Switch>(R.id.switch_general_alerts)
        val tvGeneralDesc  = findViewById<TextView>(R.id.tv_general_desc)
        val btnPickCity    = findViewById<Button>(R.id.btn_pick_city)
        val tvCurrentCity  = findViewById<TextView>(R.id.tv_current_city)
        val etPersonalMsg  = findViewById<EditText>(R.id.et_personal_message)

        // ── General toggle ────────────────────────────────────────────────
        switchGeneral.isChecked = Prefs.showGeneralAlerts(this)
        updateGeneralDesc(tvGeneralDesc, switchGeneral.isChecked)
        switchGeneral.setOnCheckedChangeListener { _, checked ->
            Prefs.setShowGeneralAlerts(this, checked)
            updateGeneralDesc(tvGeneralDesc, checked)
        }

        // ── City picker ───────────────────────────────────────────────────
        tvCurrentCity.text = Prefs.homeCity(this)
        btnPickCity.setOnClickListener {
            startActivity(Intent(this, CityPickerActivity::class.java))
        }

        // ── Personal message ──────────────────────────────────────────────
        etPersonalMsg.setText(Prefs.personalMessage(this))
        etPersonalMsg.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) =
                Prefs.setPersonalMessage(this@SettingsActivity, s.toString())
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
    }

    override fun onResume() {
        super.onResume()
        // Refresh city label after returning from picker
        findViewById<TextView>(R.id.tv_current_city).text = Prefs.homeCity(this)
    }

    private fun updateGeneralDesc(tv: TextView, enabled: Boolean) {
        tv.text = if (enabled) "מציג התרעות על שאר הארץ (נעלם אחרי 3–8 שניות)"
                  else         "מוצגות רק התרעות על הישוב שלך"
    }
}
