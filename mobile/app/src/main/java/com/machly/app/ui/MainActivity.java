package com.machly.app.ui;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.machly.app.R;
import com.machly.app.api.ApiClient;
import com.machly.app.api.ApiService;
import com.machly.app.models.EquipmentPage;
import com.machly.app.ui.adapters.EquipmentAdapter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private EquipmentAdapter adapter;
    private SwipeRefreshLayout refresh;
    private TextView tvEmpty, tvConnection, tvResults;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etSearch = findViewById(R.id.etSearch);
        MaterialButton btnSearch = findViewById(R.id.btnSearch);
        TextView btnClear = findViewById(R.id.btnClear);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvConnection = findViewById(R.id.tvConnection);
        tvResults = findViewById(R.id.tvResults);
        refresh = findViewById(R.id.swipeRefresh);
        RecyclerView list = findViewById(R.id.rvEquipment);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EquipmentAdapter(this);
        list.setAdapter(adapter);

        BottomNavHelper.bind(this, findViewById(R.id.bottomNav), R.id.nav_home);

        btnSearch.setOnClickListener(v -> loadEquipment(etSearch.getText().toString().trim()));
        btnClear.setOnClickListener(v -> {
            etSearch.setText("");
            loadEquipment("");
        });
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                loadEquipment(etSearch.getText().toString().trim());
                return true;
            }
            return false;
        });
        refresh.setOnRefreshListener(() -> loadEquipment(etSearch.getText().toString().trim()));
        refresh.setColorSchemeResources(R.color.forest, R.color.leaf);

        loadEquipment("");
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        BottomNavHelper.bind(this, findViewById(R.id.bottomNav), R.id.nav_home);
    }

    private void loadEquipment(String search) {
        refresh.setRefreshing(true);
        tvConnection.setVisibility(View.GONE);
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getEquipment(search.isEmpty() ? null : search, null, null, 1, 50)
                .enqueue(new Callback<EquipmentPage>() {
                    @Override
                    public void onResponse(Call<EquipmentPage> call, Response<EquipmentPage> response) {
                        refresh.setRefreshing(false);
                        EquipmentPage page = response.body();
                        if (response.isSuccessful() && page != null) {
                            adapter.setItems(page.getItems());
                            tvResults.setText(getString(R.string.equipments_found, page.getTotal()));
                            boolean empty = page.getItems().isEmpty();
                            tvEmpty.setText(empty ? "No encontramos equipos con esa búsqueda." : "");
                            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                        } else {
                            showError("El servidor respondió con error (" + response.code() + ").");
                        }
                    }

                    @Override
                    public void onFailure(Call<EquipmentPage> call, Throwable error) {
                        refresh.setRefreshing(false);
                        showError("No se pudo conectar con la API. Comprueba que el servidor esté encendido.");
                    }
                });
    }

    private void showError(String message) {
        adapter.setItems(java.util.Collections.emptyList());
        tvEmpty.setVisibility(View.GONE);
        tvResults.setText(getString(R.string.equipments_found, 0));
        tvConnection.setText(message);
        tvConnection.setVisibility(View.VISIBLE);
    }
}
