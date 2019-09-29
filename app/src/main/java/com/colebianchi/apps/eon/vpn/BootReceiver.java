package com.colebianchi.apps.eon.vpn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.colebianchi.apps.eon.MainActivity;
import com.colebianchi.apps.eon.vpn.vservice.VhostsService;

//use adb for test
//am broadcast -a android.intent.action.BOOT_COMPLETED -p com.github.apps.Eon
public class BootReceiver extends BroadcastReceiver {

    public static final String RECONNECT_ON_REBOOT = "RECONNECT_ON_REBOOT";

    public static void setEnabled(Context context,Boolean enabled){
		SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(RECONNECT_ON_REBOOT, enabled);
        editor.apply();
    }

    public static boolean getEnabled(Context context){
		SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getBoolean(RECONNECT_ON_REBOOT, false);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (getEnabled(context)) {
            if(!VhostsService.isRunning()) {
                VhostsService.startVService(context,2);
            }
        }
    }

}
