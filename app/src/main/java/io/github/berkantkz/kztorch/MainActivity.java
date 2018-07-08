package io.github.berkantkz.kztorch;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends PreferenceActivity {

    SharedPreferences sharedPreferences;

    boolean suAvailable;

    AdView mAdView;
    static InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main);
        setContentView(R.layout.ad_view);

        startinterstitialAd(MainActivity.this);

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("966CA55011953A922BA919A3EC4298C0")
                .build();
        mAdView.loadAd(adRequest);

        suAvailable = Shell.SU.available();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (suAvailable) {
            findPreference("pref_root_status").setSummary(R.string.root_status_true);
        } else {
            findPreference("pref_general").setEnabled(false);
        }

        findPreference("pref_disable_ads").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if (!sharedPreferences.getBoolean("pref_disable_ads",false)) {
                    Toast.makeText(MainActivity.this, R.string.ad_disabled_warning, Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
    }

    static void startinterstitialAd(Context context) {
        mInterstitialAd = new InterstitialAd(context);
        mInterstitialAd.setAdUnitId("ca-app-pub-2951689275458403/7609563416");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                mInterstitialAd.show();
            }
        });
    }
}
