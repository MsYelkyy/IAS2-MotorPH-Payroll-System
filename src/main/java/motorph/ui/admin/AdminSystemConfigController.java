package motorph.ui.admin;

import motorph.application.service.TwoFactorService;
import motorph.api.ApiClientFactory;
import motorph.api.SystemConfigApiClient;
import motorph.api.dto.systemconfig.PhilHealthRateDTO;
import motorph.api.dto.systemconfig.SssRateDTO;
import motorph.api.dto.systemconfig.PagIbigRateDTO;
import motorph.api.dto.systemconfig.WithholdingTaxBracketDTO;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles the Admin System Configuration screen.
 * Includes statutory rate tables, edit forms, and OTP settings.
 */
public class AdminSystemConfigController {

    // Constants
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final String ADD_FORM_PANEL_CLASS = "admin-add-form-panel";

    // Hardcoded deduction type IDs per business rules
    private static final int DEDUCTION_TYPE_SSS       = 1;
    private static final int DEDUCTION_TYPE_PHILHEALTH = 2;
    private static final int DEDUCTION_TYPE_PAGIBIG    = 3;
    private static final int DEDUCTION_TYPE_TAX        = 4;

    // Services
    private SystemConfigApiClient configApiClient;

    // State
    private ConfigCategory selectedCategory;

    // Selected rate table row
    private Object selectedRate;

    // Edit mode flag
    private boolean editMode;

    // Active form controls
    private final List<Object> formControls = new ArrayList<>();

    // FXML injections
    // Toolbar
    @FXML private Button backButton;
    @FXML private Button addRateButton;
    @FXML private Button editRateButton;
    @FXML private Label  toolbarTitleLabel;
    @FXML private Label  moduleMessageLabel;

    // Form header / error
    @FXML private Label formTitleLabel;
    @FXML private Label formErrorLabel;

    // OTP panel labels
    @FXML private Label otpLengthLabel;
    @FXML private Label otpExpiryLabel;
    @FXML private Label otpAttemptsLabel;

    // Panels
    @FXML private VBox categoryListPanel;
    @FXML private VBox rateTablePanel;
    @FXML private Region formBackdrop;
    @FXML private VBox rateFormPanel;
    @FXML private VBox otpInfoPanel;

    // Panel internals
    @FXML private HBox      rateTableHeader;
    @FXML private VBox      rateRows;
    @FXML private VBox      categoryRows;
    @FXML private GridPane  formGrid;

    // Lifecycle
    @FXML
    private void initialize() {
        configApiClient = ApiClientFactory.getInstance().getSystemConfigApiClient();
    }

    // Refreshes the screen after tab selection
    public void refresh() {
        selectedCategory = null;
        selectedRate     = null;
        editMode         = false;
        showCategoryList();
    }

    // Toolbar handlers
    @FXML
    private void handleBack() {
        if (rateFormPanel.isVisible()) {
            // Form → Table
            showRateTable(selectedCategory);
        } else if (rateTablePanel.isVisible() || otpInfoPanel.isVisible()) {
            // Table / OTP → Category list
            showCategoryList();
        } else {
            showCategoryList();
        }
    }

    @FXML
    private void handleAddRate() {
        if (selectedCategory == null) return;
        openForm(false, null);
    }

    @FXML
    private void handleEditRate() {
        if (selectedRate == null) return;
        openForm(true, selectedRate);
    }

    // Panel 1: Category list
    private void showCategoryList() {
        selectedRate = null;
        buildCategoryRows();
        toolbarTitleLabel.setText("System Configuration");
        setVis(backButton,    false);
        setVis(addRateButton, false);
        setVis(editRateButton, false);
        showPanel(categoryListPanel);
    }

    private void buildCategoryRows() {
        categoryRows.getChildren().clear();
        for (ConfigCategory cat : ConfigCategory.values()) {
            categoryRows.getChildren().add(buildCategoryRow(cat));
        }
    }

