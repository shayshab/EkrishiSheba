package com.app.androidkrishiapp.activities;

import static com.app.androidkrishiapp.utils.Tools.PERMISSIONS_REQUEST;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.app.androidkrishiapp.R;
import com.app.androidkrishiapp.database.prefs.SharedPref;
import com.app.androidkrishiapp.utils.Tools;
import com.app.androidkrishiapp.utils.TouchImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class ActivityWebViewImage extends AppCompatActivity {

    TouchImageView newsImage;
    String strImage;
    SharedPref sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_full_screen_image);
        Tools.setNavigation(this);
        sharedPref = new SharedPref(this);
        strImage = getIntent().getStringExtra("image_url");

        newsImage = findViewById(R.id.image);

        Glide.with(this)
                .load(strImage.replace(" ", "%20"))
                .placeholder(R.drawable.ic_thumbnail)
                .thumbnail(Tools.RequestBuilder(this))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(newsImage);

        initToolbar();

    }

    private void initToolbar() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPref sharedPref = new SharedPref(this);
        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setTitle("");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_image, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {

            case R.id.close_image:
                new Handler().postDelayed(this::finish, 300);
                return true;

            case R.id.save_image:
                new Handler().postDelayed(this::requestStoragePermission, 300);
                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    public void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(ActivityWebViewImage.this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, PERMISSIONS_REQUEST);
            } else {
                downloadImage();
            }
        } else {
            downloadImage();
        }
    }

    public void downloadImage() {
        String image_name = strImage;
        String image_url = sharedPref.getBaseUrl() + "/upload/" + strImage;
        Tools.downloadImage(this, image_name, image_url, "image/jpeg");
    }

}
