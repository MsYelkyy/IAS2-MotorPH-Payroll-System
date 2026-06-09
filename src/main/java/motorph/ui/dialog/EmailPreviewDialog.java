package motorph.ui.dialog;

import javafx.scene.control.Alert;
import javafx.scene.web.WebView;
import motorph.application.dto.auth.EmailPreviewDTO;

import java.time.format.DateTimeFormatter;

/**
 * Displays the latest QA email preview in a popup window.
 */
public class EmailPreviewDialog {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a");

    // shows email preview popup
    public static void show(EmailPreviewDTO emailPreview) {
        if (emailPreview == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("MotorPH Email Preview");
        alert.setHeaderText(emailPreview.getSubject());

        WebView webView = new WebView();
        webView.setPrefWidth(560);
        webView.setPrefHeight(360);
        webView.getEngine().loadContent(buildEmailHtml(emailPreview));

        alert.getDialogPane().setContent(webView);
        alert.showAndWait();
    }

    // builds email preview HTML
    private static String buildEmailHtml(EmailPreviewDTO emailPreview) {
        return """
                <html>
                    <body style="font-family: Segoe UI, Arial, sans-serif; font-size: 14px; color: #1f2937;">
                        <p><b>From:</b> %s</p>
                        <p><b>To:</b> %s</p>
                        <p><b>Date:</b> %s</p>
                        <hr>
                        <p><b>Subject:</b> %s</p>
                        %s
                    </body>
                </html>
                """.formatted(
                escape(emailPreview.getFrom()),
                escape(emailPreview.getTo()),
                escape(emailPreview.getSentAt().format(FORMATTER)),
                escape(emailPreview.getSubject()),
                formatBody(emailPreview.getBody())
        );
    }

    // formats email body and highlights OTP
    private static String formatBody(String body) {
        String escapedBody = escape(body);

        return escapedBody
                .replaceAll(
                        "(verification code is: )(\\d{6})",
                        "$1<b style=\"font-size: 20px; color: #004b75;\">$2</b>"
                )
                .replace("\n", "<br>");
    }

    // escapes html-sensitive characters
    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}