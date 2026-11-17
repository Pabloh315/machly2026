package com.machly.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.machly.app.R;
import com.machly.app.api.ApiClient;
import com.machly.app.api.ApiService;
import com.machly.app.models.Booking;
import com.machly.app.models.StatusUpdateRequest;
import com.machly.app.storage.PrefsManager;
import com.machly.app.ui.adapters.BookingAdapter;
import com.machly.app.util.ApiError;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingsActivity extends AppCompatActivity {
    private static final String EXTRA_OWNER = "owner_view";

    private boolean ownerView;
    private BookingAdapter adapter;
    private TextView tvEmpty;

    public static Intent intent(Context context, boolean ownerView) {
        Intent intent = new Intent(context, BookingsActivity.class);
        intent.putExtra(EXTRA_OWNER, ownerView);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings);
        ownerView = getIntent().getBooleanExtra(EXTRA_OWNER, false);
        PrefsManager prefs = new PrefsManager(this);
        if (!prefs.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(ownerView ? "Solicitudes recibidas" : "Mis reservas");
        tvEmpty = findViewById(R.id.tvEmpty);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView list = findViewById(R.id.rvBookings);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingAdapter(this, ownerView, this::updateStatus);
        list.setAdapter(adapter);
        load();
    }

    private void load() {
        ApiClient.getClient(this).create(ApiService.class)
                .getBookings(ownerView ? "owner" : null)
                .enqueue(new Callback<List<Booking>>() {
                    @Override
                    public void onResponse(Call<List<Booking>> call, Response<List<Booking>> response) {
                        List<Booking> items = response.body();
                        if (response.isSuccessful() && items != null) {
                            adapter.setItems(items);
                            tvEmpty.setText(ownerView
                                    ? "Aún no recibiste solicitudes."
                                    : "Aún no tienes reservas.");
                            tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Booking>> call, Throwable t) {
                        tvEmpty.setText("No se pudieron cargar las reservas.");
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void updateStatus(Booking booking, String status) {
        ApiClient.getClient(this).create(ApiService.class)
                .updateBookingStatus(booking.getId(), new StatusUpdateRequest(status))
                .enqueue(new Callback<Booking>() {
                    @Override
                    public void onResponse(Call<Booking> call, Response<Booking> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(BookingsActivity.this, "Reserva actualizada", Toast.LENGTH_SHORT).show();
                            load();
                        } else {
                            Toast.makeText(BookingsActivity.this,
                                    ApiError.message(response, "No se pudo actualizar"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Booking> call, Throwable t) {
                        Toast.makeText(BookingsActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
