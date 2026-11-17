package com.machly.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.machly.app.R;
import com.machly.app.api.ApiClient;
import com.machly.app.api.ApiService;
import com.machly.app.models.DashboardStats;
import com.machly.app.models.User;
import com.machly.app.storage.PrefsManager;
import com.machly.app.util.MoneyFormat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountActivity extends AppCompatActivity {
    private PrefsManager prefs;
    private View guestPanel, userPanel, dividerOwner1, dividerOwner2;
    private TextView tvGreeting, tvRole, tvEmail, tvPhone, tvAvatar;
    private LinearLayout statsRow;
    private View btnListings, btnRequests, btnPublish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        prefs = new PrefsManager(this);

        guestPanel = findViewById(R.id.guestPanel);
        userPanel = findViewById(R.id.userPanel);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvRole = findViewById(R.id.tvRole);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvAvatar = findViewById(R.id.tvAvatar);
        statsRow = findViewById(R.id.statsRow);
        btnListings = findViewById(R.id.btnListings);
        btnRequests = findViewById(R.id.btnRequests);
        btnPublish = findViewById(R.id.btnPublish);
        dividerOwner1 = findViewById(R.id.dividerOwner1);
        dividerOwner2 = findViewById(R.id.dividerOwner2);

        findViewById(R.id.btnLogin).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
        findViewById(R.id.btnBookings).setOnClickListener(v ->
                startActivity(BookingsActivity.intent(this, false)));
        findViewById(R.id.btnFavorites).setOnClickListener(v ->
                startActivity(new Intent(this, FavoritesActivity.class)));
        btnListings.setOnClickListener(v ->
                startActivity(ListingsActivity.intent(this)));
        btnRequests.setOnClickListener(v ->
                startActivity(BookingsActivity.intent(this, true)));
        btnPublish.setOnClickListener(v ->
                startActivity(new Intent(this, PublishEquipmentActivity.class)));
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            prefs.clearSession();
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
            render();
        });

        BottomNavHelper.bind(this, findViewById(R.id.bottomNav), R.id.nav_account);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        User user = prefs.getUser();
        if (!prefs.isLoggedIn() || user == null) {
            guestPanel.setVisibility(View.VISIBLE);
            userPanel.setVisibility(View.GONE);
            return;
        }
        guestPanel.setVisibility(View.GONE);
        userPanel.setVisibility(View.VISIBLE);

        String name = user.getFullName() == null ? "Usuario" : user.getFullName();
        tvGreeting.setText(name);
        tvEmail.setText(user.getEmail() == null ? "" : user.getEmail());
        tvPhone.setText(user.getPhone() == null || user.getPhone().isEmpty() ? "Sin teléfono" : user.getPhone());
        tvRole.setText(user.getRoleLabel());
        tvAvatar.setText(String.valueOf(Character.toUpperCase(user.getFirstName().charAt(0))));

        boolean owner = user.isOwner();
        int ownerVis = owner ? View.VISIBLE : View.GONE;
        btnListings.setVisibility(ownerVis);
        btnRequests.setVisibility(ownerVis);
        btnPublish.setVisibility(ownerVis);
        dividerOwner1.setVisibility(ownerVis);
        dividerOwner2.setVisibility(ownerVis);
        loadStats(owner);
    }

    private void loadStats(boolean owner) {
        ApiClient.getClient(this).create(ApiService.class).dashboard()
                .enqueue(new Callback<DashboardStats>() {
                    @Override
                    public void onResponse(Call<DashboardStats> call, Response<DashboardStats> response) {
                        statsRow.removeAllViews();
                        if (!response.isSuccessful() || response.body() == null) return;
                        DashboardStats stats = response.body();
                        if (owner) {
                            addStat("Equipos", String.valueOf(stats.getListings()));
                            addStat("Pendientes", String.valueOf(stats.getPending()));
                            addStat("Ingresos", MoneyFormat.bs(stats.getRevenue()));
                        } else {
                            addStat("Reservas", String.valueOf(stats.getBookings()));
                            addStat("Favoritos", String.valueOf(stats.getFavorites()));
                        }
                    }

                    @Override
                    public void onFailure(Call<DashboardStats> call, Throwable t) {
                        statsRow.removeAllViews();
                    }
                });
    }

    private void addStat(String label, String value) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_stat, statsRow, false);
        ((TextView) view.findViewById(R.id.tvStatLabel)).setText(label);
        ((TextView) view.findViewById(R.id.tvStatValue)).setText(value);
        statsRow.addView(view);
    }
}
