package com.machly.app.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.machly.app.R;
import com.machly.app.models.Booking;
import com.machly.app.util.MoneyFormat;
import com.machly.app.util.SvgImageLoader;

import java.util.ArrayList;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.Holder> {
    public interface ActionListener {
        void onAction(Booking booking, String status);
    }

    private final Context context;
    private final boolean ownerView;
    private final ActionListener listener;
    private final List<Booking> items = new ArrayList<>();

    public BookingAdapter(Context context, boolean ownerView, ActionListener listener) {
        this.context = context;
        this.ownerView = ownerView;
        this.listener = listener;
    }

    public void setItems(List<Booking> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Booking item = items.get(position);
        holder.tvStatus.setText(item.getStatusLabel());
        holder.tvName.setText(item.getEquipmentName());
        holder.tvDates.setText(item.getStartDate() + " — " + item.getEndDate() + " · " + item.getDays() + " días");
        holder.tvMeta.setText(ownerView
                ? ("Solicitante: " + (item.getRenterName() == null ? "-" : item.getRenterName()))
                : (item.getCity() == null ? "" : item.getCity()));
        holder.tvTotal.setText(MoneyFormat.bs(item.getTotal()));
        SvgImageLoader.load(context, holder.imgBooking, item.getImage());
        holder.actionsRow.removeAllViews();

        if (ownerView && "requested".equals(item.getStatus())) {
            addAction(holder, "Aceptar", "confirmed", true);
            addAction(holder, "Rechazar", "rejected", false);
        } else if (ownerView && "confirmed".equals(item.getStatus())) {
            addAction(holder, "Marcar entrega", "active", true);
        } else if (ownerView && "active".equals(item.getStatus())) {
            addAction(holder, "Completar", "completed", true);
        } else if (!ownerView && ("requested".equals(item.getStatus()) || "confirmed".equals(item.getStatus()))) {
            addAction(holder, "Cancelar", "cancelled", false);
        }
    }

    private void addAction(Holder holder, String label, String status, boolean primary) {
        MaterialButton button = new MaterialButton(context);
        button.setText(label);
        button.setTextSize(11f);
        button.setAllCaps(false);
        if (primary) {
            button.setBackgroundColor(context.getResources().getColor(R.color.forest));
            button.setTextColor(context.getResources().getColor(R.color.white));
        } else {
            button.setBackgroundColor(context.getResources().getColor(R.color.white));
            button.setTextColor(context.getResources().getColor(R.color.forest));
            button.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    context.getResources().getColor(R.color.forest)));
            button.setStrokeWidth(2);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(8, 0, 0, 0);
        button.setLayoutParams(params);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(28, 10, 28, 10);
        button.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (listener != null && pos >= 0) listener.onAction(items.get(pos), status);
        });
        holder.actionsRow.addView(button);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView imgBooking;
        final TextView tvStatus, tvName, tvDates, tvMeta, tvTotal;
        final LinearLayout actionsRow;

        Holder(@NonNull View itemView) {
            super(itemView);
            imgBooking = itemView.findViewById(R.id.imgBooking);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvName = itemView.findViewById(R.id.tvName);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            actionsRow = itemView.findViewById(R.id.actionsRow);
        }
    }
}
