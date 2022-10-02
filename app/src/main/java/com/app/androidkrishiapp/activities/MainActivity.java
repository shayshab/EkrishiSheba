package com.app.androidkrishiapp.activities;

import static com.app.androidkrishiapp.utils.Constant.BANNER_HOME;
import static com.app.androidkrishiapp.utils.Constant.INTERSTITIAL_POST_LIST;

import android.content.Intent;
import android.content.IntentSender;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.viewpager.widget.ViewPager;

import com.app.androidkrishiapp.BuildConfig;
import com.app.androidkrishiapp.R;
import com.app.androidkrishiapp.callbacks.CallbackUser;
import com.app.androidkrishiapp.config.AppConfig;
import com.app.androidkrishiapp.database.prefs.AdsPref;
import com.app.androidkrishiapp.database.prefs.SharedPref;
import com.app.androidkrishiapp.models.User;
import com.app.androidkrishiapp.rests.ApiInterface;
import com.app.androidkrishiapp.rests.RestAdapter;
import com.app.androidkrishiapp.utils.AdsManager;
import com.app.androidkrishiapp.utils.AppBarLayoutBehavior;
import com.app.androidkrishiapp.utils.Constant;
import com.app.androidkrishiapp.utils.RtlViewPager;
import com.app.androidkrishiapp.utils.Tools;
import com.app.androidkrishiapp.utils.ViewPagerHelper;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private long exitTime = 0;
    MyApplication myApplication;
    private BottomNavigationView navigation;
    private ViewPager viewPager;
    private RtlViewPager viewPagerRTL;
    TextView titleToolbar;
    User user;
    Call<CallbackUser> callbackCall = null;
    ImageView imgProfile;
    RelativeLayout btnProfile;
    ImageButton btnSearch;
    ImageButton btnSettings;
    SharedPref sharedPref;
    AdsPref adsPref;
    AdsManager adsManager;
    CoordinatorLayout parentView;
    ViewPagerHelper viewPagerHelper;
    private AppUpdateManager appUpdateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        adsPref = new AdsPref(this);
        setContentView(R.layout.activity_main);

        Tools.setNavigation(this);

        sharedPref = new SharedPref(this);
        viewPagerHelper = new ViewPagerHelper(this);
        adsManager = new AdsManager(this);

        AppBarLayout appBarLayout = findViewById(R.id.tab_appbar_layout);
        ((CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams()).setBehavior(new AppBarLayoutBehavior());

        myApplication = MyApplication.getInstance();

        parentView = findViewById(R.id.tab_coordinator_layout);
        titleToolbar = findViewById(R.id.title_toolbar);
        imgProfile = findViewById(R.id.img_profile);

        navigation = findViewById(R.id.navigation);
        navigation.getMenu().clear();
        if (sharedPref.getVideoMenu().equals("yes")) {
            navigation.inflateMenu(R.menu.menu_navigation_default);
        } else {
            navigation.inflateMenu(R.menu.menu_navigation_no_video);
        }
        navigation.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);

        viewPager = findViewById(R.id.viewpager);
        viewPagerRTL = findViewById(R.id.viewpager_rtl);
        if (AppConfig.ENABLE_RTL_MODE) {
            viewPagerHelper.setupViewPagerRTL(viewPagerRTL, navigation, titleToolbar);
        } else {
            viewPagerHelper.setupViewPager(viewPager, navigation, titleToolbar);
        }

        Tools.notificationOpenHandler(this, getIntent());

        initToolbarIcon();
        displayUserProfile();
        adsPref.saveCounter(1);

        adsManager.initializeAd();
        adsManager.updateConsentStatus();
        adsManager.loadBannerAd(BANNER_HOME);
        adsManager.loadInterstitialAd(INTERSTITIAL_POST_LIST, adsPref.getInterstitialAdInterval());

        if (!BuildConfig.DEBUG) {
            appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());
            inAppUpdate();
            inAppReview();
        }

        if (!Tools.isConnect(this)) {
            if (sharedPref.getVideoMenu().equals("yes")) {
                if (AppConfig.ENABLE_RTL_MODE) {
                    viewPagerRTL.setCurrentItem(3);
                } else {
                    viewPager.setCurrentItem(3);
                }
            } else {
                if (AppConfig.ENABLE_RTL_MODE) {
                    viewPagerRTL.setCurrentItem(2);
                } else {
                    viewPager.setCurrentItem(2);
                }
            }
        }

    }

    public void showInterstitialAd() {
        adsManager.showInterstitialAd();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void initToolbarIcon() {
        if (sharedPref.getIsDarkTheme()) {
            findViewById(R.id.toolbar).setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
            navigation.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
        } else {
            findViewById(R.id.toolbar).setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        btnSearch = findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(view -> new Handler().postDelayed(() -> {
            startActivity(new Intent(getApplicationContext(), ActivitySearch.class));
            destroyBannerAd();
        }, 50));

        btnProfile = findViewById(R.id.btn_profile);
        btnProfile.setOnClickListener(view -> new Handler().postDelayed(() -> startActivity(new Intent(getApplicationContext(), ActivityProfile.class)), 50));

        btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(view -> new Handler().postDelayed(() -> startActivity(new Intent(getApplicationContext(), ActivityProfile.class)), 50));

        if (sharedPref.getLoginFeature().equals("yes")) {
            btnProfile.setVisibility(View.VISIBLE);
            btnSettings.setVisibility(View.GONE);
        } else {
            btnProfile.setVisibility(View.GONE);
            btnSettings.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (AppConfig.ENABLE_RTL_MODE) {
            if (viewPagerRTL.getCurrentItem() != 0) {
                viewPagerRTL.setCurrentItem((0), true);
            } else {
                if (AppConfig.ENABLE_EXIT_DIALOG) {
                    exitDialog();
                } else {
                    exitApp();
                }
            }
        } else {
            if (viewPager.getCurrentItem() != 0) {
                viewPager.setCurrentItem((0), true);
            } else {
                if (AppConfig.ENABLE_EXIT_DIALOG) {
                    exitDialog();
                } else {
                    exitApp();
                }
            }
        }

    }

    public void exitApp() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            showSnackBar(getString(R.string.press_again_to_exit));
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    public void showSnackBar(String msg) {
        Snackbar.make(parentView, msg, Snackbar.LENGTH_SHORT).show();
    }

    public void exitDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle(R.string.dialog_close_title);
        dialog.setMessage(R.string.dialog_close_msg);
        dialog.setPositiveButton(R.string.dialog_option_quit, (dialogInterface, i) -> finish());

        dialog.setNegativeButton(R.string.dialog_option_rate_us, (dialogInterface, i) -> {
            final String appName = getPackageName();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appName)));
            }

            finish();
        });

        dialog.setNeutralButton(R.string.dialog_option_more, (dialogInterface, i) -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sharedPref.getMoreAppsUrl())));

            finish();
        });
        dialog.show();
    }

    private void displayUserProfile() {
        if (myApplication.getIsLogin()) {
            requestUserData();
        } else {
            imgProfile.setImageResource(R.drawable.ic_account_circle_white);
        }
    }

    private void requestUserData() {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        callbackCall = apiInterface.getUser(myApplication.getUserId());
        callbackCall.enqueue(new Callback<CallbackUser>() {
            @Override
            public void onResponse(@NonNull Call<CallbackUser> call, @NonNull Response<CallbackUser> response) {
                CallbackUser resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    user = resp.response;
                    if (user.image.equals("")) {
                        imgProfile.setImageResource(R.drawable.ic_account_circle_white);
                    } else {
                        Glide.with(MainActivity.this)
                                .load(sharedPref.getBaseUrl() + "/upload/avatar/" + user.image.replace(" ", "%20"))
                                .apply(new RequestOptions().override(54, 54))
                                .thumbnail(Tools.RequestBuilder(MainActivity.this))
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .centerCrop()
                                .placeholder(R.drawable.ic_account_circle_white)
                                .into(imgProfile);
                    }

                    if (user.status.equals("0")) {
                        dialogAccountDisabled();
                    }

                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackUser> call, @NonNull Throwable t) {
                if (sharedPref.getLoginFeature().equals("yes")) {
                    imgProfile.setImageResource(R.drawable.ic_account_circle_white);
                } else {
                    imgProfile.setImageResource(R.drawable.ic_settings_white);
                }
            }

        });
    }

    private void dialogAccountDisabled() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.whops);
        dialog.setMessage(R.string.login_disabled);
        dialog.setPositiveButton(R.string.dialog_ok, (dialogInterface, i) -> {
            MyApplication.getInstance().saveIsLogin(false);
            new Handler().postDelayed(this::recreate, 200);
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        displayUserProfile();
        adsManager.resumeBannerAd(BANNER_HOME);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyBannerAd();
    }

    public void destroyBannerAd() {
        adsManager.destroyBannerAd();
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    private void inAppReview() {
        if (sharedPref.getInAppReviewToken() <= 3) {
            sharedPref.updateInAppReviewToken(sharedPref.getInAppReviewToken() + 1);
        } else {
            ReviewManager manager = ReviewManagerFactory.create(this);
            Task<ReviewInfo> request = manager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ReviewInfo reviewInfo = task.getResult();
                    manager.launchReviewFlow(MainActivity.this, reviewInfo).addOnFailureListener(e -> {
                    }).addOnCompleteListener(complete -> {
                                Log.d(TAG, "In-App Review Success");
                            }
                    ).addOnFailureListener(failure -> {
                        Log.d(TAG, "In-App Review Rating Failed");
                    });
                }
            }).addOnFailureListener(failure -> Log.d("In-App Review", "In-App Request Failed " + failure));
        }
        Log.d(TAG, "in app review token : " + sharedPref.getInAppReviewToken());
    }

    private void inAppUpdate() {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startUpdateFlow(appUpdateInfo);
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdateFlow(appUpdateInfo);
            }
        });
    }

    private void startUpdateFlow(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, Constant.IMMEDIATE_APP_UPDATE_REQ_CODE);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.IMMEDIATE_APP_UPDATE_REQ_CODE) {
            if (resultCode == RESULT_CANCELED) {
                showSnackBar(getString(R.string.msg_cancel_update));
            } else if (resultCode == RESULT_OK) {
                showSnackBar(getString(R.string.msg_success_update));
            } else {
                showSnackBar(getString(R.string.msg_failed_update));
                inAppUpdate();
            }
        }
    }

}
