package com.bjobs.vvtcards.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Ibragim on 2017-08-18.
 */

public class DeviceRebootReceiver extends BroadcastReceiver {

    //To retrieve settings
    SharedPreferences settingsPref;
    int syncFrequency;
    boolean syncTickets;
    boolean syncMoney;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            /* Setting the alarm here */
            Intent alarmIntent = new Intent(context, AutomatedCardChecker.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);

            settingsPref = PreferenceManager.getDefaultSharedPreferences(context);
            syncFrequency = Integer.parseInt(settingsPref.getString("sync_frequency", "12"));

            syncTickets = settingsPref.getBoolean("ticket_sync", true);
            syncMoney = settingsPref.getBoolean("money_sync", true);

            if (syncTickets == true || syncMoney == true) {
                AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                int interval = syncFrequency * 60 * 1000;
                manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
            }
        }
    }
}
