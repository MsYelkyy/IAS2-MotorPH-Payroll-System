package motorph.ui.common;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import motorph.application.dto.auth.EmailPreviewDTO;
import motorph.application.dto.auth.EmailPreviewType;

import java.time.format.DateTimeFormatter;

/** Renders a modal in-app preview for system-generated emails. */
public final class EmailPreviewDialog {

    private static final DateTimeFormatter SENT_AT_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    private EmailPreviewDialog() {
    }

    public static void show(Window owner, EmailPreviewDTO preview, boolean lightMode) {
        if (preview == null) {
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Email Sent");
        dialog.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("email-preview-dialog-pane");
        if (lightMode) {
            pane.getStyleClass().add("light-mode");
        }
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.getStylesheets().add(stylesheet());
        pane.setContent(buildContent(dialog, preview));

        Node closeNode = pane.lookupButton(ButtonType.CLOSE);
        if (closeNode != null) {
            closeNode.setVisible(false);
            closeNode.setManaged(false);
        }

        dialog.setOnShown(event -> {
            Button closeButton = (Button) pane.lookup(".email-preview-footer-close");
            if (closeButton != null) {
                closeButton.requestFocus();
            }
        });

        dialog.showAndWait();
    }

    private static VBox buildContent(Dialog<Void> dialog, EmailPreviewDTO preview) {
        VBox root = new VBox();
        root.getStyleClass().add("email-preview-root");

        VBox paperWrap = new VBox(buildPaper(preview));
        paperWrap.getStyleClass().add("email-preview-paper-wrap");

        root.getChildren().add(buildHeader(dialog, preview));
        root.getChildren().add(paperWrap);
        root.getChildren().add(buildFooter(dialog, preview));

        return root;
    }

    private static HBox buildHeader(Dialog<Void> dialog, EmailPreviewDTO preview) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("email-preview-header");

        VBox titleBlock = new VBox(2);
        Label brand = new Label("MotorPH");
        brand.getStyleClass().add("email-preview-brand");
        Label title = new Label("Email Sent");
        title.getStyleClass().add("email-preview-title");
        Label subtitle = new Label(typeLabel(preview.getType()));
        subtitle.getStyleClass().add("email-preview-subtitle");
        titleBlock.getChildren().addAll(brand, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button("X");
        close.getStyleClass().add("email-preview-close-x");
        close.setOnAction(event -> dialog.close());

        header.getChildren().addAll(titleBlock, spacer, close);
        return header;
    }

    private static VBox buildPaper(EmailPreviewDTO preview) {
        VBox paper = new VBox(14);
        paper.getStyleClass().add("email-preview-paper");

        paper.getChildren().add(metaRow("From", preview.getFrom()));
        paper.getChildren().add(metaRow("To", preview.getTo()));
        paper.getChildren().add(metaRow("Subject", preview.getSubject()));
        paper.getChildren().add(metaRow("Sent", SENT_AT_FORMAT.format(preview.getSentAt())));

        Region divider = new Region();
        divider.getStyleClass().add("email-preview-divider");
        paper.getChildren().add(divider);

        if (preview.hasHighlight()) {
            paper.getChildren().add(highlightRow(preview));
        }

        TextArea body = new TextArea(preview.getBody());
        body.setEditable(false);
        body.setWrapText(true);
        body.getStyleClass().add("email-preview-body");
        VBox.setVgrow(body, Priority.ALWAYS);
        paper.getChildren().add(body);

        return paper;
    }

    private static HBox metaRow(String labelText, String valueText) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("email-preview-meta-row");

        Label label = new Label(labelText);
        label.getStyleClass().add("email-preview-meta-label");

        Label value = new Label(valueText);
        value.setWrapText(true);
        value.getStyleClass().add("email-preview-meta-value");
        HBox.setHgrow(value, Priority.ALWAYS);

        row.getChildren().addAll(label, value);
        return row;
    }

    private static VBox highlightRow(EmailPreviewDTO preview) {
        VBox wrap = new VBox(7);
        wrap.getStyleClass().add("email-preview-highlight");

        Label label = new Label(preview.getHighlightLabel() + ":");
        label.getStyleClass().add("email-preview-highlight-label");

        TextField value = new TextField(preview.getHighlightValue());
        value.setEditable(false);
        value.getStyleClass().add("email-preview-highlight-value");

        wrap.getChildren().addAll(label, value);
        return wrap;
    }

    private static HBox buildFooter(Dialog<Void> dialog, EmailPreviewDTO preview) {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("email-preview-footer");

        if (preview.hasHighlight()) {
            Button copy = new Button("Copy " + preview.getHighlightLabel());
            copy.getStyleClass().add("email-preview-primary-button");
            copy.setOnAction(event -> copyToClipboard(preview.getHighlightValue()));
            footer.getChildren().add(copy);
        }

        Button close = new Button("Close");
        close.getStyleClass().addAll("email-preview-secondary-button", "email-preview-footer-close");
        close.setOnAction(event -> dialog.close());
        footer.getChildren().add(close);

        return footer;
    }

    private static void copyToClipboard(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private static String typeLabel(EmailPreviewType type) {
        if (type == null) {
            return "System email preview";
        }

        return switch (type) {
            case LOGIN_OTP -> "Login verification email";
            case PASSWORD_RESET_OTP -> "Password reset verification email";
            case TEMPORARY_PASSWORD -> "Temporary password email";
            case EMPLOYEE_ACCOUNT_CREDENTIALS -> "Employee account credentials";
        };
    }

    private static String stylesheet() {
        return EmailPreviewDialog.class
                .getResource("/css/modules/shared/email-preview-dialog.css")
                .toExternalForm();
    }
}