    private HBox buildCategoryRow(ConfigCategory cat) {
        HBox row = new HBox(0);
        row.getStyleClass().add("table-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        Label nameLabel = new Label(cat.displayName());
        nameLabel.getStyleClass().addAll("data-line", "table-cell");
        nameLabel.setMinWidth(220);
        nameLabel.setPrefWidth(220);

        Label descLabel = new Label(cat.description());
        descLabel.getStyleClass().addAll("data-line", "table-cell");
        descLabel.setMinWidth(420);
        descLabel.setPrefWidth(420);

        Label spacer = new Label();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewBtn = new Button("View");
        viewBtn.getStyleClass().add("table-action-button");
        viewBtn.setOnAction(ev -> selectCategory(cat));

        row.getChildren().addAll(nameLabel, descLabel, spacer, actionsBox(90, viewBtn));
        return row;
    }

    private void selectCategory(ConfigCategory cat) {
        selectedCategory = cat;
        if (cat == ConfigCategory.OTP_INFO) {
            showOtpPanel();
        } else {
            showRateTable(cat);
        }
    }

    // Panel 4: OTP info
    private void showOtpPanel() {
        otpLengthLabel.setText(String.valueOf(TwoFactorService.getOtpLength()));
        otpExpiryLabel.setText(TwoFactorService.getOtpExpiryMinutes() + " minutes");
        otpAttemptsLabel.setText(String.valueOf(TwoFactorService.getMaxAttempts()));

        toolbarTitleLabel.setText(ConfigCategory.OTP_INFO.displayName());
        setVis(backButton,     true);
        setVis(addRateButton,  false);
        setVis(editRateButton, false);
        showPanel(otpInfoPanel);
    }

    // Panel 2: Rate table
    private void showRateTable(ConfigCategory cat) {
        selectedRate = null;
        buildTableHeader(cat);
        loadRateRows(cat);

        toolbarTitleLabel.setText(cat.displayName());
        setVis(backButton,     true);
        setVis(addRateButton,  true);
        setVis(editRateButton, false);
        showPanel(rateTablePanel);
    }

    private void buildTableHeader(ConfigCategory cat) {
        rateTableHeader.getChildren().clear();
        String[] cols = cat.tableColumns();
        for (String col : cols) {
            Label lbl = new Label(col);
            lbl.getStyleClass().add("table-head");
            lbl.setMinWidth(110);
            lbl.setPrefWidth(110);
            rateTableHeader.getChildren().add(lbl);
        }

        Label spacer = new Label();
        spacer.getStyleClass().add("table-head");
        HBox.setHgrow(spacer, Priority.ALWAYS);
        rateTableHeader.getChildren().add(spacer);

        Label actions = new Label("ACTIONS");
        actions.getStyleClass().add("table-head");
        actions.setMinWidth(90);
        actions.setPrefWidth(90);
        rateTableHeader.getChildren().add(actions);
    }

    private void loadRateRows(ConfigCategory cat) {
        rateRows.getChildren().clear();
        try {
            switch (cat) {
                case PHILHEALTH -> {
                    List<PhilHealthRateDTO> list = configApiClient.getAllPhilHealthRates();
                    if (list.isEmpty()) {
                        rateRows.getChildren().add(emptyLabel("No PhilHealth rates configured."));
                    } else {
                        for (PhilHealthRateDTO dto : list) rateRows.getChildren().add(buildPhilHealthRow(dto));
                    }
                }
                case SSS -> {
                    List<SssRateDTO> list = configApiClient.getAllSssRates();
                    if (list.isEmpty()) {
                        rateRows.getChildren().add(emptyLabel("No SSS rates configured."));
                    } else {
                        for (SssRateDTO dto : list) rateRows.getChildren().add(buildSssRow(dto));
                    }
                }
                case PAGIBIG -> {
                    List<PagIbigRateDTO> list = configApiClient.getAllPagIbigRates();
                    if (list.isEmpty()) {
                        rateRows.getChildren().add(emptyLabel("No Pag-IBIG rates configured."));
                    } else {
                        for (PagIbigRateDTO dto : list) rateRows.getChildren().add(buildPagIbigRow(dto));
                    }
                }
                case TAX -> {
                    List<WithholdingTaxBracketDTO> list = configApiClient.getAllTaxBrackets();
                    if (list.isEmpty()) {
                        rateRows.getChildren().add(emptyLabel("No tax brackets configured."));
                    } else {
                        for (WithholdingTaxBracketDTO dto : list) rateRows.getChildren().add(buildTaxRow(dto));
                    }
                }
                default -> rateRows.getChildren().add(emptyLabel("Select a category."));
            }
        } catch (Exception e) {
            rateRows.getChildren().add(emptyLabel("Failed to load rates: " + e.getMessage()));
            setMessage("Error loading rates: " + e.getMessage());
        }
    }

    // Row builders
    private HBox buildPhilHealthRow(PhilHealthRateDTO dto) {
        HBox row = new HBox(0);
        row.getStyleClass().add("table-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        String status = isActive(dto.getEffectiveStartDate(), dto.getEffectiveEndDate()) ? "Active" : "Closed";
        row.getChildren().addAll(
                col(String.valueOf(dto.getPhilhealthRateId()), 110),
                col(fmtPct(dto.getPremiumRate()), 110),
                col(fmtNum(dto.getSalaryFloor()), 110),
                col(fmtNum(dto.getSalaryCeiling()), 110),
                col(fmtPct(dto.getEmployeeShareRatio()), 110),
                col(fmtPct(dto.getEmployerShareRatio()), 110),
                col(fmtDate(dto.getEffectiveStartDate()), 110),
                col(fmtDate(dto.getEffectiveEndDate()), 110),
                col(status, 110),
                growSpacer(),
                actionsBox(90, editButton(dto))
        );
        return row;
    }

    private HBox buildSssRow(SssRateDTO dto) {
        HBox row = new HBox(0);
        row.getStyleClass().add("table-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        String status = isActive(dto.getEffectiveStartDate(), dto.getEffectiveEndDate()) ? "Active" : "Closed";
        row.getChildren().addAll(
                col(String.valueOf(dto.getSssRateId()), 110),
                col(fmtNum(dto.getMinSalary()), 110),
                col(fmtNum(dto.getMaxSalary()), 110),
                col(fmtNum(dto.getEmployeeShare()), 110),
                col(fmtNum(dto.getEmployerShare()), 110),
                col(fmtDate(dto.getEffectiveStartDate()), 110),
                col(fmtDate(dto.getEffectiveEndDate()), 110),
                col(status, 110),
                growSpacer(),
                actionsBox(90, editButton(dto))
        );
        return row;
    }

    private HBox buildPagIbigRow(PagIbigRateDTO dto) {
        HBox row = new HBox(0);
        row.getStyleClass().add("table-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        String status = isActive(dto.getEffectiveStartDate(), dto.getEffectiveEndDate()) ? "Active" : "Closed";
        String maxEmp = dto.getMaxEmployeeShare() != null ? fmtNum(dto.getMaxEmployeeShare()) : "—";
        row.getChildren().addAll(
                col(String.valueOf(dto.getPagibigRateId()), 110),
                col(fmtNum(dto.getMinSalary()), 110),
                col(fmtNum(dto.getMaxSalary()), 110),
                col(fmtPct(dto.getEmployeeShareRate()), 110),
                col(fmtPct(dto.getEmployerShareRate()), 110),
                col(maxEmp, 110),
                col(fmtDate(dto.getEffectiveStartDate()), 110),
                col(fmtDate(dto.getEffectiveEndDate()), 110),
                col(status, 110),
                growSpacer(),
                actionsBox(90, editButton(dto))
        );
        return row;
    }

    private HBox buildTaxRow(WithholdingTaxBracketDTO dto) {
        HBox row = new HBox(0);
        row.getStyleClass().add("table-data-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        String status = isActive(dto.getEffectiveStartDate(), dto.getEffectiveEndDate()) ? "Active" : "Closed";
        row.getChildren().addAll(
                col(String.valueOf(dto.getTaxBracketId()), 110),
                col(fmtNum(dto.getMinSalary()), 110),
                col(fmtNum(dto.getMaxSalary()), 110),
                col(fmtNum(dto.getBaseTax()), 110),
                col(fmtPct(dto.getTaxRate()), 110),
                col(fmtNum(dto.getExcessOver()), 110),
                col(fmtDate(dto.getEffectiveStartDate()), 110),
                col(fmtDate(dto.getEffectiveEndDate()), 110),
                col(status, 110),
                growSpacer(),
                actionsBox(90, editButton(dto))
        );
        return row;
    }

    private void selectRate(Object rate) {
        selectedRate = rate;
        setVis(editRateButton, true);
    }

    // Panel 3: Rate form
    private Button editButton(Object rate) {
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("table-action-button");
        editBtn.setOnAction(ev -> {
            selectedRate = rate;
            openForm(true, rate);
        });
        return editBtn;
    }

    private HBox actionsBox(double width, Button... buttons) {
        HBox actions = new HBox(6);
        actions.getChildren().addAll(buttons);
        actions.setMinWidth(width);
        actions.setPrefWidth(width);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setStyle("-fx-padding: 0 0 0 12;");
        return actions;
    }

    private Label growSpacer() {
        Label spacer = new Label();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private void openForm(boolean isEdit, Object existingRate) {
        editMode     = isEdit;
        formControls.clear();
        formGrid.getChildren().clear();
        formGrid.getRowConstraints().clear();
        formErrorLabel.setText("");

        if (isEdit) {
            formTitleLabel.setText("Edit " + selectedCategory.displayName());
        } else {
            formTitleLabel.setText("Add " + selectedCategory.displayName());
        }
        applyAddFormVisuals(!isEdit);

        switch (selectedCategory) {
            case PHILHEALTH -> buildPhilHealthForm(isEdit ? (PhilHealthRateDTO) existingRate : null);
            case SSS        -> buildSssForm(isEdit ? (SssRateDTO) existingRate : null);
            case PAGIBIG    -> buildPagIbigForm(isEdit ? (PagIbigRateDTO) existingRate : null);
            case TAX        -> buildTaxForm(isEdit ? (WithholdingTaxBracketDTO) existingRate : null);
            default         -> { /* OTP_INFO is read-only, unreachable here */ }
        }

        toolbarTitleLabel.setText(isEdit ? "Edit Rate" : "Add Rate");
        setVis(backButton,     true);
        setVis(addRateButton,  false);
        setVis(editRateButton, false);
        showPanel(rateFormPanel);
    }

    // PhilHealth form fields: premiumRate, salaryFloor, salaryCeiling,
    //                          employeeShareRatio, employerShareRatio, startDate, endDate
    private void buildPhilHealthForm(PhilHealthRateDTO existing) {
        String[] labels = {"Premium Rate", "Salary Floor", "Salary Ceiling",
                           "Employee Share Ratio", "Employer Share Ratio",
                           "Effective Start Date", "Effective End Date"};
        String[] defaults = existing == null ? new String[7] : new String[]{
                String.valueOf(existing.getPremiumRate()),
                String.valueOf(existing.getSalaryFloor()),
                String.valueOf(existing.getSalaryCeiling()),
                String.valueOf(existing.getEmployeeShareRatio()),
                String.valueOf(existing.getEmployerShareRatio()),
                null, null  // dates handled separately
        };
        LocalDate startDefault = existing != null ? existing.getEffectiveStartDate() : null;
        LocalDate endDefault   = existing != null ? existing.getEffectiveEndDate()   : null;

        addTextRow(0, labels[0], defaults[0]);
        addTextRow(1, labels[1], defaults[1]);
        addTextRow(2, labels[2], defaults[2]);
        addTextRow(3, labels[3], defaults[3]);
        addTextRow(4, labels[4], defaults[4]);
        addDateRow(5, labels[5], startDefault);
        addDateRow(6, labels[6], endDefault);
    }

    // SSS form fields: minSalary, maxSalary, employeeShare, employerShare, startDate, endDate
    private void buildSssForm(SssRateDTO existing) {
        String[] defaults = existing == null ? new String[4] : new String[]{
                String.valueOf(existing.getMinSalary()),
                String.valueOf(existing.getMaxSalary()),
                String.valueOf(existing.getEmployeeShare()),
                String.valueOf(existing.getEmployerShare())
        };
        LocalDate startDefault = existing != null ? existing.getEffectiveStartDate() : null;
        LocalDate endDefault   = existing != null ? existing.getEffectiveEndDate()   : null;

        addTextRow(0, "Min Salary",      defaults[0]);
        addTextRow(1, "Max Salary",      defaults[1]);
        addTextRow(2, "Employee Share",  defaults[2]);
        addTextRow(3, "Employer Share",  defaults[3]);
        addDateRow(4, "Effective Start Date", startDefault);
        addDateRow(5, "Effective End Date",   endDefault);
    }

    // PagIbig form fields: minSalary, maxSalary, employeeShareRate, employerShareRate,
    //                       maxEmployeeShare (nullable), maxEmployerShare (nullable), startDate, endDate
    private void buildPagIbigForm(PagIbigRateDTO existing) {
        String[] defaults = existing == null ? new String[6] : new String[]{
                String.valueOf(existing.getMinSalary()),
                String.valueOf(existing.getMaxSalary()),
                String.valueOf(existing.getEmployeeShareRate()),
                String.valueOf(existing.getEmployerShareRate()),
                existing.getMaxEmployeeShare() != null ? String.valueOf(existing.getMaxEmployeeShare()) : "",
                existing.getMaxEmployerShare() != null ? String.valueOf(existing.getMaxEmployerShare()) : ""
        };
        LocalDate startDefault = existing != null ? existing.getEffectiveStartDate() : null;
        LocalDate endDefault   = existing != null ? existing.getEffectiveEndDate()   : null;

        addTextRow(0, "Min Salary",               defaults[0]);
        addTextRow(1, "Max Salary",               defaults[1]);
        addTextRow(2, "Employee Share Rate",      defaults[2]);
        addTextRow(3, "Employer Share Rate",      defaults[3]);
        addTextRow(4, "Max Employee Share (opt)", defaults[4]);
        addTextRow(5, "Max Employer Share (opt)", defaults[5]);
        addDateRow(6, "Effective Start Date",     startDefault);
        addDateRow(7, "Effective End Date",       endDefault);
    }

    // Tax form fields: minSalary, maxSalary, baseTax, taxRate, excessOver, startDate, endDate
    private void buildTaxForm(WithholdingTaxBracketDTO existing) {
        String[] defaults = existing == null ? new String[5] : new String[]{
                String.valueOf(existing.getMinSalary()),
                String.valueOf(existing.getMaxSalary()),
                String.valueOf(existing.getBaseTax()),
                String.valueOf(existing.getTaxRate()),
                String.valueOf(existing.getExcessOver())
        };
        LocalDate startDefault = existing != null ? existing.getEffectiveStartDate() : null;
        LocalDate endDefault   = existing != null ? existing.getEffectiveEndDate()   : null;

        addTextRow(0, "Min Salary",           defaults[0]);
        addTextRow(1, "Max Salary",           defaults[1]);
        addTextRow(2, "Base Tax",             defaults[2]);
        addTextRow(3, "Tax Rate",             defaults[3]);
        addTextRow(4, "Excess Over",          defaults[4]);
        addDateRow(5, "Effective Start Date", startDefault);
        addDateRow(6, "Effective End Date",   endDefault);
    }

    // Form grid helpers
    private void addTextRow(int row, String labelText, String value) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("field-label");
        GridPane.setMargin(lbl, new Insets(4, 8, 4, 0));

        TextField field = new TextField(value != null ? value : "");
        field.getStyleClass().add("input-field");
        field.setPrefWidth(260);

        formGrid.add(lbl,   0, row);
        formGrid.add(field, 1, row);
        formControls.add(field);
    }

    private void addDateRow(int row, String labelText, LocalDate value) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("field-label");
        GridPane.setMargin(lbl, new Insets(4, 8, 4, 0));

        DatePicker picker = new DatePicker(value);
        picker.getStyleClass().add("input-field");
        picker.setPrefWidth(260);

        formGrid.add(lbl,    0, row);
        formGrid.add(picker, 1, row);
        formControls.add(picker);
    }

    // Form save
    @FXML
    private void saveForm() {
        formErrorLabel.setText("");

        if (editMode) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Save changes to this " + selectedCategory.displayName() + " entry?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Confirm Edit");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.YES) return;
        }

        try {
            switch (selectedCategory) {
                case PHILHEALTH -> savePhilHealth();
                case SSS        -> saveSss();
                case PAGIBIG    -> savePagIbig();
                case TAX        -> saveTax();
                default         -> { /* no-op */ }
            }
            setMessage((editMode ? "Updated" : "Added") + " " + selectedCategory.displayName() + " successfully.");
            showRateTable(selectedCategory);
        } catch (IllegalArgumentException | IllegalStateException e) {
            formErrorLabel.setText(e.getMessage());
        } catch (Exception e) {
            formErrorLabel.setText("Unexpected error: " + e.getMessage());
        }
    }

    private void savePhilHealth() {
        // formControls order: premiumRate[0], salaryFloor[1], salaryCeiling[2],
        //                      employeeShareRatio[3], employerShareRatio[4],
        //                      startDate[5], endDate[6]
        double premiumRate         = requireDouble(0, "Premium Rate");
        double salaryFloor         = requireDouble(1, "Salary Floor");
        double salaryCeiling       = requireDouble(2, "Salary Ceiling");
        double employeeShareRatio  = requireDouble(3, "Employee Share Ratio");
        double employerShareRatio  = requireDouble(4, "Employer Share Ratio");
        LocalDate startDate        = requireDate(5, "Effective Start Date");
        LocalDate endDate          = optionalDate(6);

        int id = editMode ? ((PhilHealthRateDTO) selectedRate).getPhilhealthRateId() : 0;
        PhilHealthRateDTO dto = new PhilHealthRateDTO(
                id, DEDUCTION_TYPE_PHILHEALTH,
                premiumRate, salaryFloor, salaryCeiling,
                employeeShareRatio, employerShareRatio,
                startDate, endDate);

        if (editMode) {
            configApiClient.updatePhilHealthRate(dto);
        } else {
            configApiClient.addPhilHealthRate(dto);
        }
    }

    private void saveSss() {
        // formControls order: minSalary[0], maxSalary[1], employeeShare[2], employerShare[3],
        //                      startDate[4], endDate[5]
        double minSalary      = requireDouble(0, "Min Salary");
        double maxSalary      = requireDouble(1, "Max Salary");
        double employeeShare  = requireDouble(2, "Employee Share");
        double employerShare  = requireDouble(3, "Employer Share");
        LocalDate startDate   = requireDate(4, "Effective Start Date");
        LocalDate endDate     = optionalDate(5);

        int id = editMode ? ((SssRateDTO) selectedRate).getSssRateId() : 0;
        SssRateDTO dto = new SssRateDTO(
                id, DEDUCTION_TYPE_SSS,
                minSalary, maxSalary,
                employeeShare, employerShare,
                startDate, endDate);

        if (editMode) {
            configApiClient.updateSssRate(dto);
        } else {
            configApiClient.addSssRate(dto);
        }
    }

    private void savePagIbig() {
        // formControls order: minSalary[0], maxSalary[1], employeeShareRate[2], employerShareRate[3],
        //                      maxEmployeeShare[4], maxEmployerShare[5], startDate[6], endDate[7]
        double minSalary          = requireDouble(0, "Min Salary");
        double maxSalary          = requireDouble(1, "Max Salary");
        double employeeShareRate  = requireDouble(2, "Employee Share Rate");
        double employerShareRate  = requireDouble(3, "Employer Share Rate");
        Double maxEmployeeShare   = optionalDouble(4);
        Double maxEmployerShare   = optionalDouble(5);
        LocalDate startDate       = requireDate(6, "Effective Start Date");
        LocalDate endDate         = optionalDate(7);

        int id = editMode ? ((PagIbigRateDTO) selectedRate).getPagibigRateId() : 0;
        PagIbigRateDTO dto = new PagIbigRateDTO(
                id, DEDUCTION_TYPE_PAGIBIG,
                minSalary, maxSalary,
                employeeShareRate, employerShareRate,
                maxEmployeeShare, maxEmployerShare,
                startDate, endDate);

        if (editMode) {
            configApiClient.updatePagIbigRate(dto);
        } else {
            configApiClient.addPagIbigRate(dto);
        }
    }

    private void saveTax() {
        // formControls order: minSalary[0], maxSalary[1], baseTax[2], taxRate[3], excessOver[4],
        //                      startDate[5], endDate[6]
        double minSalary   = requireDouble(0, "Min Salary");
        double maxSalary   = requireDouble(1, "Max Salary");
        double baseTax     = requireDouble(2, "Base Tax");
        double taxRate     = requireDouble(3, "Tax Rate");
        double excessOver  = requireDouble(4, "Excess Over");
        LocalDate startDate = requireDate(5, "Effective Start Date");
        LocalDate endDate   = optionalDate(6);

        int id = editMode ? ((WithholdingTaxBracketDTO) selectedRate).getTaxBracketId() : 0;
        WithholdingTaxBracketDTO dto = new WithholdingTaxBracketDTO(
                id, DEDUCTION_TYPE_TAX,
                minSalary, maxSalary,
                baseTax, taxRate, excessOver,
                startDate, endDate);

        if (editMode) {
            configApiClient.updateTaxBracket(dto);
        } else {
            configApiClient.addTaxBracket(dto);
        }
    }

    // Form input extraction helpers
    // Reads a required decimal field
    private double requireDouble(int index, String fieldName) {
        Object ctrl = formControls.get(index);
        if (!(ctrl instanceof TextField tf)) {
            throw new IllegalStateException("Internal error: expected TextField at index " + index);
        }
        String text = tf.getText() == null ? "" : tf.getText().trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    // Reads an optional decimal field
    private Double optionalDouble(int index) {
        Object ctrl = formControls.get(index);
        if (!(ctrl instanceof TextField tf)) return null;
        String text = tf.getText() == null ? "" : tf.getText().trim();
        if (text.isBlank()) return null;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Field at position " + (index + 1) + " must be a valid number.");
        }
    }

    // Reads a required date field
    private LocalDate requireDate(int index, String fieldName) {
        Object ctrl = formControls.get(index);
        if (!(ctrl instanceof DatePicker dp)) {
            throw new IllegalStateException("Internal error: expected DatePicker at index " + index);
        }
        LocalDate value = dp.getValue();
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }

    // Reads an optional date field
    private LocalDate optionalDate(int index) {
        Object ctrl = formControls.get(index);
        if (!(ctrl instanceof DatePicker dp)) return null;
        return dp.getValue();
    }

    // Panel management
    // Shows the requested panel
    private void showPanel(VBox target) {
        setPane(categoryListPanel, target == categoryListPanel);
        setPane(rateTablePanel,    target == rateTablePanel || target == rateFormPanel);
        setPane(formBackdrop,      target == rateFormPanel);
        setPane(rateFormPanel,     target == rateFormPanel);
        setPane(otpInfoPanel,      target == otpInfoPanel);
    }

    private void setPane(javafx.scene.Node n, boolean show) {
        if (n != null) { n.setVisible(show); n.setManaged(show); }
    }

    private void setVis(javafx.scene.Node n, boolean show) {
        if (n != null) { n.setVisible(show); n.setManaged(show); }
    }

    private void applyAddFormVisuals(boolean addMode) {
        if (rateFormPanel == null) return;
        rateFormPanel.getStyleClass().remove(ADD_FORM_PANEL_CLASS);
        if (addMode) {
            rateFormPanel.getStyleClass().add(ADD_FORM_PANEL_CLASS);
        }
    }

    // Cell / label factories
    private Label col(String text, double width) {
        Label lbl = new Label(text != null ? text : "—");
        lbl.getStyleClass().addAll("data-line", "table-cell");
        lbl.setMinWidth(width);
        lbl.setPrefWidth(width);
        return lbl;
    }

    private Label growCol(String text) {
        Label lbl = new Label(text != null ? text : "—");
        lbl.getStyleClass().addAll("data-line", "table-cell");
        HBox.setHgrow(lbl, Priority.ALWAYS);
        return lbl;
    }

    private Label emptyLabel(String msg) {
        Label lbl = new Label(msg);
        lbl.getStyleClass().add("muted-text");
        lbl.setStyle("-fx-padding: 16;");
        return lbl;
    }

    // Format helpers
    private String fmtNum(double value) {
        return String.format("%.2f", value);
    }

    private String fmtNum(Double value) {
        return value != null ? String.format("%.2f", value) : "—";
    }

    private String fmtPct(double value) {
        return String.format("%.4f", value);
    }

    private String fmtDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "Open";
    }

    private boolean isActive(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        if (start != null && start.isAfter(today)) return false;
        return end == null || !end.isBefore(today);
    }

    // Message helpers
    private void setMessage(String msg) {
        if (moduleMessageLabel != null) moduleMessageLabel.setText(msg);
    }
}
