package com.machly.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.machly.app.R;
import com.machly.app.api.ApiClient;
import com.machly.app.api.ApiService;
import com.machly.app.models.LoginRequest;
import com.machly.app.models.LoginResponse;
import com.machly.app.models.RegisterRequest;
import com.machly.app.storage.PrefsManager;
import com.machly.app.util.ApiError;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private boolean registerMode;
    private TextView tabLogin, tabRegister, tvTitle, tvSubtitle, tvError, tvDemo;
    private View registerFields;
    private TextInputEditText etEmail, etPassword, etFullName, etPhone;
    private RadioGroup rgRole;
    private MaterialButton btnSubmit;
    private PrefsManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        prefs = new PrefsManager(this);

        ImageButton btnBack = findViewById(R.id.btnBack);
        tabLogin = findViewById(R.id.tabLogin);
        tabRegister = findViewById(R.id.tabRegister);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvError = findViewById(R.id.tvError);
        tvDemo = findViewById(R.id.tvDemo);
        registerFields = findViewById(R.id.registerFields);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        rgRole = findViewById(R.id.rgRole);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnBack.setOnClickListener(v -> finish());
        tabLogin.setOnClickListener(v -> setMode(false));
        tabRegister.setOnClickListener(v -> setMode(true));
        btnSubmit.setOnClickListener(v -> submit());
        setMode(false);
    }

    private void setMode(boolean register) {
        registerMode = register;
        registerFields.setVisibility(register ? View.VISIBLE : View.GONE);
        tvDemo.setVisibility(register ? View.GONE : View.VISIBLE);
        tvTitle.setText(register ? R.string.create_account : R.string.welcome_back);
        tvSubtitle.setText(register ? R.string.register_subtitle : R.string.login_subtitle);
        btnSubmit.setText(register ? R.string.register : R.string.login);
        tabLogin.setBackgroundResource(register ? 0 : R.drawable.bg_tab_active);
        tabRegister.setBackgroundResource(register ? R.drawable.bg_tab_active : 0);
        tabLogin.setTextColor(getResources().getColor(register ? R.color.forest : R.color.white));
        tabRegister.setTextColor(getResources().getColor(register ? R.color.white : R.color.forest));
        tvError.setVisibility(View.GONE);
    }

    private void submit() {
        String email = text(etEmail);
        String password = text(etPassword);
        tvError.setVisibility(View.GONE);
        btnSubmit.setEnabled(false);
        ApiService api = ApiClient.getClient(this).create(ApiService.class);

        if (registerMode) {
            String fullName = text(etFullName);
            String phone = text(etPhone);
            String role = ((RadioButton) findViewById(rgRole.getCheckedRadioButtonId())).getId() == R.id.rbOwner
                    ? "owner" : "renter";
            api.register(new RegisterRequest(fullName, email, phone, password, role))
                    .enqueue(authCallback("No se pudo crear la cuenta."));
        } else {
            api.login(new LoginRequest(email, password))
                    .enqueue(authCallback("Correo o contraseña incorrectos."));
        }
    }

    private Callback<LoginResponse> authCallback(String fallback) {
        return new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnSubmit.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    prefs.saveSession(response.body());
                    Toast.makeText(LoginActivity.this, "Sesión iniciada", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    showError(ApiError.message(response, fallback));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnSubmit.setEnabled(true);
                showError("No hay conexión con el servidor.");
            }
        };
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private String text(TextInputEditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString().trim();
    }
}
