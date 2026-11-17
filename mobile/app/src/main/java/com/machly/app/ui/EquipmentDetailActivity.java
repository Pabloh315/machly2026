package com.machly.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.machly.app.R;
import com.machly.app.api.ApiClient;
import com.machly.app.api.ApiService;
import com.machly.app.models.Equipment;
import com.machly.app.models.FavoriteResponse;
import com.machly.app.models.User;
import com.machly.app.storage.PrefsManager;
import com.machly.app.util.MoneyFormat;
import com.machly.app.util.SvgImageLoader;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EquipmentDetailActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "equipment_id";

    private PrefsManager prefs;
    private Equipment equipment;
    private ImageView imgEquipment;
    private TextView tvEyebrow, tvName, tvLocation, tvDescription, tvOwner, tvRating, tvPrice, tvDeposit;
    private ChipGroup chipFeatures;
    private ImageButton btnFavorite;
    private MaterialButton btnBook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment_detail);
        prefs = new PrefsManager(this);

        imgEquipment = findViewById(R.id.imgEquipment);
        tvEyebrow = findViewById(R.id.tvEyebrow);
        tvName = findViewById(R.id.tvName);
        tvLocation = findViewById(R.id.tvLocation);
        tvDescription = findViewById(R.id.tvDescription);
        tvOwner = findViewById(R.id.tvOwner);
        tvRating = findViewById(R.id.tvRating);
        tvPrice = findViewById(R.id.tvPrice);
        tvDeposit = findViewById(R.id.tvDeposit);
        chipFeatures = findViewById(R.id.chipFeatures);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnBook = findViewById(R.id.btnBook);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnBook.setOnClickListener(v -> {
            if (!prefs.isLoggedIn()) {
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }
            if (equipment == null) return;
            Intent intent = new Intent(this, BookingActivity.class);
            intent.putExtra(BookingActivity.EXTRA_EQUIPMENT_ID, equipment.getId());
            intent.putExtra(BookingActivity.EXTRA_EQUIPMENT_NAME, equipment.getDisplayName());
            intent.putExtra(BookingActivity.EXTRA_PRICE_DAY, equipment.getPriceDay());
            intent.putExtra(BookingActivity.EXTRA_DEPOSIT, equipment.getDeposit());
            intent.putExtra(BookingActivity.EXTRA_IMAGE, equipment.getImage());
            startActivity(intent);
        });

        int id = getIntent().getIntExtra(EXTRA_ID, -1);
        if (id <= 0) {
            finish();
            return;
        }
        load(id);
    }

    private void load(int id) {
        ApiClient.getClient(this).create(ApiService.class).getEquipment(id)
                .enqueue(new Callback<Equipment>() {
                    @Override
                    public void onResponse(Call<Equipment> call, Response<Equipment> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(EquipmentDetailActivity.this, "No se pudo cargar el equipo", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        bind(response.body());
                    }

                    @Override
                    public void onFailure(Call<Equipment> call, Throwable t) {
                        Toast.makeText(EquipmentDetailActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void bind(Equipment item) {
        this.equipment = item;
        SvgImageLoader.load(this, imgEquipment, item.getImage());
        String typeLabel = "vehicle".equals(item.getType()) ? "Vehículo" : "Maquinaria";
        tvEyebrow.setText(typeLabel + " · " + (item.getCategory() == null ? "" : item.getCategory()));
        tvName.setText(item.getDisplayName());
        tvLocation.setText("⌖ " + item.getLocationLabel() + " · " + item.getYear());
        tvDescription.setText(item.getDescription());
        tvOwner.setText(item.getOwnerName() == null ? "Machly" : item.getOwnerName());
        tvRating.setText(item.getRating() > 0
                ? String.format("★ %.1f (%d)", item.getRating(), item.getReviewsCount())
                : "★ Nuevo");
        tvPrice.setText(MoneyFormat.bs(item.getPriceDay()));
        tvDeposit.setText(item.getDeposit() > 0 ? "Garantía: " + MoneyFormat.bs(item.getDeposit()) : "");
        chipFeatures.removeAllViews();
        for (String feature : item.getFeatures()) {
            Chip chip = new Chip(this);
            chip.setText("✓ " + feature);
            chip.setClickable(false);
            chipFeatures.addView(chip);
        }
        updateFavoriteIcon();
        User user = prefs.getUser();
        boolean own = user != null && user.getId() == item.getOwnerId();
        btnBook.setEnabled(item.isAvailable() && !own);
        btnBook.setText(own ? "Tu equipo" : (item.isAvailable() ? "Reservar" : "No disponible"));
    }

    private void updateFavoriteIcon() {
        if (equipment == null) return;
        btnFavorite.setColorFilter(getResources().getColor(
                equipment.isFavorite() ? R.color.warm : R.color.forest));
    }

    private void toggleFavorite() {
        if (!prefs.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        if (equipment == null) return;
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        if (equipment.isFavorite()) {
            api.removeFavorite(equipment.getId()).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        equipment.setFavorite(false);
                        updateFavoriteIcon();
                    }
                }
                @Override public void onFailure(Call<Void> call, Throwable t) { }
            });
        } else {
            api.addFavorite(equipment.getId()).enqueue(new Callback<FavoriteResponse>() {
                @Override public void onResponse(Call<FavoriteResponse> call, Response<FavoriteResponse> response) {
                    if (response.isSuccessful()) {
                        equipment.setFavorite(true);
                        updateFavoriteIcon();
                    }
                }
                @Override public void onFailure(Call<FavoriteResponse> call, Throwable t) { }
            });
        }
    }
}
