package com.Cali.mohtimer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    // Secret dev-mode trigger
    private var tapCount = 0
    private var lastTapTime = 0L
    private val tapWindowMs = 1500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)

        // Hamburger opens the drawer
        findViewById<View>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Timer mode buttons
        findViewById<MaterialButton>(R.id.btnEmom).setOnClickListener {
            startActivity(Intent(this, EmomActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnAmrap).setOnClickListener {
            startActivity(Intent(this, AmrapActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnForTime).setOnClickListener {
            startActivity(Intent(this, ForTimeActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnTabata).setOnClickListener {
            startActivity(Intent(this, TabataActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnTimer).setOnClickListener {
            startActivity(Intent(this, CountdownActivity::class.java))
        }

        // Bottom PRESETS
        findViewById<View>(R.id.btnPresets).setOnClickListener {
            startActivity(Intent(this, PresetsActivity::class.java))
        }

        // Drawer: Settings
        findViewById<View>(R.id.menuSettings).setOnClickListener {
            drawerLayout.closeDrawers()
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Drawer: Developer mode (only visible if unlocked)
        val menuDeveloper = findViewById<View>(R.id.menuDeveloper)
        menuDeveloper.setOnClickListener {
            drawerLayout.closeDrawers()
            Toast.makeText(this, "Developer mode is on — open any timer to use it", Toast.LENGTH_LONG).show()
        }

        // Secret unlock: tap the title 7 times
        findViewById<TextView>(R.id.tvAppTitle).setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - lastTapTime > tapWindowMs) tapCount = 0
            lastTapTime = now
            tapCount++

            val alreadyUnlocked = DevPrefs.isUnlocked(this)

            if (tapCount in 3..6) {
                val remaining = 7 - tapCount
                Toast.makeText(this, "$remaining more tap${if (remaining == 1) "" else "s"}...", Toast.LENGTH_SHORT).show()
            }

            if (tapCount == 7) {
                tapCount = 0
                if (alreadyUnlocked) {
                    DevPrefs.setUnlocked(this, false)
                    DevPrefs.setSpeed(this, 1)
                    Toast.makeText(this, "Developer mode LOCKED", Toast.LENGTH_LONG).show()
                } else {
                    DevPrefs.setUnlocked(this, true)
                    Toast.makeText(this, "🛠 Developer mode UNLOCKED", Toast.LENGTH_LONG).show()
                }
                refreshDevMenuVisibility()
            }
        }

        refreshDevMenuVisibility()
    }

    private fun refreshDevMenuVisibility() {
        val menuDeveloper = findViewById<View>(R.id.menuDeveloper)
        menuDeveloper.visibility = if (DevPrefs.isUnlocked(this)) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        refreshDevMenuVisibility()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}