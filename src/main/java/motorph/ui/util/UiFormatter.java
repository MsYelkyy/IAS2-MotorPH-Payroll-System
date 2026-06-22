package motorph.ui.util;

import java.text.NumberFormat;
import java.util.Locale;

/** Provides formatting helpers for currency, payslip text, and null-safe values. */
public final class UiFormatter {

    private static final NumberFormat PESO_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("en", "PH"));


    // utility class — no instances
    private UiFormatter() {
    }

    // formats a numeric value as Philippine peso currency
    public static String formatPeso(double amount) {
        return PESO_FORMAT.format(amount);
    }

    // returns the value, or the fallback string when the value is null or blank
    public static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

}
