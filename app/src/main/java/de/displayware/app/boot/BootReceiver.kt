package de.displayware.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.displayware.app.ui.PlayerActivity

private const val TAG = "BootReceiver"

/**
 * Receiver that listens for device boot completion and starts the app.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received broadcast: $action")
        
        if (Intent.ACTION_BOOT_COMPLETED == action || 
            "android.intent.action.QUICKBOOT_POWERON" == action) {
            
            val startIntent = Intent(context, PlayerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(startIntent)
        }
    }
}
