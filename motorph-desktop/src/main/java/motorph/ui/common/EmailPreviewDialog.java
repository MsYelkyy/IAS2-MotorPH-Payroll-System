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

import java.util.Locale;

/** Renders a modal, demo-only preview of a system-generated email (no real email is sent). */
public final class EmailPreviewDialog {

    private EmailPreviewDialog() {
    }

    public static void show(Window owner, EmailPreviewDTO preview) {
        if (preview == null) {
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Demo email");
        dialog.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("email-preview-dialog-pane");
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.getStylesheets().add(stylesheet());
        pane.setContent(buildContent(dialog, preview));

        // the window's own close button needs a CLOSE type; the visible one is ours
        Node closeNode = pane.lookupButton(ButtonType.CLOSE);
        if (closeNode != null) {
            closeNode.setVisible(false);
            closeNode.setManaged(false);
        }

        dialog.setOnShown(event -> {
            Button closeButton = (Button) pane.lookup(".email-preview-close");
            if (closeButton != null) {
                closeButton.requestFocus();
            }
        });

        dialog.showAndWait();
    }

    private static VBox buildContent(Dialog<Void> dialog, EmailPreviewDTO preview) {
        VBox root = new VBox();
        root.getStyleClass().add("email-preview-root");
        root.getChildren().addAll(buildHeader(preview), buildBody(preview), buildFooter(dialog));
        return root;
    }

    private static VBox buildHeader(EmailPreviewDTO preview) {
        VBox header = new VBox();
        header.getStyleClass().add("email-preview-header");

        Label tag = new Label("FOR DEMO ONLY");
        tag.getStyleClass().add("email-preview-tag");

        Label title = new Label(typeLabel(preview.getType()));
        title.getStyleClass().add("email-preview-title");

        header.getChildren().addAll(tag, title);
        return header;
    }

    private static VBox buildBody(EmailPreviewDTO preview) {
        VBox body = new VBox();
        body.getStyleClass().add("email-preview-body");

        body.getChildren().add(metaRow("TO", preview.getTo()));
        body.getChildren().add(metaRow("SUBJECT", preview.getSubject()));

        if (preview.hasHighlight()) {
            body.getChildren().add(highlightRow(preview));
        }

        TextArea text = new TextArea(preview.getBody());
        text.setEditable(false);
        text.setWrapText(true);
        text.getStyleClass().add("email-preview-text");
        body.getChildren().add(text);

        return body;
    }

    private static HBox metaRow(String labelText, String valueText) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("email-preview-meta");

        Label label = new Label(labelText);
        label.getStyleClass().add("email-preview-meta-label");

        Label value = new Label(valueText);
        value.setWrapText(true);
        value.getStyleClass().add("email-preview-meta-value");
        HBox.setHgrow(value, Priority.ALWAYS);

        row.getChildren().addAll(label, value);
        return row;
    }

    // the temporary password, large and selectable, with a copy button
    private static HBox highlightRow(EmailPreviewDTO preview) {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("email-preview-highlight");

        Label label = new Label(preview.getHighlightLabel().toUpperCase(Locale.ROOT));
        label.getStyleClass().add("email-preview-highlight-label");

        TextField value = new TextField(preview.getHighlightValue());
        value.setEditable(false);
        value.getStyleClass().add("email-preview-highlight-value");

        VBox text = new VBox(label, value);
        text.getStyleClass().add("email-preview-highlight-text");
        HBox.setHgrow(text, Priority.ALWAYS);

        Button copy = new Button("Copy");
        copy.getStyleClass().add("email-preview-copy");
        copy.setMinWidth(Region.USE_PREF_SIZE);
        copy.setOnAction(event -> {
            copyToClipboard(preview.getHighlightValue());
            copy.setText("Copied");
        });

        box.getChildren().addAll(text, copy);
        return box;
    }

    private static HBox buildFooter(Dialog<Void> dialog) {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("email-preview-footer");

        Label note = new Label("For demo only. No real email was sent.");
        note.setWrapText(true);
        note.getStyleClass().add("email-preview-note");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button close = new Button("Close");
        close.getStyleClass().add("email-preview-close");
        close.setOnAction(event -> dialog.close());

        footer.getChildren().addAll(note, spacer, close);
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
