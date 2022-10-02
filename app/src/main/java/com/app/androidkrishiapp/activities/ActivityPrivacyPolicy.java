package com.app.androidkrishiapp.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.app.androidkrishiapp.R;
import com.app.androidkrishiapp.database.prefs.SharedPref;
import com.app.androidkrishiapp.utils.Tools;

public class ActivityPrivacyPolicy extends AppCompatActivity {

    SharedPref sharedPref;
    WebView wb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_privacy_policy);
        Tools.setNavigation(this);
        sharedPref = new SharedPref(this);
        wb = findViewById(R.id.webView);


        wb.getSettings().setJavaScriptEnabled(true);
        wb.getSettings().setLoadWithOverviewMode(true);
        wb.getSettings().setUseWideViewPort(true);
        wb.getSettings().setBuiltInZoomControls(true);
        wb.getSettings().setPluginState(WebSettings.PluginState.ON);
      //  wb.getSettings().setPluginsEnabled(true);
       // wb.setWebViewClient(new HelloWebViewClient());
        wb.loadUrl("https://ekrishi.decoderssquad.com/privacypolicy.html");


        //Tools.displayPostDescription(this, webView, sharedPref.getPrivacyPolicy());
        setupToolbar();
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
            getSupportActionBar().setTitle(getResources().getString(R.string.title_about_privacy));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

}
