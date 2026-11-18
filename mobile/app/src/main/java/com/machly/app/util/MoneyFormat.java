package com.machly.app.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyFormat {
    private static final NumberFormat FORMAT = NumberFormat.getNumberInstance(new Locale("es", "BO"));

    static {
        FORMAT.setMaximumFractionDigits(0);
        FORMAT.setMinimumFractionDigits(0);
    }

    private MoneyFormat() {}

    public static String bs(double amount) {
        return "Bs " + FORMAT.format(Math.round(amount));
    }
}
