package motorph.ui.admin;

import motorph.api.ApiClientFactory;
import motorph.api.DatabaseBackupApiClient;
import motorph.api.dto.system.BackupFileDTO;
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
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/** Manages the Database Backup &amp; Recovery sub-view. */
public class AdminDatabaseToolsController {

    private DatabaseBackupApiClient databaseBackupApiClient;

    // Toolbar
    @FXML private Button createBackupButton;
    @FXML private Label  backupCountLabel;
    @FXML private Label  moduleMessageLabel;

    // Table rows
    @FXML private VBox backupRows;

    @FXML
    private void initialize() {
        databaseBackupApiClient = ApiClientFactory.getInstance().getDatabaseBackupApiClient();
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

        Thread worker = new Thread(() -> {
            try {
                BackupFileDTO created = databaseBackupApiClient.createBackup();
                Platform.runLater(() -> {
                    UiMessageUtil.setSuccess(moduleMessageLabel, "Backup created: " + created.fileName());
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

    // Data loading
    private void loadBackups() {
        try {
            List<BackupFileDTO> entries = databaseBackupApiClient.listBackups();
            renderRows(entries);
            if (backupCountLabel != null) {
                int n = entries.size();
                backupCountLabel.setText(n + " backup" + (n == 1 ? "" : "s"));
            }
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    // Action handlers
    private void handleRestore(BackupFileDTO entry) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Restore Database");
        confirm.setHeaderText("Restore from: " + entry.fileName());
        confirm.setContentText("All current data will be replaced with the backup. This cannot be undone. Continue?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            setControlsDisabled(true);
            UiMessageUtil.clearMessage(moduleMessageLabel);

            Thread worker = new Thread(() -> {
                try {
                    databaseBackupApiClient.restoreBackup(entry.fileName());
                    Platform.runLater(() -> {
                        UiMessageUtil.setSuccess(moduleMessageLabel, "Database restored from: " + entry.fileName());
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

    private void handleDelete(BackupFileDTO entry) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Backup");
        confirm.setHeaderText("Delete: " + entry.fileName());
        confirm.setContentText("This backup file will be permanently deleted. Continue?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            try {
                databaseBackupApiClient.deleteBackup(entry.fileName());
                UiMessageUtil.setSuccess(moduleMessageLabel, "Deleted: " + entry.fileName());
                loadBackups();
            } catch (Exception e) {
                UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
            }
        });
    }

    // backups live on the server now, so getting a local copy is an explicit save
    private void handleDownload(BackupFileDTO entry) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Backup File");
        chooser.setInitialFileName(entry.fileName());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQL Files", "*.sql"));

        File target = chooser.showSaveDialog(
                backupRows.getScene() == null ? null : backupRows.getScene().getWindow());

        if (target == null) return;

        try {
            Files.write(target.toPath(), databaseBackupApiClient.downloadBackup(entry.fileName()));
            UiMessageUtil.setSuccess(moduleMessageLabel, "Saved to " + target.getName() + ".");
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    // Row rendering
    private void renderRows(List<BackupFileDTO> entries) {
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

    private HBox buildRow(BackupFileDTO entry) {
        HBox row = new HBox(0);
        row.getStyleClass().add("table-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        Label nameLbl = new Label(entry.fileName());
        nameLbl.getStyleClass().addAll("data-line", "table-cell");
        nameLbl.setMinWidth(320);
        HBox.setHgrow(nameLbl, Priority.ALWAYS);

        Label dateLbl = col(entry.getFormattedDate(), 185);
        Label sizeLbl = col(entry.getFormattedSize(), 90);

        Button downloadBtn = new Button("Download");
        downloadBtn.getStyleClass().add("secondary-button");
        downloadBtn.setOnAction(e -> handleDownload(entry));

        Button restoreBtn = new Button("Restore");
        restoreBtn.getStyleClass().add("secondary-button");
        restoreBtn.setOnAction(e -> handleRestore(entry));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("danger-outline-button");
        deleteBtn.setOnAction(e -> handleDelete(entry));

        HBox actions = new HBox(8, downloadBtn, restoreBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER);
        actions.setMinWidth(320);
        actions.setPrefWidth(320);

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
