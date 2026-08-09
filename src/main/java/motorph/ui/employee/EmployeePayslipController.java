package motorph.ui.employee;

import motorph.api.ApiClientFactory;
import motorph.api.ApiException;
import motorph.api.PayslipApiClient;
import motorph.api.dto.payroll.PayslipSummaryDTO;
import motorph.ui.session.CurrentUserSession;
import motorph.ui.util.UiFormatter;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Displays the employee's payroll history as a paginated summary table. */
public class EmployeePayslipController {

    private static final Logger LOG = Logger.getLogger(EmployeePayslipController.class.getName());
    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MMMM d");
    private static final DateTimeFormatter MONTH_DAY_YEAR = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private PayslipApiClient payslipApiClient;
    private final List<PayslipSummaryDTO> allRows = new ArrayList<>();
    private int currentPage = 1;

    @FXML private TableView<PayslipSummaryDTO> payrollTable;
    @FXML private TableColumn<PayslipSummaryDTO, String> colPayPeriod;
    @FXML private TableColumn<PayslipSummaryDTO, String> colBasicPay;
    @FXML private TableColumn<PayslipSummaryDTO, String> colGrossPay;
    @FXML private TableColumn<PayslipSummaryDTO, String> colDeductions;
    @FXML private TableColumn<PayslipSummaryDTO, String> colNetPay;
    @FXML private TableColumn<PayslipSummaryDTO, Void>   colAction;
    @FXML private Label  recordInfoLabel;
    @FXML private Label  pageLabel;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;

    @FXML
    private void initialize() {
        payslipApiClient = ApiClientFactory.getInstance().getPayslipApiClient();

        payrollTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        setupColumns();
    }

    // reloads payroll data when the view becomes active
    public void refresh() {
        loadPayrollData();
    }

    // fetches the caller's own payroll history from the backend
    private void loadPayrollData() {
        allRows.clear();
        try {
            List<PayslipSummaryDTO> results = payslipApiClient.getOwnPayslips();
            results.sort(Comparator.comparing(PayslipSummaryDTO::getPeriodStart).reversed());
            allRows.addAll(results);
        } catch (Exception e) {
            LOG.warning("Failed to load payroll data: " + e.getMessage());
        }

        currentPage = 1;
        renderPage();
    }

    // refreshes the table items and footer labels for the current page
    private void renderPage() {
        int total = allRows.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        int from = (currentPage - 1) * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);

        payrollTable.getItems().setAll(allRows.subList(from, to));

        pageLabel.setText("Page " + currentPage + " of " + totalPages);
        recordInfoLabel.setText(total == 0
                ? "No payroll records found."
                : "ⓘ Showing " + (from + 1) + " to " + to + " of " + total + " records");

        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= totalPages);
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            renderPage();
        }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) allRows.size() / PAGE_SIZE));
        if (currentPage < totalPages) {
            currentPage++;
            renderPage();
        }
    }

    // wires cell value factories and cell factories for all table columns
    private void setupColumns() {
        colPayPeriod.setCellValueFactory(data -> {
            PayslipSummaryDTO row = data.getValue();
            String period = row.getPeriodStart().format(MONTH_DAY)
                    + " - " + row.getPeriodEnd().format(MONTH_DAY_YEAR);
            return new SimpleStringProperty(period);
        });

        colBasicPay.setCellValueFactory(data ->
                new SimpleStringProperty(UiFormatter.formatPeso(data.getValue().getBasicPay())));

        colGrossPay.setCellValueFactory(data ->
                new SimpleStringProperty(UiFormatter.formatPeso(data.getValue().getGrossPay())));

        colDeductions.setCellValueFactory(data ->
                new SimpleStringProperty(UiFormatter.formatPeso(data.getValue().getDeductions())));

        colNetPay.setCellValueFactory(data ->
                new SimpleStringProperty(UiFormatter.formatPeso(data.getValue().getNetPay())));

        // net pay cells render in gold using the net-pay-cell style
        colNetPay.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    getStyleClass().remove("net-pay-cell");
                } else {
                    setText(value);
                    if (!getStyleClass().contains("net-pay-cell")) {
                        getStyleClass().add("net-pay-cell");
                    }
                }
            }
        });

        // action column renders a View Payslip button for each row
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnView = new Button("View Payslip");
            {
                btnView.getStyleClass().add("view-payslip-button");
                btnView.setOnAction(evt -> {
                    PayslipSummaryDTO row = getTableView().getItems().get(getIndex());
                    handleViewPayslip(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_RIGHT);
                setGraphic(empty ? null : btnView);
            }
        });
    }

    // downloads the payslip PDF and opens it in the OS's default PDF viewer -
    // there is no in-app preview window anymore now that generation happens
    // server-side (the backend has no GUI to pop a Swing viewer from)
    private void handleViewPayslip(PayslipSummaryDTO row) {
        int employeeId = CurrentUserSession.getEmployeeId();
        if (employeeId <= 0) return;

        try {
            byte[] pdf = payslipApiClient.getPayslipPdf(employeeId, row.getPayrunId());

            File tempFile = File.createTempFile("payslip-" + employeeId + "-" + row.getPayrunId() + "-", ".pdf");
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), pdf);

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(tempFile);
            }
        } catch (ApiException e) {
            LOG.warning("Failed to fetch payslip PDF: " + e.getMessage());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save or open payslip PDF", e);
        }
    }
}
