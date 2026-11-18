package com.machly.app.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class BookingCalculator {
    private static final BigDecimal SERVICE_RATE = new BigDecimal("0.08");

    private BookingCalculator() { }

    public static long inclusiveDays(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("Las fechas de reserva no son válidas");
        }
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    public static Quote quote(LocalDate start, LocalDate end, BigDecimal dailyRate) {
        if (dailyRate == null || dailyRate.signum() < 0) throw new IllegalArgumentException("La tarifa no es válida");
        long days = inclusiveDays(start, end);
        BigDecimal subtotal = dailyRate.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal serviceFee = subtotal.multiply(SERVICE_RATE).setScale(2, RoundingMode.HALF_UP);
        return new Quote(days, subtotal, serviceFee, subtotal.add(serviceFee).setScale(2, RoundingMode.HALF_UP));
    }

    public static final class Quote {
        public final long days; public final BigDecimal subtotal; public final BigDecimal serviceFee; public final BigDecimal total;
        Quote(long days, BigDecimal subtotal, BigDecimal serviceFee, BigDecimal total) { this.days=days; this.subtotal=subtotal; this.serviceFee=serviceFee; this.total=total; }
    }
}
