package com.machly.app.ui.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.machly.app.R;
import com.machly.app.api.ApiClient;
import com.machly.app.api.ApiService;
import com.machly.app.models.Equipment;
import com.machly.app.models.FavoriteResponse;
import com.machly.app.storage.PrefsManager;
import com.machly.app.ui.EquipmentDetailActivity;
import com.machly.app.ui.LoginActivity;
import com.machly.app.util.MoneyFormat;
import com.machly.app.util.SvgImageLoader;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EquipmentAdapter extends RecyclerView.Adapter<EquipmentAdapter.Holder> {
    private final Context context;
    private final PrefsManager prefs;
    private final List<Equipment> items = new ArrayList<>();

    public EquipmentAdapter(Context context) {
        this.context = context;
        this.prefs = new PrefsManager(context);
    }

    public void setItems(List<Equipment> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equipment, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Equipment item = items.get(position);
        holder.tvCategory.setText(item.getCategory() == null ? "" : item.getCategory().toUpperCase());
        holder.tvName.setText(item.getDisplayName());
        holder.tvLocation.setText(item.getLocationLabel());
        holder.tvPrice.setText(MoneyFormat.bs(item.getPriceDay()) + " / día");
        holder.tvStatus.setText(item.getStatusLabel());
        if (item.getRating() > 0) {
            holder.tvRating.setText(String.format("★ %.1f", item.getRating()));
        } else {
            holder.tvRating.setText("★ NUEVO");
        }
        holder.btnFavorite.setAlpha(item.isFavorite() ? 1f : 0.55f);
        holder.btnFavorite.setColorFilter(item.isFavorite()
                ? context.getResources().getColor(R.color.warm)
                : context.getResources().getColor(R.color.forest));

        holder.imgEquipment.setClickable(false);
        holder.imgEquipment.setFocusable(false);
        SvgImageLoader.load(context, holder.imgEquipment, item.getImage());

        View.OnClickListener openDetail = v -> openDetail(item);
        holder.itemView.setOnClickListener(openDetail);
        holder.btnOpen.setOnClickListener(openDetail);
        holder.btnFavorite.setOnClickListener(v -> toggleFavorite(item, holder.getBindingAdapterPosition()));
    }

    private void openDetail(Equipment item) {
        Context ctx = context;
        Intent intent = new Intent(ctx, EquipmentDetailActivity.class);
        intent.putExtra(EquipmentDetailActivity.EXTRA_ID, item.getId());
        if (!(ctx instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        ctx.startActivity(intent);
    }

    private void toggleFavorite(Equipment item, int position) {
        if (!prefs.isLoggedIn()) {
            context.startActivity(new Intent(context, LoginActivity.class));
            return;
        }
        ApiService api = ApiClient.getClient(context).create(ApiService.class);
        if (item.isFavorite()) {
            api.removeFavorite(item.getId()).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        item.setFavorite(false);
                        if (position >= 0) notifyItemChanged(position);
                    }
                }
                @Override public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(context, "No se pudo actualizar el favorito", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            api.addFavorite(item.getId()).enqueue(new Callback<FavoriteResponse>() {
                @Override public void onResponse(Call<FavoriteResponse> call, Response<FavoriteResponse> response) {
                    if (response.isSuccessful()) {
                        item.setFavorite(true);
                        if (position >= 0) notifyItemChanged(position);
                    }
                }
                @Override public void onFailure(Call<FavoriteResponse> call, Throwable t) {
                    Toast.makeText(context, "No se pudo guardar el favorito", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView imgEquipment;
        final TextView tvStatus, tvCategory, tvRating, tvName, tvLocation, tvPrice;
        final ImageButton btnFavorite, btnOpen;

        Holder(@NonNull View itemView) {
            super(itemView);
            imgEquipment = itemView.findViewById(R.id.imgEquipment);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvName = itemView.findViewById(R.id.tvName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            btnOpen = itemView.findViewById(R.id.btnOpen);
        }
    }
}
