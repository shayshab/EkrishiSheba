package com.app.androidkrishiapp.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.app.androidkrishiapp.BuildConfig;
import com.app.androidkrishiapp.R;
import com.app.androidkrishiapp.adapter.AdapterSearch;
import com.app.androidkrishiapp.callbacks.CallbackUser;
import com.app.androidkrishiapp.database.prefs.SharedPref;
import com.app.androidkrishiapp.models.User;
import com.app.androidkrishiapp.rests.ApiInterface;
import com.app.androidkrishiapp.rests.RestAdapter;
import com.app.androidkrishiapp.utils.Constant;
import com.app.androidkrishiapp.utils.Tools;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityProfile extends AppCompatActivity {

    MyApplication myApplication;
    User user;
    RelativeLayout lytUser;
    View lytSignIn, lytSignOut;
    TextView txtLogin;
    TextView txtRegister, txtUsername, txtEmail;
    ImageView imgProfile;
    Call<CallbackUser> callbackCall = null;
    ProgressDialog progressDialog;
    Button btnLogout;
    SharedPref sharedPref;
    SwitchMaterial switchTheme;
    RelativeLayout btnSwitchTheme;
    RelativeLayout btnTextSize;
    RelativeLayout btnNotification;
    RelativeLayout btnClearSearchHistory;
    RelativeLayout btnPublisherInfo;
    RelativeLayout btnPrivacyPolicy;
    RelativeLayout btnShare;
    RelativeLayout btnRate;
    RelativeLayout btnMore;
    RelativeLayout btnAbout;
    AdapterSearch adapterSearch;
    private String singleChoiceSelected;
    LinearLayout parentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_profile);
        Tools.setNavigation(this);

        sharedPref = new SharedPref(this);
        myApplication = MyApplication.getInstance();

        setupToolbar();
        initView();
        initSettings();
        requestAction();

    }

    public void setupToolbar() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            if (sharedPref.getLoginFeature().equals("yes")) {
                getSupportActionBar().setTitle(getResources().getString(R.string.title_menu_profile));
            } else {
                getSupportActionBar().setTitle(getResources().getString(R.string.title_menu_settings));
            }
        }
    }

    private void initView() {

        parentView = findViewById(R.id.parent_view);
        lytUser = findViewById(R.id.lyt_user);

        lytSignIn = findViewById(R.id.view_sign_in);
        lytSignOut = findViewById(R.id.view_sign_out);

        txtLogin = findViewById(R.id.btn_login);
        txtRegister = findViewById(R.id.txt_register);

        txtUsername = findViewById(R.id.txt_username);
        txtEmail = findViewById(R.id.txt_email);
        imgProfile = findViewById(R.id.img_profile);

        switchTheme = findViewById(R.id.switch_theme);
        btnSwitchTheme = findViewById(R.id.btn_switch_theme);
        btnTextSize = findViewById(R.id.btn_text_size);
        btnNotification = findViewById(R.id.btn_notification);
        btnClearSearchHistory = findViewById(R.id.btn_clear_search_history);
        btnPublisherInfo = findViewById(R.id.btn_publisher_info);
        btnPrivacyPolicy = findViewById(R.id.btn_privacy_policy);
        btnShare = findViewById(R.id.btn_share);
        btnRate = findViewById(R.id.btn_rate);
        btnMore = findViewById(R.id.btn_more);
        btnAbout = findViewById(R.id.btn_about);

        btnLogout = findViewById(R.id.btn_logout);

        viewVisibility();

    }

    private void viewVisibility() {
        btnSwitchTheme.setVisibility(View.VISIBLE);
        btnTextSize.setVisibility(View.VISIBLE);
        btnNotification.setVisibility(View.VISIBLE);
        btnClearSearchHistory.setVisibility(View.VISIBLE);
        btnPublisherInfo.setVisibility(View.VISIBLE);
        btnPrivacyPolicy.setVisibility(View.VISIBLE);
        btnShare.setVisibility(View.VISIBLE);
        btnRate.setVisibility(View.VISIBLE);
        btnMore.setVisibility(View.VISIBLE);
        btnAbout.setVisibility(View.VISIBLE);
    }

    private void initSettings() {

        if (!sharedPref.getLoginFeature().equals("yes")) {
            lytUser.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);
        }

        switchTheme.setChecked(sharedPref.getIsDarkTheme());
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.e("INFO", "" + isChecked);
            sharedPref.setIsDarkTheme(isChecked);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        btnSwitchTheme.setOnClickListener(v -> {
            if (switchTheme.isChecked()) {
                sharedPref.setIsDarkTheme(false);
                switchTheme.setChecked(false);
            } else {
                sharedPref.setIsDarkTheme(true);
                switchTheme.setChecked(true);
            }
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }, 200);
        });

        btnTextSize.setOnClickListener(v -> {
            String[] items = getResources().getStringArray(R.array.dialog_font_size);
            singleChoiceSelected = items[sharedPref.getFontSize()];
            int itemSelected = sharedPref.getFontSize();
            new AlertDialog.Builder(ActivityProfile.this)
                    .setTitle(getString(R.string.title_dialog_font_size))
                    .setSingleChoiceItems(items, itemSelected, (dialogInterface, i) -> singleChoiceSelected = items[i])
                    .setPositiveButton(R.string.dialog_ok, (dialogInterface, i) -> {
                        if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_xsmall))) {
                            sharedPref.updateFontSize(0);
                        } else if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_small))) {
                            sharedPref.updateFontSize(1);
                        } else if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_medium))) {
                            sharedPref.updateFontSize(2);
                        } else if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_large))) {
                            sharedPref.updateFontSize(3);
                        } else if (singleChoiceSelected.equals(getResources().getString(R.string.font_size_xlarge))) {
                            sharedPref.updateFontSize(4);
                        } else {
                            sharedPref.updateFontSize(2);
                        }
                        dialogInterface.dismiss();
                    })
                    .show();
        });

        btnNotification.setOnClickListener(v -> {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID);
            } else {
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", BuildConfig.APPLICATION_ID);
                intent.putExtra("app_uid", getApplicationInfo().uid);
            }
            startActivity(intent);
        });

        btnClearSearchHistory.setOnClickListener(v -> clearSearchHistory());

        btnPublisherInfo.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ActivityPublisherInfo.class)));

        btnPrivacyPolicy.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ActivityPrivacyPolicy.class)));

        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_content) + "\n" + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
            intent.setType("text/plain");
            startActivity(intent);
        });

        btnRate.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID))));

        btnMore.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(sharedPref.getMoreAppsUrl()))));

        btnAbout.setOnClickListener(v -> aboutDialog());

    }

    private void requestAction() {
        if (myApplication.getIsLogin()) {
            lytSignIn.setVisibility(View.VISIBLE);
            lytSignOut.setVisibility(View.GONE);

            btnLogout.setVisibility(View.VISIBLE);
            btnLogout.setOnClickListener(view -> logoutDialog());

            requestPostApi();
        } else {
            lytSignIn.setVisibility(View.GONE);
            lytSignOut.setVisibility(View.VISIBLE);
            txtLogin.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ActivityUserLogin.class)));
            txtRegister.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ActivityUserRegister.class)));
            btnLogout.setVisibility(View.GONE);
        }
    }

    private void requestPostApi() {
        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        callbackCall = apiInterface.getUser(myApplication.getUserId());
        callbackCall.enqueue(new Callback<CallbackUser>() {
            @Override
            public void onResponse(@NonNull Call<CallbackUser> call, @NonNull Response<CallbackUser> response) {
                CallbackUser resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    user = resp.response;
                    displayData();
                } else {
                    onFailRequest();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackUser> call, @NonNull Throwable t) {
                if (!call.isCanceled()) onFailRequest();
            }

        });
    }

    public void displayData() {

        ((TextView) findViewById(R.id.txt_username)).setText(Tools.usernameFormatter(user.name));

        ((TextView) findViewById(R.id.txt_email)).setText(user.email);

        ImageView img_profile = findViewById(R.id.img_profile);
        if (user.image.equals("")) {
            img_profile.setImageResource(R.drawable.ic_user_account);
        } else {
            Glide.with(this)
                    .load(sharedPref.getBaseUrl() + "/upload/avatar/" + user.image.replace(" ", "%20"))
                    .placeholder(R.drawable.ic_user_account)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .apply(new RequestOptions().override(256, 256))
                    .centerCrop()
                    .into(img_profile);
        }

        RelativeLayout btn_edit = findViewById(R.id.btn_edit);
        btn_edit.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), ActivityEditProfile.class);
            intent.putExtra("name", Tools.usernameFormatter(user.name));
            intent.putExtra("email", user.email);
            intent.putExtra("user_image", user.image);
            intent.putExtra("password", user.password);
            startActivity(intent);
        });

    }

    private void onFailRequest() {
        if (!Tools.isConnect(this)) {
            Toast.makeText(getApplicationContext(), getString(R.string.msg_no_network), Toast.LENGTH_SHORT).show();
        }
    }

    public void logoutDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityProfile.this);
        builder.setTitle(R.string.logout_title);
        builder.setMessage(R.string.logout_message);
        builder.setPositiveButton(R.string.dialog_yes, (di, i) -> {

            progressDialog = new ProgressDialog(ActivityProfile.this);
            progressDialog.setTitle(getResources().getString(R.string.title_please_wait));
            progressDialog.setMessage(getResources().getString(R.string.logout_process));
            progressDialog.setCancelable(false);
            progressDialog.show();

            MyApplication.getInstance().saveIsLogin(false);

            new Handler().postDelayed(() -> {
                progressDialog.dismiss();
                AlertDialog.Builder builder1 = new AlertDialog.Builder(ActivityProfile.this);
                builder1.setMessage(R.string.logout_success);
                builder1.setPositiveButton(R.string.dialog_ok, (dialogInterface, i1) -> finish());
                builder1.setCancelable(false);
                builder1.show();
            }, Constant.DELAY_PROGRESS_DIALOG);

        });
        builder.setNegativeButton(R.string.dialog_cancel, null);
        builder.show();

    }

    public void aboutDialog() {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(ActivityProfile.this);
        View view = layoutInflaterAndroid.inflate(R.layout.custom_dialog_about, null);
        ((TextView) view.findViewById(R.id.txt_app_version)).setText(BuildConfig.VERSION_NAME + "");
        final AlertDialog.Builder alert = new AlertDialog.Builder(ActivityProfile.this);
        alert.setView(view);
        alert.setCancelable(false);
        alert.setPositiveButton(R.string.dialog_ok, (dialog, which) -> dialog.dismiss());
        alert.show();
    }

    private void clearSearchHistory() {
        adapterSearch = new AdapterSearch(this);
        if (adapterSearch.getItemCount() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ActivityProfile.this);
            builder.setTitle(getString(R.string.title_dialog_clear_search_history));
            builder.setMessage(getString(R.string.msg_dialog_clear_search_history));
            builder.setPositiveButton(R.string.dialog_yes, (di, i) -> {
                progressDialog = new ProgressDialog(ActivityProfile.this);
                progressDialog.setTitle(getResources().getString(R.string.title_please_wait));
                progressDialog.setMessage(getResources().getString(R.string.clearing_process));
                progressDialog.setCancelable(false);
                progressDialog.show();
                adapterSearch.clearSearchHistory();
                new Handler().postDelayed(() -> {
                    progressDialog.dismiss();
                    Snackbar.make(parentView, getString(R.string.clearing_success), Snackbar.LENGTH_SHORT).show();
                }, Constant.DELAY_PROGRESS_DIALOG);
            });
            builder.setNegativeButton(R.string.dialog_cancel, null);
            builder.show();
        } else {
            Snackbar.make(parentView, getString(R.string.clearing_empty), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requestAction();
    }

}
