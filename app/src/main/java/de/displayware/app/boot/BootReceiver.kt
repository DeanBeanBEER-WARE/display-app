package de.displayware.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.displayware.app.ui.PlayerActivity

/**
 * BootReceiver fängt das System-Event BOOT_COMPLETED ab.
 * Sobald das Gerät nach einem Neustart (z.B. nach einem Stromausfall) hochgefahren ist,
 * wird automatisch die [PlayerActivity] im Vordergrund gestartet,
 * um den Kiosk-Modus und die Werbefläche direkt wiederherzustellen.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || 
            intent?.action == "de.displayware.app.TEST_BOOT_RECEIVER") {
            val startIntent = Intent(context, PlayerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(startIntent)
        }
    }
}
