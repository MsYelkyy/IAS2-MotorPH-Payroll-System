package motorph.ui.admin;

/**
 * Defines Admin System Configuration categories.
 * Includes labels, descriptions, and table columns.
 */
public enum ConfigCategory {

    PHILHEALTH(
            "PhilHealth Rates",
            "Premium rate brackets and employee/employer share ratios",
            new String[]{"ID", "Premium Rate", "Salary Floor", "Salary Ceiling",
                         "Emp Share", "Empl Share", "Eff. Start", "Eff. End", "Status"}
    ),
    SSS(
            "SSS Contribution Rates",
            "Salary bracket employee and employer contributions",
            new String[]{"ID", "Min Salary", "Max Salary",
                         "Emp Share", "Empl Share", "Eff. Start", "Eff. End", "Status"}
    ),
    PAGIBIG(
            "Pag-IBIG Rates",
            "Contribution rates and maximum caps per salary bracket",
            new String[]{"ID", "Min Salary", "Max Salary", "Emp Rate", "Empl Rate",
                         "Max Emp Share", "Eff. Start", "Eff. End", "Status"}
    ),
    TAX(
            "Withholding Tax Brackets",
            "Tax brackets, base amounts, and rates",
            new String[]{"ID", "Min Salary", "Max Salary", "Base Tax",
                         "Tax Rate", "Excess Over", "Eff. Start", "Eff. End", "Status"}
    ),
    OTP_INFO(
            "OTP Configuration",
            "Current OTP expiry settings (read-only)",
            new String[]{}
    );

    private final String displayName;
    private final String description;
    private final String[] tableColumns;

    ConfigCategory(String displayName, String description, String[] tableColumns) {
        this.displayName  = displayName;
        this.description  = description;
        this.tableColumns = tableColumns;
    }

    public String displayName()   { return displayName; }
    public String description()   { return description; }
    public String[] tableColumns() { return tableColumns; }
}
