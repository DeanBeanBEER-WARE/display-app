package de.displayware.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import de.displayware.app.R
import de.displayware.app.config.DisplayIdStore

class DisplayIdSetupActivity : AppCompatActivity() {

    private lateinit var displayIdStore: DisplayIdStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_id_setup)

        checkAndRequestOverlayPermission()

        displayIdStore = DisplayIdStore(this)

        val etDisplayId = findViewById<EditText>(R.id.etDisplayId)
        val btnSaveAndStart = findViewById<Button>(R.id.btnSaveAndStart)

        // Pre-fill if exists
        val currentId = displayIdStore.getDisplayId()
        if (currentId != null) {
            etDisplayId.setText(currentId)
        }

        btnSaveAndStart.setOnClickListener {
            val inputId = etDisplayId.text.toString().trim()
            if (inputId.isNotEmpty()) {
                displayIdStore.setDisplayId(inputId)
                
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                finish()
            } else {
                etDisplayId.error = "Display ID darf nicht leer sein"
            }
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(
                    this,
                    "Bitte aktiviere 'Über anderen Apps einblenden', damit die App nach einem Neustart automatisch öffnen kann.",
                    Toast.LENGTH_LONG
                ).show()

                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
    }
}
