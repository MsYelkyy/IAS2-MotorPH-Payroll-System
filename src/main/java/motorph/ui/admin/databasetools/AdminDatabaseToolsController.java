package motorph.ui.admin.databasetools;

import motorph.application.service.DatabaseBackupService;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiMessageUtil;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Manages the Database Backup &amp; Recovery sub-view. */
public class AdminDatabaseToolsController {

    private DatabaseBackupService databaseBackupService;

    // ── Toolbar ───────────────────────────────────────────────────────────────
    @FXML private Button createBackupButton;
    @FXML private Label  backupCountLabel;
    @FXML private Label  moduleMessageLabel;

    // ── Table rows ────────────────────────────────────────────────────────────
    @FXML private VBox backupRows;

    @FXML
    private void initialize() {
        databaseBackupService = EmployeeUIServiceFactory.getInstance().getDatabaseBackupService();
        loadBackups();
    }

    /** Called by AdminMainController each time the database tools nav is activated. */
    public void refresh() {
        loadBackups();
        UiMessageUtil.clearMessage(moduleMessageLabel);
    }

    @FXML
    private void handleCreateBackup() {
        setControlsDisabled(true);
        UiMessageUtil.clearMessage(moduleMessageLabel);

        int userId = CurrentUserSession.getUserAccountId();
        Thread worker = new Thread(() -> {
            try {
                var created = databaseBackupService.createBackup(userId);
                Platform.runLater(() -> {
                    UiMessageUtil.setSuccess(moduleMessageLabel, "Backup created: " + created.getFileName());
                    loadBackups();
                    setControlsDisabled(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    UiMessageUtil.setError(moduleMessageLabel, "Backup failed: " + UiMessageUtil.toUserMessage(e));
                    setControlsDisabled(false);
                });
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleRefresh() {
        loadBackups();
        UiMessageUtil.clearMessage(moduleMessageLabel);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadBackups() {
        try {
            List<DatabaseBackupService.BackupEntry> entries = databaseBackupService.listBackups();
            renderRows(entries);
            if (backupCountLabel != null) {
                int n = entries.size();
                backupCountLabel.setText(n + " backup" + (n == 1 ? "" : "s"));
            }
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    private void handleRestore(DatabaseBackupService.BackupEntry entry) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restore Database");
        confirm.setHeaderText("Restore from: " + entry.getFileName());
        confirm.setContentText("All current data will be replaced with the backup. This cannot be undone. Continue?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            setControlsDisabled(true);
            UiMessageUtil.clearMessage(moduleMessageLabel);

            int userId = CurrentUserSession.getUserAccountId();
            Thread worker = new Thread(() -> {
                try {
                    databaseBackupService.restoreBackup(entry.path, userId);
                    Platform.runLater(() -> {
                        UiMessageUtil.setSuccess(moduleMessageLabel, "Database restored from: " + entry.getFileName());
                        setControlsDisabled(false);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        UiMessageUtil.setError(moduleMessageLabel, "Restore failed: " + UiMessageUtil.toUserMessage(e));
                        setControlsDisabled(false);
                    });
                }
            });
            worker.setDaemon(true);
            worker.start();
        });
    }

    private void handleDelete(DatabaseBackupService.BackupEntry entry) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Backup");
        confirm.setHeaderText("Delete: " + entry.getFileName());
        confirm.setContentText("This backup file will be permanently deleted. Continue?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            try {
                databaseBackupService.deleteBackup(entry.path);
                UiMessageUtil.setSuccess(moduleMessageLabel, "Deleted: " + entry.getFileName());
                loadBackups();
            } catch (Exception e) {
                UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
            }
        });
    }

    // ── Row rendering ─────────────────────────────────────────────────────────

    private void renderRows(List<DatabaseBackupService.BackupEntry> entries) {
        backupRows.getChildren().clear();
        if (entries.isEmpty()) {
            Label empty = new Label("No backups found. Click \"Create Backup\" to generate one.");
            empty.getStyleClass().add("muted-text");
            empty.setStyle("-fx-padding: 32 0 0 0;");
            backupRows.getChildren().add(empty);
            return;
        }
        entries.forEach(e -> backupRows.getChildren().add(buildRow(e)));
    }

    private HBox buildRow(DatabaseBackupService.BackupEntry entry) {
        HBox row = new HBox(0);
        row.getStyleClass().add("table-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        Label nameLbl = new Label(entry.getFileName());
        nameLbl.getStyleClass().addAll("data-line", "table-cell");
        nameLbl.setMinWidth(320);
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        Label dateLbl = col(entry.getFormattedDate(), 185);
        Label sizeLbl = col(entry.getFormattedSize(), 90);

        Button restoreBtn = new Button("Restore");
        restoreBtn.getStyleClass().addAll("secondary-button", "table-cell");
        restoreBtn.setOnAction(e -> handleRestore(entry));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().addAll("danger-outline-button", "table-cell");
        deleteBtn.setOnAction(e -> handleDelete(entry));

        HBox actions = new HBox(8, restoreBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("table-cell");
        actions.setMinWidth(170);
        actions.setPrefWidth(170);

        row.getChildren().addAll(nameLbl, dateLbl, sizeLbl, actions);
        return row;
    }

    private Label col(String text, double width) {
        Label lbl = new Label(text);
        lbl.getStyleClass().addAll("data-line", "table-cell");
        lbl.setMinWidth(width);
        lbl.setPrefWidth(width);
        return lbl;
    }

    private void setControlsDisabled(boolean disabled) {
        if (createBackupButton != null) createBackupButton.setDisable(disabled);
    }
}
