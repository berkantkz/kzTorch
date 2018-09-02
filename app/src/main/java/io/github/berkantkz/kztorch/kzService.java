package io.github.berkantkz.kztorch;

import android.app.Notification;
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

    //static String torch_path = "/sys/class/leds/flashlight-front/brightness";
    static String torch_path = "/sys/class/leds/led:torch_1/brightness", appTag = "kzTorch";
    static int torch_level, torch_status;
    static boolean torch_current_status;
    SharedPreferences sharedPreferences;
    static NotificationManagerCompat notificationManager;
    static NotificationCompat.Builder mBuilder;
    static Tile tile;

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

            // Intent for turning off the torch
            Intent turnOffIntent = new Intent(this, kzService.ActionReceiver.class);
            turnOffIntent.setAction("turnoff");
            PendingIntent turnOffPendingIntent = PendingIntent.getBroadcast(this, 1, turnOffIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            // Intent for increasing the level of the torch
            Intent increaseIntent = new Intent(this, kzService.ActionReceiver.class);
            increaseIntent.setAction("increase");
            PendingIntent increasePendingIntent = PendingIntent.getBroadcast(this, 1, increaseIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            // Intent for decreasing the level of the torch
            Intent decreaseIntent = new Intent(this, kzService.ActionReceiver.class);
            decreaseIntent.setAction("decrease");
            PendingIntent decreasePendingIntent = PendingIntent.getBroadcast(this, 1, decreaseIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            mBuilder = new NotificationCompat.Builder(this, "kzTorch")
                    .setContentTitle(getString(R.string.notification_on_title))
                    .setContentText(getString(R.string.notification_on_content))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.notification_on_content)))
                    .setSmallIcon(R.drawable.ic_tile_on)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .addAction(R.drawable.ic_tile_on, getString(R.string.increase), increasePendingIntent)
                    .addAction(R.drawable.ic_tile_on, getString(R.string.decrease), decreasePendingIntent)
                    .addAction(R.drawable.ic_tile_off, getString(R.string.notification_action_turn_off), turnOffPendingIntent)
                    .setVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager = NotificationManagerCompat.from(this);

            if (torch_status == 0) {
                if (sharedPreferences.getBoolean("prevent_when_locked",isSecure())) {
                    Log.d(appTag,"Device is in locked state. User prevented it to be turned on.");
                    unlockAndRun(new Runnable() {
                        @Override
                        public void run() {
                            turnOn();
                        }
                    });
                } else {
                    turnOn();
                }
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
        Log.d(appTag, "Turned on\nLevel=" + torch_level);
        notificationManager.notify(1, mBuilder.build());
        updateTile(R.drawable.ic_tile_on, Tile.STATE_ACTIVE);
    }

    public void turnOff() {
        torch_current_status = false;
        Shell.SU.run("echo 0 > " + torch_path);
        Log.d(appTag, "kz Torch turned off");
        notificationManager.cancel(1);
        updateTile(R.drawable.ic_tile_off, Tile.STATE_INACTIVE);
    }

    public static void increase() {
        if (torch_level < 255) {
            int increasedLevel;
            if (torch_level >= 200) {
                increasedLevel = 255;
            } else {
                increasedLevel = torch_level + 55;
            }
            torch_level = increasedLevel;
            Shell.SU.run("echo " + torch_level + "> " + torch_path);
            Log.d(appTag, "Value increased\nLevel = " + String.valueOf(increasedLevel));
        } else {
            Log.w(appTag,"Highest value reached.");
        }
    }

    public static void decrease() {
        if (torch_level <= 255) {
            int decreasedLevel;
            if (torch_level <= 55) {
                decreasedLevel = 0;
                notificationManager.cancel(1);
                torch_current_status = false;
                Shell.SU.run("echo 0 > " + torch_path);
                Log.d(appTag, "Turned off");
                notificationManager.cancel(1);
            } else {
                decreasedLevel = torch_level - 55;
            }
            torch_level = decreasedLevel;
            Shell.SU.run("echo " + torch_level + "> " + torch_path);
            Log.d(appTag, "Value decreased\nLevel = " + String.valueOf(decreasedLevel));
        }
    }

    private void updateTile(int icon, int state) {
        tile = getQsTile();
        tile.setLabel(getString(R.string.app_name));
        tile.setIcon(Icon.createWithResource(getApplicationContext(), icon));
        tile.setState(state);
        tile.updateTile();
    }

    public static class ActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getAction();
            switch (String.valueOf(type)) {
                case "turnoff":
                    notificationManager.cancel(1);
                    torch_current_status = false;
                    Shell.SU.run("echo 0 > " + torch_path);
                    Log.d(appTag, "Turned off");
                    notificationManager.cancel(1);
                    break;
                case "increase":
                    increase();
                    break;
                case "decrease":
                    decrease();
                    break;
            }
        }
    }
}
