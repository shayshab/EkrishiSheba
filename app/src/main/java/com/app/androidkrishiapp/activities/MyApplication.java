package com.app.androidkrishiapp.activities;

import static com.app.androidkrishiapp.utils.Constant.LOCALHOST_ADDRESS;
import static com.solodroid.ads.sdk.util.Constant.ADMOB;
import static com.solodroid.ads.sdk.util.Constant.AD_STATUS_ON;
import static com.solodroid.ads.sdk.util.Constant.GOOGLE_AD_MANAGER;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDex;

import com.app.androidkrishiapp.BuildConfig;
import com.app.androidkrishiapp.callbacks.CallbackSettings;
import com.app.androidkrishiapp.config.AppConfig;
import com.app.androidkrishiapp.database.prefs.AdsPref;
import com.app.androidkrishiapp.models.Settings;
import com.app.androidkrishiapp.rests.RestAdapter;
import com.app.androidkrishiapp.utils.Tools;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onesignal.OneSignal;
import com.solodroid.ads.sdk.format.AppOpenAdManager;
import com.solodroid.ads.sdk.format.AppOpenAdMob;
import com.solodroid.ads.sdk.util.OnShowAdCompleteListener;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks, LifecycleObserver {

    public static final String TAG = "MyApplication";
    public String prefName = "news_pref";
    public static MyApplication mInstance;
    FirebaseAnalytics mFirebaseAnalytics;
    public SharedPreferences preferences;
    private AppOpenAdMob appOpenAdMob;
    private AppOpenAdManager appOpenAdManager;
    Activity currentActivity;
    AdsPref adsPref;
    String message = "";
    String bigPicture = "";
    String title = "";
    String link = "";
    long post_id = -1;
    long uniqueId = -1;
    Settings settings;

    public MyApplication() {
        mInstance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.registerActivityLifecycleCallbacks(this);
        adsPref = new AdsPref(this);
        MobileAds.initialize(this, initializationStatus -> {
        });
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        appOpenAdMob = new AppOpenAdMob();
        appOpenAdManager = new AppOpenAdManager();
        mInstance = this;
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        OneSignal.disablePush(false);
        Log.d(TAG, "OneSignal Notification is enabled");

        // Enable verbose OneSignal logging to debug issues if needed.
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
        OneSignal.initWithContext(this);
        requestTopic();

        OneSignal.setNotificationOpenedHandler(
                result -> {
                    title = result.getNotification().getTitle();
                    message = result.getNotification().getBody();
                    bigPicture = result.getNotification().getBigPicture();
                    Log.d(TAG, title + ", " + message + ", " + bigPicture);
                    try {
                        uniqueId = result.getNotification().getAdditionalData().getLong("unique_id");
                        post_id = result.getNotification().getAdditionalData().getLong("post_id");
                        link = result.getNotification().getAdditionalData().getString("link");
                        Log.d(TAG, post_id + ", " + uniqueId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("unique_id", uniqueId);
                    intent.putExtra("post_id", post_id);
                    intent.putExtra("title", title);
                    intent.putExtra("link", link);
                    startActivity(intent);
                });

        OneSignal.unsubscribeWhenNotificationsAreDisabled(true);

    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

    private void requestTopic() {
        if (!AppConfig.SERVER_KEY.contains("XXXX")) {
            String decode = Tools.decodeBase64(AppConfig.SERVER_KEY);
            String data = Tools.decrypt(decode);
            String[] results = data.split("_applicationId_");
            String baseUrl = results[0].replace("http://localhost", LOCALHOST_ADDRESS);

            Call<CallbackSettings> callbackCall = RestAdapter.createAPI(baseUrl).getSettings(BuildConfig.APPLICATION_ID, AppConfig.REST_API_KEY);
            callbackCall.enqueue(new Callback<CallbackSettings>() {
                public void onResponse(@NonNull Call<CallbackSettings> call, @NonNull Response<CallbackSettings> response) {
                    CallbackSettings resp = response.body();
                    if (resp != null && resp.status.equals("ok")) {
                        settings = resp.settings;
                        if (!settings.fcm_notification_topic.equals("0")) {
                            FirebaseMessaging.getInstance().subscribeToTopic(settings.fcm_notification_topic);
                        }
                        if (!settings.onesignal_app_id.equals("0")) {
                            OneSignal.setAppId(settings.onesignal_app_id);
                        }
                        Log.d(TAG, "FCM Subscribe topic : " + settings.fcm_notification_topic);
                        Log.d(TAG, "OneSignal App ID : " + settings.onesignal_app_id);
                    }
                }

                public void onFailure(@NonNull Call<CallbackSettings> call, @NonNull Throwable th) {
                    Log.e("onFailure", "" + th.getMessage());
                }
            });
        }
    }

    public void saveIsLogin(boolean flag) {
        preferences = this.getSharedPreferences(prefName, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("IsLoggedIn", flag);
        editor.apply();
    }

    public boolean getIsLogin() {
        preferences = this.getSharedPreferences(prefName, 0);
        if (preferences != null) {
            return preferences.getBoolean("IsLoggedIn", false);
        }
        return false;
    }

    public void saveLogin(String user_id, String user_name, String email) {
        preferences = this.getSharedPreferences(prefName, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("user_id", user_id);
        editor.putString("user_name", user_name);
        editor.putString("email", email);
        editor.apply();
    }

    public String getUserId() {
        preferences = this.getSharedPreferences(prefName, 0);
        if (preferences != null) {
            return preferences.getString("user_id", "");
        }
        return "";
    }

    public String getUserName() {
        preferences = this.getSharedPreferences(prefName, 0);
        if (preferences != null) {
            return preferences.getString("user_name", "");
        }
        return "";
    }

    public String getUserEmail() {
        preferences = this.getSharedPreferences(prefName, 0);
        if (preferences != null) {
            return preferences.getString("email", "");
        }
        return "";
    }

    public String getType() {
        preferences = this.getSharedPreferences(prefName, 0);
        if (preferences != null) {
            return preferences.getString("type", "");
        }
        return "";
    }

    public void saveType(String type) {
        preferences = this.getSharedPreferences(prefName, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("type", type);
        editor.apply();
    }

    @SuppressWarnings("deprecation")
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected void onMoveToForeground() {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            switch (adsPref.getAdType()) {
                case ADMOB:
                    if (!adsPref.getAdMobAppOpenAdId().equals("0")) {
                        if (!currentActivity.getIntent().hasExtra("unique_id")) {
                            appOpenAdMob.showAdIfAvailable(currentActivity, adsPref.getAdMobAppOpenAdId());
                        }
                    }
                    break;
                case GOOGLE_AD_MANAGER:
                    if (!adsPref.getAdManagerAppOpenAdId().equals("0")) {
                        if (!currentActivity.getIntent().hasExtra("unique_id")) {
                            appOpenAdManager.showAdIfAvailable(currentActivity, adsPref.getAdManagerAppOpenAdId());
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            switch (adsPref.getAdType()) {
                case ADMOB:
                    if (!adsPref.getAdMobAppOpenAdId().equals("0")) {
                        if (!appOpenAdMob.isShowingAd) {
                            currentActivity = activity;
                        }
                    }
                    break;
                case GOOGLE_AD_MANAGER:
                    if (!adsPref.getAdManagerAppOpenAdId().equals("0")) {
                        if (!appOpenAdManager.isShowingAd) {
                            currentActivity = activity;
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }

    public void showAdIfAvailable(@NonNull Activity activity, @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication class
        if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            switch (adsPref.getAdType()) {
                case ADMOB:
                    if (!adsPref.getAdMobAppOpenAdId().equals("0")) {
                        appOpenAdMob.showAdIfAvailable(activity, adsPref.getAdMobAppOpenAdId(), onShowAdCompleteListener);
                    }
                    break;
                case GOOGLE_AD_MANAGER:
                    if (!adsPref.getAdManagerAppOpenAdId().equals("0")) {
                        appOpenAdManager.showAdIfAvailable(activity, adsPref.getAdManagerAppOpenAdId(), onShowAdCompleteListener);
                    }
                    break;
            }
        }
    }

}
