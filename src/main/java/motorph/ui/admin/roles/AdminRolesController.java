package motorph.ui.admin.roles;

import motorph.application.service.PermissionService;
import motorph.domain.model.Permission;
import motorph.domain.value.PermissionKey;
import motorph.domain.value.Role;
import motorph.ui.factory.EmployeeUIServiceFactory;
import motorph.ui.util.UiMessageUtil;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Manages the Roles & Permissions sub-view: role selector, permission matrix, and save. */
public class AdminRolesController {

    private PermissionService permissionService;

    // ── State ─────────────────────────────────────────────────────────────────
    private Role selectedRole;
    private List<Permission> allPermissions = new ArrayList<>();
    private final Map<PermissionKey, CheckBox> permissionChecks = new LinkedHashMap<>();

    // ── Role selector buttons ─────────────────────────────────────────────────
    @FXML private Button roleAdminBtn;
    @FXML private Button roleHrBtn;
    @FXML private Button roleFinanceBtn;
    @FXML private Button roleItBtn;
    @FXML private Button roleEmployeeBtn;

    // ── Permission panel ──────────────────────────────────────────────────────
    @FXML private Label  selectedRoleLabel;
    @FXML private VBox   permissionMatrix;
    @FXML private Label  moduleMessageLabel;

    @FXML
    private void initialize() {
        permissionService = EmployeeUIServiceFactory.getInstance().getPermissionService();
        loadAllPermissions();
        selectRole(Role.ADMIN, roleAdminBtn);
    }

    /** Called by AdminMainController each time role management nav is activated. */
    public void refresh() {
        loadAllPermissions();
        if (selectedRole != null) {
            renderMatrix(selectedRole);
        }
        UiMessageUtil.clearMessage(moduleMessageLabel);
    }

    // ── Role selection ────────────────────────────────────────────────────────

    @FXML private void selectAdmin()    { selectRole(Role.ADMIN,    roleAdminBtn); }
    @FXML private void selectHr()       { selectRole(Role.HR,       roleHrBtn); }
    @FXML private void selectFinance()  { selectRole(Role.FINANCE,  roleFinanceBtn); }
    @FXML private void selectIt()       { selectRole(Role.IT,       roleItBtn); }
    @FXML private void selectEmployee() { selectRole(Role.EMPLOYEE, roleEmployeeBtn); }

    private void selectRole(Role role, Button activeBtn) {
        selectedRole = role;
        selectedRoleLabel.setText(role.name());
        highlightRoleButton(activeBtn);
        renderMatrix(role);
        UiMessageUtil.clearMessage(moduleMessageLabel);
    }

    private void highlightRoleButton(Button active) {
        Button[] all = { roleAdminBtn, roleHrBtn, roleFinanceBtn, roleItBtn, roleEmployeeBtn };
        for (Button b : all) {
            if (b != null) b.getStyleClass().remove("selected-nav");
        }
        if (active != null && !active.getStyleClass().contains("selected-nav")) {
            active.getStyleClass().add("selected-nav");
        }
    }

    // ── Permission matrix ─────────────────────────────────────────────────────

    private void loadAllPermissions() {
        try {
            allPermissions = permissionService.getAllPermissions();
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    private void renderMatrix(Role role) {
        permissionMatrix.getChildren().clear();
        permissionChecks.clear();

        Set<Permission> rolePermissions;
        try {
            rolePermissions = permissionService.getPermissionsByRole(role);
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
            return;
        }

        Set<String> grantedKeys = rolePermissions.stream()
                .map(Permission::getPermissionKey)
                .collect(Collectors.toSet());

        // Group by module
        Map<String, List<Permission>> byModule = new LinkedHashMap<>();
        for (Permission p : allPermissions) {
            String module = p.getModule() != null ? p.getModule() : "Other";
            byModule.computeIfAbsent(module, k -> new ArrayList<>()).add(p);
        }

        for (Map.Entry<String, List<Permission>> entry : byModule.entrySet()) {
            Label moduleHeader = new Label(entry.getKey().toUpperCase());
            moduleHeader.getStyleClass().add("field-label");
            moduleHeader.setStyle("-fx-padding: 8 0 4 0;");
            permissionMatrix.getChildren().add(moduleHeader);

            for (Permission p : entry.getValue()) {
                boolean granted = grantedKeys.contains(p.getPermissionKey());
                CheckBox cb = new CheckBox();
                cb.setSelected(granted);

                String desc = p.getDescription() != null && !p.getDescription().isBlank()
                        ? p.getDescription() : p.getPermissionKey();

                Label descLabel = new Label(desc);
                descLabel.getStyleClass().add("data-line");
                descLabel.setWrapText(true);
                HBox.setHgrow(descLabel, javafx.scene.layout.Priority.ALWAYS);

                HBox row = new HBox(8, cb, descLabel);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-padding: 2 0;");
                permissionMatrix.getChildren().add(row);

                // Map permission key enum to checkbox if it exists
                try {
                    PermissionKey key = PermissionKey.valueOf(p.getPermissionKey());
                    permissionChecks.put(key, cb);
                } catch (IllegalArgumentException ignored) {
                    // DB has a key not in the enum — store with a sentinel approach via tag
                    cb.setUserData(p.getPermissionKey());
                }
            }
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        if (selectedRole == null) return;

        EnumSet<PermissionKey> selected = EnumSet.noneOf(PermissionKey.class);
        for (Map.Entry<PermissionKey, CheckBox> entry : permissionChecks.entrySet()) {
            if (entry.getValue().isSelected()) {
                selected.add(entry.getKey());
            }
        }

        try {
            permissionService.updateRolePermissions(selectedRole, selected);
            UiMessageUtil.setSuccess(moduleMessageLabel,
                    "Permissions saved for role: " + selectedRole.name());
            // Re-render to reflect saved state
            renderMatrix(selectedRole);
        } catch (Exception e) {
            UiMessageUtil.setError(moduleMessageLabel, UiMessageUtil.toUserMessage(e));
        }
    }

    @FXML
    private void handleSelectAll() {
        permissionChecks.values().forEach(cb -> cb.setSelected(true));
    }

    @FXML
    private void handleClearAll() {
        permissionChecks.values().forEach(cb -> cb.setSelected(false));
    }
}
