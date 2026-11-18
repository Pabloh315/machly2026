package com.machly.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.machly.app.R;
import com.machly.app.api.ApiClient;
import com.machly.app.api.ApiService;
import com.machly.app.models.Equipment;
import com.machly.app.models.EquipmentRequest;
import com.machly.app.util.ApiError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PublishEquipmentActivity extends AppCompatActivity {
    public static final String EXTRA_EDIT_ID = "edit_id";

    private int editId = -1;
    private Spinner spType;
    private TextInputEditText etCategory, etName, etBrand, etModel, etYear, etPrice, etDeposit, etCity, etProvince, etDescription, etFeatures;
    private TextView tvError;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish);
        editId = getIntent().getIntExtra(EXTRA_EDIT_ID, -1);

        spType = findViewById(R.id.spType);
        etCategory = findViewById(R.id.etCategory);
        etName = findViewById(R.id.etName);
        etBrand = findViewById(R.id.etBrand);
        etModel = findViewById(R.id.etModel);
        etYear = findViewById(R.id.etYear);
        etPrice = findViewById(R.id.etPrice);
        etDeposit = findViewById(R.id.etDeposit);
        etCity = findViewById(R.id.etCity);
        etProvince = findViewById(R.id.etProvince);
        etDescription = findViewById(R.id.etDescription);
        etFeatures = findViewById(R.id.etFeatures);
        tvError = findViewById(R.id.tvError);
        btnSave = findViewById(R.id.btnSave);

        spType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Maquinaria", "Vehículo")));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> save());
        etYear.setText(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));

        if (editId > 0) {
            ((TextView) findViewById(R.id.tvTitle)).setText("Editar equipo");
            btnSave.setText("Guardar cambios");
            loadExisting();
        }
    }

    private void loadExisting() {
        ApiClient.getClient(this).create(ApiService.class).getEquipment(editId)
                .enqueue(new Callback<Equipment>() {
                    @Override
                    public void onResponse(Call<Equipment> call, Response<Equipment> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        Equipment item = response.body();
                        spType.setSelection("vehicle".equals(item.getType()) ? 1 : 0);
                        etCategory.setText(item.getCategory());
                        etName.setText(item.getName());
                        etBrand.setText(item.getBrand());
                        etModel.setText(item.getModel());
                        etYear.setText(String.valueOf(item.getYear()));
                        etPrice.setText(String.valueOf(item.getPriceDay()));
                        etDeposit.setText(String.valueOf(item.getDeposit()));
                        etCity.setText(item.getCity());
                        etProvince.setText(item.getProvince());
                        etDescription.setText(item.getDescription());
                        etFeatures.setText(android.text.TextUtils.join(", ", item.getFeatures()));
                    }

                    @Override
                    public void onFailure(Call<Equipment> call, Throwable t) { }
                });
    }

    private void save() {
        tvError.setVisibility(View.GONE);
        EquipmentRequest req = new EquipmentRequest();
        req.type = spType.getSelectedItemPosition() == 1 ? "vehicle" : "machinery";
        req.category = text(etCategory);
        req.name = text(etName);
        req.brand = text(etBrand);
        req.model = text(etModel);
        req.city = text(etCity);
        req.province = text(etProvince);
        req.description = text(etDescription);
        req.image = "/assets/equipment/default.svg";
        req.status = "available";
        try {
            req.year = Integer.parseInt(text(etYear));
            req.priceDay = Double.parseDouble(text(etPrice));
            req.deposit = text(etDeposit).isEmpty() ? 0 : Double.parseDouble(text(etDeposit));
        } catch (Exception e) {
            showError("Revisa año, precio y garantía.");
            return;
        }
        List<String> features = new ArrayList<>();
        for (String part : text(etFeatures).split(",")) {
            String clean = part.trim();
            if (!clean.isEmpty()) features.add(clean);
        }
        req.features = features;

        btnSave.setEnabled(false);
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        Call<Equipment> call = editId > 0 ? api.updateEquipment(editId, req) : api.createEquipment(req);
        call.enqueue(new Callback<Equipment>() {
            @Override
            public void onResponse(Call<Equipment> call, Response<Equipment> response) {
                btnSave.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(PublishEquipmentActivity.this,
                            editId > 0 ? "Equipo actualizado" : "Equipo publicado",
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    showError(ApiError.message(response, "No se pudo guardar el equipo."));
                }
            }

            @Override
            public void onFailure(Call<Equipment> call, Throwable t) {
                btnSave.setEnabled(true);
                showError("No hay conexión con el servidor.");
            }
        });
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private String text(TextInputEditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString().trim();
    }
}
