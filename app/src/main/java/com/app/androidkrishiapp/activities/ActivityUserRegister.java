package com.app.androidkrishiapp.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.app.androidkrishiapp.R;
import com.app.androidkrishiapp.database.prefs.SharedPref;
import com.app.androidkrishiapp.models.Value;
import com.app.androidkrishiapp.rests.ApiInterface;
import com.app.androidkrishiapp.rests.RestAdapter;
import com.app.androidkrishiapp.utils.Constant;
import com.app.androidkrishiapp.utils.Tools;

import id.solodroid.validationlibrary.Rule;
import id.solodroid.validationlibrary.Validator;
import id.solodroid.validationlibrary.annotation.Email;
import id.solodroid.validationlibrary.annotation.Password;
import id.solodroid.validationlibrary.annotation.Required;
import id.solodroid.validationlibrary.annotation.TextRule;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityUserRegister extends AppCompatActivity implements Validator.ValidationListener {

    @Required(order = 1)
    @TextRule(order = 2, minLength = 3, maxLength = 35, trim = true, message = "Enter Valid Full Name")
    EditText edtFullName;

    @Required(order = 3)
    @Email(order = 4, message = "Please Check and Enter a valid Email Address")
    EditText edtEmail;

    @Required(order = 5)
    @Password(order = 6, message = "Enter a Valid Password")
    @TextRule(order = 7, minLength = 6, message = "Enter a Password Correctly")
    EditText edtPassword;

    private Validator validator;

    Button btnSignUp, btnLogin;
    TextView txtTerms;
    String strFullName, strEmail, strPassword;

    SharedPref sharedPref;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        setContentView(R.layout.activity_user_register);
        Tools.setNavigation(this);

        sharedPref = new SharedPref(this);

        edtFullName = findViewById(R.id.edt_user);
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);

        txtTerms = findViewById(R.id.txt_terms);
        btnSignUp = findViewById(R.id.btn_register);
        btnLogin = findViewById(R.id.btn_login);

        txtTerms.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), ActivityPrivacyPolicy.class)));

        btnLogin.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(getApplicationContext(), ActivityUserLogin.class));
        });

        btnSignUp.setOnClickListener(v -> validator.validateAsync());

        validator = new Validator(this);
        validator.setValidationListener(this);

    }

    @Override
    public void onValidationSucceeded() {
        strFullName = edtFullName.getText().toString();
        strEmail = edtEmail.getText().toString();
        strPassword = edtPassword.getText().toString();

        if (Tools.isConnect(ActivityUserRegister.this)) {
            doRegister(strFullName, strEmail, strPassword);
        } else {
            dialogFailed();
        }

    }

    @Override
    public void onValidationFailed(View failedView, Rule<?> failedRule) {
        String message = failedRule.getFailureMessage();
        if (failedView instanceof EditText) {
            failedView.requestFocus();
            ((EditText) failedView).setError(message);
        }
    }

    private void doRegister(String name, String email, String password) {

        progressDialog = new ProgressDialog(ActivityUserRegister.this);
        progressDialog.setTitle(getResources().getString(R.string.title_please_wait));
        progressDialog.setMessage(getResources().getString(R.string.register_process));
        progressDialog.setCancelable(false);
        progressDialog.show();

        ApiInterface apiInterface = RestAdapter.createAPI(sharedPref.getBaseUrl());
        Call<Value> call = apiInterface.userRegister(name, email, password);
        call.enqueue(new Callback<Value>() {
            @Override
            public void onResponse(@NonNull Call<Value> call, @NonNull Response<Value> response) {
                assert response.body() != null;
                String value = response.body().value;

                new Handler().postDelayed(() -> {
                    progressDialog.dismiss();
                    if (value.equals("0")) {
                        dialogEmailAlreadyUsed();
                    } else if (value.equals("1")) {
                        dialogSuccess();
                    }
                }, Constant.DELAY_REFRESH);
            }

            @Override
            public void onFailure(@NonNull Call<Value> call, @NonNull Throwable t) {
                t.printStackTrace();
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), getString(R.string.msg_no_network), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void dialogEmailAlreadyUsed() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityUserRegister.this);
        dialog.setTitle(R.string.whops);
        dialog.setMessage(R.string.register_exist);
        dialog.setPositiveButton(R.string.dialog_ok, null);
        dialog.setCancelable(false);
        dialog.show();
        edtEmail.setText("");
        edtEmail.requestFocus();
    }

    public void dialogSuccess() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityUserRegister.this);
        dialog.setTitle(R.string.register_title);
        dialog.setMessage(R.string.register_success);
        dialog.setPositiveButton(R.string.dialog_ok, (dialogInterface, i) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityUserLogin.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    public void dialogFailed() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.whops);
        dialog.setMessage(R.string.msg_no_network);
        dialog.setPositiveButton(R.string.dialog_ok, null);
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
        } else {
            return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }
}

