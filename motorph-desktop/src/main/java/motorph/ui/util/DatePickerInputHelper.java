package motorph.ui.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

/**
 * Enables typed date entry on a DatePicker: the user types only digits and the
 * MM-dd-yyyy dashes are inserted automatically (e.g. 06252026 -> 06-25-2026).
 */
public final class DatePickerInputHelper {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    // utility class — no instances
    private DatePickerInputHelper() {
    }

    // wires the digit mask, auto-dash formatting, and MM-dd-yyyy converter onto the picker
    public static void enableMaskedInput(DatePicker picker) {
        if (picker == null) return;

        picker.setPromptText("MM-DD-YYYY");
        picker.setConverter(converter());

        TextField editor = picker.getEditor();
        installDigitMaskFormatter(editor);
        editor.textProperty().addListener((obs, oldText, newText) -> {
            String masked = applyDateMask(editor, newText);
            if (masked.length() == 10) {
                try { picker.setValue(LocalDate.parse(masked, DATE_FMT)); }
                catch (DateTimeParseException ignored) { picker.setValue(null); }
            } else {
                picker.setValue(null);
            }
        });
    }

    // allows only digits and dashes, capped at 8 digits (MMddyyyy)
    private static void installDigitMaskFormatter(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            if (!text.matches("[\\d-]*")) return null;
            int digits = 0;
            for (char c : text.toCharArray()) if (Character.isDigit(c)) digits++;
            return digits <= 8 ? change : null;
        }));
    }

    // inserts dashes after the month and day so digits render as MM-dd-yyyy
    private static String applyDateMask(TextField field, String newVal) {
        if (newVal == null) return "";
        String digits = newVal.replaceAll("[^\\d]", "");
        if (digits.length() > 8) digits = digits.substring(0, 8);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i == 2 || i == 4) sb.append('-');
            sb.append(digits.charAt(i));
        }
        String result = sb.toString();
        if (!result.equals(newVal)) { field.setText(result); field.positionCaret(result.length()); }
        return result;
    }

    // parses/formats the picker value using MM-dd-yyyy
    private static StringConverter<LocalDate> converter() {
        return new StringConverter<>() {
            @Override public String toString(LocalDate date) {
                return date == null ? "" : DATE_FMT.format(date);
            }
            @Override public LocalDate fromString(String text) {
                if (text == null || text.trim().length() != 10) return null;
                try { return LocalDate.parse(text.trim(), DATE_FMT); }
                catch (DateTimeParseException e) { return null; }
            }
        };
    }
}
