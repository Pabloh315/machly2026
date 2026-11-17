package com.machly.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.machly.app.R;
import com.machly.app.api.ApiClient;
import com.machly.app.api.ApiService;
import com.machly.app.models.Equipment;
import com.machly.app.util.MoneyFormat;
import com.machly.app.util.SvgImageLoader;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListingsActivity extends AppCompatActivity {
    private final List<Equipment> items = new ArrayList<>();
    private Adapter adapter;
    private TextView tvEmpty;

    public static Intent intent(Context context) {
        return new Intent(context, ListingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings);
        ((TextView) findViewById(R.id.tvTitle)).setText("Mis equipos");
        tvEmpty = findViewById(R.id.tvEmpty);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        RecyclerView list = findViewById(R.id.rvBookings);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter();
        list.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        ApiClient.getClient(this).create(ApiService.class).myEquipment()
                .enqueue(new Callback<List<Equipment>>() {
                    @Override
                    public void onResponse(Call<List<Equipment>> call, Response<List<Equipment>> response) {
                        items.clear();
                        if (response.isSuccessful() && response.body() != null) items.addAll(response.body());
                        adapter.notifyDataSetChanged();
                        tvEmpty.setText("Publica tu primer equipo.");
                        tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onFailure(Call<List<Equipment>> call, Throwable t) {
                        Toast.makeText(ListingsActivity.this, "No se pudieron cargar tus equipos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Equipment item = items.get(position);
            holder.tvStatus.setText(item.getStatusLabel());
            holder.tvName.setText(item.getDisplayName());
            holder.tvDates.setText(item.getLocationLabel());
            holder.tvMeta.setText(MoneyFormat.bs(item.getPriceDay()) + " / día");
            holder.tvTotal.setText("");
            SvgImageLoader.load(ListingsActivity.this, holder.img, item.getImage());
            holder.actions.removeAllViews();
            MaterialButton edit = new MaterialButton(ListingsActivity.this);
            edit.setText("Editar");
            edit.setAllCaps(false);
            edit.setTextSize(11f);
            edit.setBackgroundColor(getResources().getColor(R.color.white));
            edit.setTextColor(getResources().getColor(R.color.forest));
            edit.setStrokeColor(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.forest)));
            edit.setStrokeWidth(2);
            edit.setOnClickListener(v -> {
                Intent intent = new Intent(ListingsActivity.this, PublishEquipmentActivity.class);
                intent.putExtra(PublishEquipmentActivity.EXTRA_EDIT_ID, item.getId());
                startActivity(intent);
            });
            holder.actions.addView(edit);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final ImageView img;
            final TextView tvStatus, tvName, tvDates, tvMeta, tvTotal;
            final ViewGroup actions;

            Holder(@NonNull View itemView) {
                super(itemView);
                img = itemView.findViewById(R.id.imgBooking);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                tvName = itemView.findViewById(R.id.tvName);
                tvDates = itemView.findViewById(R.id.tvDates);
                tvMeta = itemView.findViewById(R.id.tvMeta);
                tvTotal = itemView.findViewById(R.id.tvTotal);
                actions = itemView.findViewById(R.id.actionsRow);
            }
        }
    }
}
