package com.insurance.gw.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyUtils {

    private MoneyUtils() {
    }

    /** "$1,234.50" / "-$12.00" / "1234.5" -> BigDecimal with scale 2. */
    public static BigDecimal parse(String text) {
        String cleaned = text.replace("$", "").replace(",", "").trim();
        return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
    }

    public static String format(BigDecimal amount) {
        String formatted = NumberFormat.getCurrencyInstance(Locale.US).format(amount.abs());
        return amount.signum() < 0 ? "-" + formatted : formatted;
    }
}
