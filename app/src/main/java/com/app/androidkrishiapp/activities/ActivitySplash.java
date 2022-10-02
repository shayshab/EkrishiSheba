package com.app.androidkrishiapp.activities;

import static com.app.androidkrishiapp.config.AppConfig.DELAY_SPLASH_SCREEN;
import static com.app.androidkrishiapp.utils.Constant.ITEM_ID;
import static com.app.androidkrishiapp.utils.Constant.ITEM_NAME;
import static com.app.androidkrishiapp.utils.Constant.LOCALHOST_ADDRESS;
import static com.solodroid.ads.sdk.util.Constant.ADMOB;
import static com.solodroid.ads.sdk.util.Constant.AD_STATUS_ON;
import static com.solodroid.ads.sdk.util.Constant.GOOGLE_AD_MANAGER;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.app.androidkrishiapp.BuildConfig;
import com.app.androidkrishiapp.R;
import com.app.androidkrishiapp.callbacks.CallbackSettings;
import com.app.androidkrishiapp.config.AppConfig;
import com.app.androidkrishiapp.database.prefs.AdsPref;
import com.app.androidkrishiapp.database.prefs.SharedPref;
import com.app.androidkrishiapp.models.Ads;
import com.app.androidkrishiapp.models.App;
import com.app.androidkrishiapp.models.License;
import com.app.androidkrishiapp.models.Settings;
import com.app.androidkrishiapp.rests.RestAdapter;
import com.app.androidkrishiapp.utils.Tools;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitySplash extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    ProgressBar progressBar;
    SharedPref sharedPref;
    ImageView imgSplash;
    Call<CallbackSettings> callbackCall = null;
    AdsPref adsPref;
    Settings settings;
    Ads ads;
    App app;
    License license;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_splash);
        Tools.setNavigation(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);

        imgSplash = findViewById(R.id.img_splash);
        if (sharedPref.getIsDarkTheme()) {
            imgSplash.setImageResource(R.drawable.bg_splash_dark);
        } else {
            imgSplash.setImageResource(R.drawable.bg_splash_default);
        }

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            Application application = getApplication();
            if (adsPref.getAdType().equals(ADMOB)) {
                if (!adsPref.getAdMobAppOpenAdId().equals("0")) {
                    ((MyApplication) application).showAdIfAvailable(ActivitySplash.this, this::requestConfig);
                } else {
                    requestConfig();
                }
            } else if (adsPref.getAdType().equals(GOOGLE_AD_MANAGER)) {
                if (!adsPref.getAdManagerAppOpenAdId().equals("0")) {
                    ((MyApplication) application).showAdIfAvailable(ActivitySplash.this, this::requestConfig);
                } else {
                    requestConfig();
                }
            } else {
                requestConfig();
            }
        } else {
            requestConfig();
        }

    }

    private void requestConfig() {
        if (AppConfig.SERVER_KEY.contains("XXXX")) {
            new AlertDialog.Builder(this)
                    .setTitle("App not configured")
                    .setMessage("Please put your Server Key and Rest API Key from settings menu in your admin panel to AppConfig, you can see the documentation for more detailed instructions.")
                    .setPositiveButton(getString(R.string.dialog_ok), (dialogInterface, i) -> startMainActivity())
                    .setCancelable(false)
                    .show();
        } else {
            String decode = Tools.decodeBase64(AppConfig.SERVER_KEY);
            String data = Tools.decrypt(decode);
            String[] results = data.split("_applicationId_");
            String baseUrl = results[0].replace("http://localhost", LOCALHOST_ADDRESS);
            String applicationId = results[1];
            sharedPref.setBaseUrl(baseUrl);

            if (applicationId.equals(BuildConfig.APPLICATION_ID)) {
                if (Tools.isConnect(this)) {
                    requestAPI(baseUrl);
                } else {
                    startMainActivity();
                }
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("Whoops! invalid server key or applicationId, please check your configuration")
                        .setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
            //Log.d(TAG, baseUrl);
            //Log.d(TAG, applicationId);
        }
    }

    private void requestAPI(String apiUrl) {
        this.callbackCall = RestAdapter.createAPI(apiUrl).getSettings(BuildConfig.APPLICATION_ID, AppConfig.REST_API_KEY);
        this.callbackCall.enqueue(new Callback<CallbackSettings>() {
            public void onResponse(@NonNull Call<CallbackSettings> call, @NonNull Response<CallbackSettings> response) {
                CallbackSettings resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    settings = resp.settings;
                    app = resp.app;
                    ads = resp.ads;
                    license = resp.license;

                    adsPref.saveAds(
                            ads.ad_status.replace("on", "1"),
                            ads.ad_type,
                            ads.backup_ads,
                            ads.admob_publisher_id,
                            ads.admob_app_id,
                            ads.admob_banner_unit_id,
                            ads.admob_interstitial_unit_id,
                            ads.admob_native_unit_id,
                            ads.admob_app_open_ad_unit_id,
                            ads.ad_manager_banner_unit_id,
                            ads.ad_manager_interstitial_unit_id,
                            ads.ad_manager_native_unit_id,
                            ads.ad_manager_app_open_ad_unit_id,
                            ads.startapp_app_id,
                            ads.unity_game_id,
                            ads.unity_banner_placement_id,
                            ads.unity_interstitial_placement_id,
                            ads.applovin_banner_ad_unit_id,
                            ads.applovin_interstitial_ad_unit_id,
                            ads.applovin_native_ad_manual_unit_id,
                            ads.applovin_banner_zone_id,
                            ads.applovin_interstitial_zone_id,
                            ads.ironsource_app_key,
                            ads.ironsource_banner_id,
                            ads.ironsource_interstitial_id,
                            ads.interstitial_ad_interval,
                            ads.native_ad_index
                    );

                    sharedPref.setConfig(
                            settings.privacy_policy,
                            settings.publisher_info,
                            settings.login_feature,
                            settings.comment_approval,
                            settings.video_menu,
                            settings.more_apps_url,
                            settings.youtube_api_key,
                            license.item_id,
                            license.item_name,
                            license.license_type
                    );

                    if (license.item_id.equals(ITEM_ID) && license.item_name.equals(ITEM_NAME)) {
                        if (app.status != null && app.status.equals("0")) {
                            new AlertDialog.Builder(ActivitySplash.this)
                                    .setTitle(getString(R.string.redirect_title))
                                    .setMessage(getString(R.string.redirect_message))
                                    .setPositiveButton(getString(R.string.dialog_ok), (dialog, which) -> {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(app.redirect_url)));
                                        finish();
                                    })
                                    .setCancelable(false)
                                    .show();
                            Log.d(TAG, "App is inactive, call redirect method");
                        } else {
                            startMainActivity();
                            Log.d(TAG, "App is active");
                        }
                        Log.d(TAG, "License is valid");
                    } else {
                        new AlertDialog.Builder(ActivitySplash.this)
                                .setTitle("Invalid License")
                                .setMessage("Whoops! this application cannot be accessed due to invalid item purchase code, if you are the owner of this application, please buy this item officially on Codecanyon to get item purchase code so that the application can be accessed by your users.")
                                .setPositiveButton("Buy this item", (dialog, which) -> {
                                    finish();
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://codecanyon.net/item/android-news-app/10771397")));
                                })
                                .setNegativeButton("Later", (dialog, which) -> finish())
                                .setCancelable(false)
                                .show();
                    }

                }
            }

            public void onFailure(@NonNull Call<CallbackSettings> call, @NonNull Throwable th) {
                Log.e(TAG, "onFailure : " + th.getMessage());
                startMainActivity();
            }
        });
    }

    private void startMainActivity() {
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }, DELAY_SPLASH_SCREEN);
    }

}
