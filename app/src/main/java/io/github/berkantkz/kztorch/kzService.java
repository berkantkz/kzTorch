package io.github.berkantkz.kztorch;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by berkantkz on 13.05.2018.
 * # kz Torch
 */

public class kzService extends TileService {

    String torch_path = "/sys/class/leds/led:torch_1/brightness";
    int torch_level, torch_status;
    boolean torch_current_status, isUserFriendly;
    SharedPreferences sharedPreferences;

    @Override
    public void onClick() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Shell.SH.run("echo 1 > " + torch_path);
        if (torch_status != 0) {
            Toast.makeText(kzService.this, "You have a user-friendly boot image!", Toast.LENGTH_SHORT).show();
            isUserFriendly = true;
            Shell.SH.run("echo 0 > " + torch_path);
        } else {
            Toast.makeText(kzService.this, "No way! You must have root access.", Toast.LENGTH_SHORT).show();
            isUserFriendly = false;
            Shell.SH.run("echo 0 > " + torch_path);
        }

        if (!sharedPreferences.getBoolean("pref_disable_ads",true)) {
            MainActivity.startinterstitialAd(kzService.this);
        }

        if (isUserFriendly) {
            if (sharedPreferences.contains("pref_torch_level")) {
                torch_level = sharedPreferences.getInt("pref_torch_level", 0);
            } else {
                torch_level = 255;
                sharedPreferences.edit().putInt("pref_torch_level", torch_level).apply();
            }
        } else {
            if (Shell.SU.available()) {
                if (sharedPreferences.contains("pref_torch_level")) {
                    torch_level = sharedPreferences.getInt("pref_torch_level", 0);
                } else {
                    torch_level = 255;
                    sharedPreferences.edit().putInt("pref_torch_level", torch_level).apply();
                }

                // Check whether the torch is on or not for first run.
                torch_status = Integer.parseInt(String.valueOf(Shell.SU.run(new String[]{"cat " + torch_path})).replace("[", "").replace("]", ""));

                if (torch_status == 0) {
                    torch_current_status = true;
                    Shell.SU.run("echo " + torch_level + "> " + torch_path);
                    updateTile(R.drawable.twotone_flash_on_white_24dp);
                    Log.d("berkantkz", "kz Torch turned on");
                } else {
                    torch_current_status = false;
                    Shell.SU.run("echo 0 > " + torch_path);
                    updateTile(R.drawable.twotone_flash_off_white_24dp);
                    Log.d("berkantkz", "kz Torch turned off");
                }
            } else {
                Toast.makeText(this, R.string.root_status_false, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateTile(int icon) {
        final Tile tile = getQsTile();
        tile.setIcon(Icon.createWithResource(getApplicationContext(), icon));
        tile.updateTile();
    }
}
