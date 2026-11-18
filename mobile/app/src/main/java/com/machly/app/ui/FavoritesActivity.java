package com.machly.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.machly.app.R;
import com.machly.app.api.ApiClient;
import com.machly.app.api.ApiService;
import com.machly.app.models.Equipment;
import com.machly.app.storage.PrefsManager;
import com.machly.app.ui.adapters.EquipmentAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesActivity extends AppCompatActivity {
    private PrefsManager prefs;
    private EquipmentAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        prefs = new PrefsManager(this);
        tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView list = findViewById(R.id.rvFavorites);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EquipmentAdapter(this);
        list.setAdapter(adapter);
        BottomNavHelper.bind(this, findViewById(R.id.bottomNav), R.id.nav_favorites);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!prefs.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        load();
    }

    private void load() {
        ApiClient.getClient(this).create(ApiService.class).getFavorites()
                .enqueue(new Callback<List<Equipment>>() {
                    @Override
                    public void onResponse(Call<List<Equipment>> call, Response<List<Equipment>> response) {
                        List<Equipment> items = response.body();
                        if (response.isSuccessful() && items != null) {
                            adapter.setItems(items);
                            tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                        } else {
                            tvEmpty.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Equipment>> call, Throwable t) {
                        tvEmpty.setText("No se pudieron cargar los favoritos.");
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }
}
