package motorph.ui.employee;

import motorph.domain.model.PayrollResult;
import motorph.domain.model.Payrun;

import java.time.LocalDate;

/** Holds one payroll summary row for display in the payslip table. */
public class PayrollRowItem {

    private final PayrollResult result;
    private final Payrun payrun;

    // constructor
    public PayrollRowItem(PayrollResult result, Payrun payrun) {
        this.result = result;
        this.payrun = payrun;
    }

    // getters
    public int getPayrunId() {
        return result.getPayrunId();
    }

    public LocalDate getPeriodStart() {
        return payrun.getStartDate();
    }

    public LocalDate getPeriodEnd() {
        return payrun.getEndDate();
    }

    public double getBasicPay() {
        return result.getGrossPay();
    }

    public double getGrossPay() {
        return result.getGrossPay();
    }

    public double getDeductions() {
        return result.getTotalDeductions();
    }

    public double getNetPay() {
        return result.getNetPay();
    }
}
