package io.github.berkantkz.kztorch;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by berkantkz on 13.05.2018.
 * # kz Torch
 */

public class kzService extends TileService {

    static String torch_path = "/sys/class/leds/led:torch_1/brightness";
    int torch_level, torch_status;
    static boolean torch_current_status;
    SharedPreferences sharedPreferences;
    static NotificationManagerCompat notificationManager;
    static NotificationCompat.Builder mBuilder;

    @Override
    public void onClick() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (!sharedPreferences.getBoolean("pref_disable_ads",true)) {
            MainActivity.startinterstitialAd(kzService.this);
        }

        if (Shell.SU.available()) {
            if (sharedPreferences.contains("pref_torch_level")) {
                torch_level = sharedPreferences.getInt("pref_torch_level", 0);
            } else {
                torch_level = 255;
                sharedPreferences.edit().putInt("pref_torch_level", torch_level).apply();
            }

            // Check whether the torch is on or not for first run.
            torch_status = Integer.parseInt(String.valueOf(Shell.SU.run(new String[]{"cat " + torch_path})).replace("[", "").replace("]", ""));

            // Intent for notification action
            Intent turnOffIntent = new Intent(this, kzService.ActionReceiver.class);
            PendingIntent turnOffPendingIntent = PendingIntent.getBroadcast(this, 1, turnOffIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            mBuilder = new NotificationCompat.Builder(this, "kzTorch")
                    .setContentTitle(getString(R.string.notification_on_title))
                    .setContentText(getString(R.string.notification_on_content))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.notification_on_content)))
                    .setSmallIcon(R.drawable.ic_tile)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .addAction(R.drawable.twotone_flash_off_white_24dp, getString(R.string.notification_action_turn_off), turnOffPendingIntent);
            notificationManager = NotificationManagerCompat.from(this);

            if (torch_status == 0) {
                turnOn();
            } else {
                turnOff();
            }
        } else {
            Toast.makeText(this, R.string.root_status_false, Toast.LENGTH_SHORT).show();
        }
    }

    public void turnOn() {
        torch_current_status = true;
        Shell.SU.run("echo " + torch_level + "> " + torch_path);
        Log.d("berkantkz", "kz Torch turned on");
        notificationManager.notify(1, mBuilder.build());
    }

    public void turnOff() {
        torch_current_status = false;
        Shell.SU.run("echo 0 > " + torch_path);
        Log.d("berkantkz", "kz Torch turned off");
        notificationManager.cancel(1);
    }

    private void updateTile(int icon, String label) {
        final Tile tile = getQsTile();
        tile.setIcon(Icon.createWithResource(getApplicationContext(), icon));
        tile.setLabel(label);
        tile.updateTile();
    }

    public static class ActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            notificationManager.cancel(1);
            torch_current_status = false;
            Shell.SU.run("echo 0 > " + torch_path);
            Log.d("berkantkz", "kz Torch turned off");
            notificationManager.cancel(1);
        }
    }
}
