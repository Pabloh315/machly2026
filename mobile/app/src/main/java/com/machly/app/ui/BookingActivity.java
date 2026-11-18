package com.machly.app.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.machly.app.R;
import com.machly.app.api.ApiClient;
import com.machly.app.api.ApiService;
import com.machly.app.models.Booking;
import com.machly.app.models.BookingRequest;
import com.machly.app.util.ApiError;
import com.machly.app.util.MoneyFormat;
import com.machly.app.util.SvgImageLoader;

import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingActivity extends AppCompatActivity {
    public static final String EXTRA_EQUIPMENT_ID = "equipment_id";
    public static final String EXTRA_EQUIPMENT_NAME = "equipment_name";
    public static final String EXTRA_PRICE_DAY = "price_day";
    public static final String EXTRA_DEPOSIT = "deposit";
    public static final String EXTRA_IMAGE = "image";

    private int equipmentId;
    private double priceDay;
    private double deposit;
    private Calendar startDate;
    private Calendar endDate;

    private TextView tvStart, tvEnd, tvDays, tvSubtotal, tvFee, tvDeposit, tvTotal, tvError;
    private TextInputEditText etMessage;
    private MaterialButton btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        equipmentId = getIntent().getIntExtra(EXTRA_EQUIPMENT_ID, -1);
        priceDay = getIntent().getDoubleExtra(EXTRA_PRICE_DAY, 0);
        deposit = getIntent().getDoubleExtra(EXTRA_DEPOSIT, 0);
        String name = getIntent().getStringExtra(EXTRA_EQUIPMENT_NAME);
        String image = getIntent().getStringExtra(EXTRA_IMAGE);

        ImageView img = findViewById(R.id.imgEquipment);
        TextView tvName = findViewById(R.id.tvName);
        TextView tvPriceDay = findViewById(R.id.tvPriceDay);
        tvStart = findViewById(R.id.tvStart);
        tvEnd = findViewById(R.id.tvEnd);
        tvDays = findViewById(R.id.tvDays);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvFee = findViewById(R.id.tvFee);
        tvDeposit = findViewById(R.id.tvDeposit);
        tvTotal = findViewById(R.id.tvTotal);
        tvError = findViewById(R.id.tvError);
        etMessage = findViewById(R.id.etMessage);
        btnConfirm = findViewById(R.id.btnConfirm);

        tvName.setText(name);
        tvPriceDay.setText(MoneyFormat.bs(priceDay) + " / día");
        SvgImageLoader.load(this, img, image);

        startDate = todayCalendar();
        endDate = todayCalendar();
        endDate.add(Calendar.DAY_OF_MONTH, 2);
        bindDates();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPickStart).setOnClickListener(v -> pickDate(true));
        findViewById(R.id.btnPickEnd).setOnClickListener(v -> pickDate(false));
        btnConfirm.setOnClickListener(v -> submit());
    }

    private void pickDate(boolean start) {
        Calendar base = start ? startDate : endDate;
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth, 0, 0, 0);
                    picked.set(Calendar.MILLISECOND, 0);
                    if (start) {
                        startDate = picked;
                        if (endDate.before(startDate)) {
                            endDate = (Calendar) startDate.clone();
                        }
                    } else {
                        endDate = picked;
                        if (endDate.before(startDate)) {
                            startDate = (Calendar) endDate.clone();
                        }
                    }
                    bindDates();
                },
                base.get(Calendar.YEAR),
                base.get(Calendar.MONTH),
                base.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMinDate(todayCalendar().getTimeInMillis());
        dialog.show();
    }

    private void bindDates() {
        tvStart.setText(formatIso(startDate));
        tvEnd.setText(formatIso(endDate));
        updateSummary();
    }

    private void updateSummary() {
        int days = daysInclusive(startDate, endDate);
        if (days <= 0) {
            tvDays.setText("0");
            tvSubtotal.setText(MoneyFormat.bs(0));
            tvFee.setText(MoneyFormat.bs(0));
            tvDeposit.setText(MoneyFormat.bs(deposit));
            tvTotal.setText(MoneyFormat.bs(deposit));
            return;
        }
        double subtotal = priceDay * days;
        double fee = Math.round(subtotal * 0.08 * 100.0) / 100.0;
        double total = subtotal + fee + deposit;
        tvDays.setText(String.valueOf(days));
        tvSubtotal.setText(MoneyFormat.bs(subtotal));
        tvFee.setText(MoneyFormat.bs(fee));
        tvDeposit.setText(MoneyFormat.bs(deposit));
        tvTotal.setText(MoneyFormat.bs(total));
    }

    private void submit() {
        updateSummary();
        tvError.setVisibility(View.GONE);
        btnConfirm.setEnabled(false);
        String message = etMessage.getText() == null ? "" : etMessage.getText().toString().trim();
        ApiClient.getClient(this).create(ApiService.class)
                .createBooking(new BookingRequest(equipmentId, formatIso(startDate), formatIso(endDate), message))
                .enqueue(new Callback<Booking>() {
                    @Override
                    public void onResponse(Call<Booking> call, Response<Booking> response) {
                        btnConfirm.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(BookingActivity.this, "Reserva solicitada", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            showError(ApiError.message(response, "No se pudo crear la reserva."));
                        }
                    }

                    @Override
                    public void onFailure(Call<Booking> call, Throwable t) {
                        btnConfirm.setEnabled(true);
                        showError("No hay conexión con el servidor.");
                    }
                });
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private static Calendar todayCalendar() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private static String formatIso(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private static int daysInclusive(Calendar start, Calendar end) {
        long diff = (end.getTimeInMillis() - start.getTimeInMillis()) / 86400000L + 1;
        return (int) diff;
    }
}
