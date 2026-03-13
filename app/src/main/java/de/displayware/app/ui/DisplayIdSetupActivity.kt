package de.displayware.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import de.displayware.app.R
import de.displayware.app.config.DisplayIdStore

class DisplayIdSetupActivity : AppCompatActivity() {

    private lateinit var displayIdStore: DisplayIdStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_id_setup)

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
}
