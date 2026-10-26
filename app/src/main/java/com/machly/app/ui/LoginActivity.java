package com.machly.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.machly.app.R;
import com.machly.app.api.ApiClient;
import com.machly.app.api.ApiService;
import com.machly.app.db.DbHelper;
import com.machly.app.models.LoginRequest;
import com.machly.app.models.LoginResponse;
import com.machly.app.models.User;
import com.machly.app.storage.PrefsManager;
import com.machly.app.sync.SyncManager;
import com.machly.app.utils.BCryptUtils;
import com.machly.app.utils.NetworkUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private PrefsManager prefsManager;
    private DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefsManager = new PrefsManager(this);
        dbHelper = new DbHelper(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (prefsManager.getToken() != null) {
            // Check expiry if needed
            startMainActivity();
        }
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (NetworkUtils.isOnline(this)) {
            performOnlineLogin(email, password);
        } else {
            performOfflineLogin(email, password);
        }
    }

    private void performOnlineLogin(String email, String password) {
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    prefsManager.saveToken(loginResponse.getToken(), loginResponse.getExpiresIn());

                    // Save user to local DB
                    User user = new User();
                    user.setUserIdServer(loginResponse.getUser().getUserId());
                    user.setFullName(loginResponse.getUser().getFullName());
                    user.setEmail(loginResponse.getUser().getEmail());
                    user.setLastUpdated(loginResponse.getUser().getLastUpdated());
                    
                    // Handle Role
                    long localRoleId = dbHelper.getLocalRoleIdByServerId(loginResponse.getUser().getRole().getRoleId());
                    // If role doesn't exist locally yet, we might need to fetch roles first or insert this one.
                    // For simplicity, let's assume sync will handle full data, but for login user we try to insert role if missing
                    if (localRoleId == -1) {
                         com.machly.app.models.Role r = new com.machly.app.models.Role();
                         r.setRoleIdServer(loginResponse.getUser().getRole().getRoleId());
                         r.setName(loginResponse.getUser().getRole().getName());
                         localRoleId = dbHelper.insertOrUpdateRole(r);
                    }
                    user.setRoleIdLocal(localRoleId);

                    // Password Hash
                    if (loginResponse.getUser().getPasswordHash() != null) {
                        user.setPasswordHash(loginResponse.getUser().getPasswordHash());
                    } else {
                        // Hash locally
                        user.setPasswordHash(BCryptUtils.hashPassword(password));
                    }

                    dbHelper.insertOrUpdateUser(user);

                    // Trigger Sync
                    SyncManager.getInstance().fetchAndSaveUsers(LoginActivity.this, null);

                    startMainActivity();
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performOfflineLogin(String email, String password) {
        User user = dbHelper.getUserByEmail(email);
        if (user == null) {
            Toast.makeText(this, "You must login online the first time.", Toast.LENGTH_LONG).show();
        } else {
            if (BCryptUtils.checkPassword(password, user.getPasswordHash())) {
                Toast.makeText(this, "Offline mode", Toast.LENGTH_SHORT).show();
                startMainActivity();
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
