package org.poirsouille.tinc_gui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context iContext, Intent iIntent)
    {
        if (iIntent.getAction().equals("android.intent.action.BOOT_COMPLETED")) 
        {
            // Auto start service if enabled
            Log.d(Tools.TAG, "Boot notif");
            SharedPreferences aSharedPref = iContext.getSharedPreferences("org.poirsouille.tinc_gui_preferences", Context.MODE_PRIVATE);
            boolean aAutoStart = false;
            aAutoStart = aSharedPref.getBoolean("pref_key_autostart_boot", aAutoStart);
            if (aAutoStart)
            {
                Log.i(Tools.TAG, "Autostarting Tinc GUI service");
                // Start tincd service
                Intent intent = new Intent(iContext, TincdService.class).setAction("org.poirsouille.tinc_gui.TincdService.START"); 
                iContext.startService(intent);
            }
        }
    }

}
