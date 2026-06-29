package motorph.ui.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;

/** Provides text field input masks and formatters for UI controllers. */
public final class TextFieldFormatters {

    // utility class — no instances
    private TextFieldFormatters() {
    }

    // limits a field to a maximum number of characters
    public static void installMaxLength(TextField field, int maxLength) {
        field.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().length() <= maxLength ? change : null));
    }

    // limits a field to a money amount with capped integer and decimal digits
    public static void installAmount(TextField field, int maxInt, int maxDec) {
        field.setTextFormatter(new TextFormatter<>(amountFilter(maxInt, maxDec)));
    }

    // limits a field to digits and dashes with a maximum digit count
    public static void installDigitMask(TextField field, int maxDigits) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            return isValidDigitMask(text, maxDigits) ? change : null;
        }));
    }

    // keeps a phone field formatted as XXX-XXX-XXXX while typing
    public static void installPhone(TextField field) {
        field.textProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            String result = maskPhone(n);
            if (!result.equals(n)) {
                field.setText(result);
                field.positionCaret(result.length());
            }
        });
    }

    // applies a date mask to the field and returns the masked text
    public static String applyDateMask(TextField field, String newVal) {
        String result = maskDate(newVal);
        if (!result.equals(newVal == null ? "" : newVal)) {
            field.setText(result);
            field.positionCaret(result.length());
        }
        return result;
    }

    // applies a government id mask using the given dash positions
    public static void autoFormatGovId(TextField field, String newVal, int[] dashPositions, int maxLen) {
        if (newVal == null) return;
        String result = maskGovId(newVal, dashPositions, maxLen);
        if (!result.equals(newVal)) {
            field.setText(result);
            field.positionCaret(result.length());
        }
    }

    // checks that text contains only digits and dashes within the digit limit
    public static boolean isValidDigitMask(String text, int maxDigits) {
        if (!text.matches("[\\d-]*")) return false;
        int digits = 0;
        for (char c : text.toCharArray()) {
            if (Character.isDigit(c)) digits++;
        }
        return digits <= maxDigits;
    }

    // formats raw input as a phone number (XXX-XXX-XXXX)
    public static String maskPhone(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^\\d]", "");
        if (digits.length() > 10) digits = digits.substring(0, 10);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i == 3 || i == 6) sb.append('-');
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    // formats raw input as a date (XX-XX-XXXX)
    public static String maskDate(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^\\d]", "");
        if (digits.length() > 8) digits = digits.substring(0, 8);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i == 2 || i == 4) sb.append('-');
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    // formats raw input as a government id with dashes at the given positions
    public static String maskGovId(String raw, int[] dashPositions, int maxLen) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^\\d]", "");
        int maxDigits = maxLen - dashPositions.length;
        if (digits.length() > maxDigits) digits = digits.substring(0, maxDigits);

        StringBuilder sb = new StringBuilder();
        int d = 0;
        for (int pos = 0; pos < maxLen && d < digits.length(); pos++) {
            boolean isDash = false;
            for (int dp : dashPositions) {
                if (pos == dp) { isDash = true; break; }
            }
            if (isDash) sb.append('-');
            else sb.append(digits.charAt(d++));
        }
        return sb.toString();
    }

    // builds an amount filter capping integer and decimal digit counts
    private static UnaryOperator<TextFormatter.Change> amountFilter(int maxInt, int maxDec) {
        return change -> {
            String text = change.getControlNewText();
            if (text.isEmpty()) return change;
            if (!text.matches("\\d*(\\.\\d*)?")) return null;
            int dot = text.indexOf('.');
            String intPart = dot >= 0 ? text.substring(0, dot) : text;
            String decPart = dot >= 0 ? text.substring(dot + 1) : "";
            if (intPart.length() > maxInt || decPart.length() > maxDec) return null;
            return change;
        };
    }
}
