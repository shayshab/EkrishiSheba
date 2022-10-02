package com.app.androidkrishiapp.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.app.androidkrishiapp.R;
import com.app.androidkrishiapp.database.prefs.SharedPref;
import com.app.androidkrishiapp.models.User;
import com.app.androidkrishiapp.rests.ApiInterface;
import com.app.androidkrishiapp.rests.RestAdapter;
import com.app.androidkrishiapp.utils.Constant;
import com.app.androidkrishiapp.utils.Tools;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityEditProfile extends AppCompatActivity {

    EditText edtEmail, edtName, edtPassword;
    MyApplication myApplication;
    Button btnUpdate;
    Button btnLogout;
    RelativeLayout lytProfile;
    FloatingActionButton imgChange;
    Bitmap bitmap;
    ImageView profileImage;
    ImageView tmpImage;
    ProgressDialog progressDialog;
    String strName, strEmail, strImage, strPassword, strNewImage, strOldImage;
    private static final int IMAGE = 100;
    SharedPref sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_edit_profile);
        Tools.setNavigation(this);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPref = new SharedPref(this);
        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle(R.string.title_menu_edit_profile);
        }

        Intent intent = getIntent();
        strName = intent.getStringExtra("name");
        strEmail = intent.getStringExtra("email");
        strImage = intent.getStringExtra("user_image");
        strPassword = intent.getStringExtra("password");

        progressDialog = new ProgressDialog(ActivityEditProfile.this);
        progressDialog.setTitle(getResources().getString(R.string.title_please_wait));
        progressDialog.setMessage(getResources().getString(R.string.logout_process));
        progressDialog.setCancelable(false);

        myApplication = MyApplication.getInstance();

        profileImage = findViewById(R.id.profile_image);
        tmpImage = findViewById(R.id.tmp_image);
        imgChange = findViewById(R.id.btn_change_image);

        lytProfile = findViewById(R.id.lyt_profile);

        edtEmail = findViewById(R.id.edt_email);
        edtName = findViewById(R.id.edt_user);
        edtPassword = findViewById(R.id.edt_password);

        edtName.setText(strName);
        edtEmail.setText(strEmail);
        edtPassword.setText(strPassword);
        displayProfileImage();

        imgChange.setOnClickListener(view -> selectImage());

        btnUpdate = findViewById(R.id.btn_update);
        btnUpdate.setOnClickListener(view -> updateUserData());

        btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(view -> logoutDialog());

    }

    public void logoutDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(ActivityEditProfile.this);
        builder.setTitle(R.string.logout_title);
        builder.setMessage(R.string.logout_message);
        builder.setPositiveButton(R.string.dialog_yes, (di, i) -> {

            progressDialog = new ProgressDialog(ActivityEditProfile.this);
            progressDialog.setTitle(getResources().getString(R.string.title_please_wait));
            progressDialog.setMessage(getResources().getString(R.string.logout_process));
            progressDialog.setCancelable(false);
            progressDialog.show();

            MyApplication.getInstance().saveIsLogin(false);

            new Handler().postDelayed(() -> {
                progressDialog.dismiss();
                androidx.appcompat.app.AlertDialog.Builder builder1 = new androidx.appcompat.app.AlertDialog.Builder(ActivityEditProfile.this);
                builder1.setMessage(R.string.logout_success);
                builder1.setPositiveButton(R.string.dialog_ok, (dialogInterface, i1) -> finish());
                builder1.setCancelable(false);
                builder1.show();
            }, Constant.DELAY_PROGRESS_DIALOG);

        });
        builder.setNegativeButton(R.string.dialog_cancel, null);
        builder.show();

    }

    private void displayProfileImage() {
        if (strImage.equals("")) {
            profileImage.setImageResource(R.drawable.ic_user_account);
        } else {
            Glide.with(this)
                    .load(sharedPref.getBaseUrl() + "/upload/avatar/" + strImage.replace(" ", "%20"))
                    .placeholder(R.drawable.ic_user_account)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .apply(new RequestOptions().override(256, 256))
                    .centerCrop()
                    .into(profileImage);
        }
    }

    @SuppressWarnings("deprecation")
    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE);
    }

    private String convertToString() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imgByte = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(imgByte, Base64.DEFAULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE && resultCode == RESULT_OK && data != null) {
            Uri path = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), path);
                tmpImage.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateUserData() {

        progressDialog = new ProgressDialog(ActivityEditProfile.this);
        progressDialog.setTitle(R.string.updating_profile);
        progressDialog.setMessage(getResources().getString(R.string.waiting_message));
        progressDialog.setCancelable(false);
        progressDialog.show();

        strName = edtName.getText().toString();
        strEmail = edtEmail.getText().toString();
        strPassword = edtPassword.getText().toString();

        if (bitmap != null) {
            uploadImage();
        } else {
            updateData();
        }
    }

    private void updateData() {

        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        Call<User> call = apiInterface.updateUserData(myApplication.getUserId(), strName, strEmail, strPassword);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                new Handler().postDelayed(() -> {
                    progressDialog.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(ActivityEditProfile.this);
                    builder.setMessage(R.string.success_updating_profile);
                    builder.setPositiveButton(getResources().getString(R.string.dialog_ok), (dialogInterface, i) -> finish());
                    builder.setCancelable(false);
                    builder.show();
                }, Constant.DELAY_PROGRESS_DIALOG);
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                progressDialog.dismiss();
            }
        });

    }

    private void uploadImage() {

        strOldImage = strImage;
        strNewImage = convertToString();

        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        Call<User> call = apiInterface.updatePhotoProfile(myApplication.getUserId(), strName, strEmail, strPassword, strOldImage, strNewImage);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                new Handler().postDelayed(() -> {
                    progressDialog.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(ActivityEditProfile.this);
                    builder.setMessage(R.string.success_updating_profile);
                    builder.setPositiveButton(getResources().getString(R.string.dialog_ok), (dialogInterface, i) -> finish());
                    builder.setCancelable(false);
                    builder.show();
                }, Constant.DELAY_PROGRESS_DIALOG);
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                progressDialog.dismiss();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
