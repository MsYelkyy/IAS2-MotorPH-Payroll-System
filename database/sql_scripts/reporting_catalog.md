# MotorPH Payroll System — Database Reporting Catalog

**Project:** MotorPH Payroll System
**Group:** Group 5 / ByteBabes
**DBMS:** MySQL 8.x
**Document purpose:** Catalog the database reporting solutions of the MotorPH
Payroll System — the data-extraction techniques used, the reports produced, and
the output formats — as evidence of comprehensive, reusable reporting.

---

## 1. Reporting Overview

Payroll data in MotorPH is stored **normalized** across many tables: a
`payroll_result` header for each employee/pay-run, separate child rows for every
earning, benefit, and deduction, and reference data spread across `employee`,
`position`, `department`, `government_details`, `payrun`, and `salary_history`.

Turning that normalized data into a readable payslip or summary requires
**multi-table joins, pivoting component rows into columns, aggregation, and
business-rule calculations**. Rather than rewrite that logic in every report,
the system encapsulates it in **reusable SQL views** that any consumer
(JasperReports, ad-hoc queries, the result snapshot, the database tests) can
read directly.

**Reporting layers in the system:**

| Layer | What it does |
|-------|--------------|
| SQL Views | Encapsulate the join/pivot/aggregation logic; one row per report unit |
| JasperReports (`.jrxml`) | Render the data as printable / exportable PDF reports |
| Application report services/repositories | Java layer that feeds reports in-app |

---

## 2. Report Catalog

### 2.1 Employee Payslip Report (semi-monthly)

| Attribute | Detail |
|-----------|--------|
| **Artifact** | View `vw_employee_payslip_report` (`employee_payslip_report_view.sql`) |
| **Unit** | One row = one complete payslip (one employee, one pay run / cutoff) |
| **Source tables (11)** | `payroll_result`, `payrun`, `employee`, `position`, `department`, `employee` (self-join → supervisor), `government_details`, `salary_history`, `payroll_earning` + `earning_type`, `payroll_benefit` + `benefit_type`, `payroll_deduction` + `deduction_type` |
| **Data extracted** | Employee/position/department header, supervisor, government IDs (TIN/SSS/PhilHealth/Pag-IBIG), pay period, monthly salary, daily rate, days worked, earnings (basic, overtime), benefits (rice/phone/clothing + total), statutory deductions (SSS, PhilHealth, Pag-IBIG, withholding tax), other deductions (late/undertime/absence roll-up), and official gross/total-deductions/net |
| **Business rules applied** | Semi-monthly cutoff reporting; `daily_rate = hourly_rate × 8`; `days_worked = total_hours_worked ÷ 8`; deductions reconcile to stored total; net = gross − deductions |
| **Output** | MySQL result grid + JasperReports PDF (`payslip_report.jrxml`) |

> *(insert snapshot of: `SELECT * FROM vw_employee_payslip_report WHERE employee_id = 10001 AND pay_run_id = 13;` — single full payslip)*

> *(insert snapshot of: JasperReports rendered payslip PDF)*

---

### 2.2 Semi-Monthly Proof (same view, both cutoffs)

| Attribute | Detail |
|-----------|--------|
| **Artifact** | `vw_employee_payslip_report`, filtered to both cutoffs of one month (USAGE 2 query) |
| **Purpose** | Demonstrates the monthly→semi-monthly business rule: monthly salary shown on both rows, gross ≈ monthly ÷ 2 per cutoff, and statutory deductions applied on the 2nd cutoff (so the 1st cutoff shows little/no statutory deduction) |
| **Output** | MySQL result grid (two rows, one per cutoff) |

> *(insert snapshot of: employee 10001, periods 2024-06-01→15 and 2024-06-16→30, showing deductions appearing on the 2nd cutoff)*

---

### 2.3 Monthly Payroll Summary Report

| Attribute | Detail |
|-----------|--------|
| **Artifact** | View `vw_payroll_summary_monthly` (`payroll_summary_monthly_view.sql`) |
| **Unit** | One row = one employee, one calendar month (both semi-monthly cutoffs combined) |
| **Data extracted** | Per-employee monthly totals: gross, benefits, each statutory deduction, total deductions, net, plus `payslip_count` (= 2 confirms both cutoffs were rolled up) |
| **Technique** | Aggregation grouped by `DATE_FORMAT(end_date,'%Y-%m')` to combine semi-monthly payslips into a monthly summary |
| **Output** | MySQL result grid (organization-wide, monthly) |

> *(insert snapshot of: `vw_payroll_summary_monthly` for `payroll_month = '2024-06'`, org-wide)*

---

### 2.4 Application-Level Reports (JasperReports)

The application generates additional reports via JasperReports templates,
exported to PDF through `JasperReportGenerator` (preview / print / PDF export):

| Report | Template | Purpose |
|--------|----------|---------|
| Employee Payslip | `payslip_report.jrxml` | Individual printable payslip |
| Payroll Summary | `payroll_summary_report.jrxml` | Department/period payroll totals |
| Organization Payroll | `organization_payroll_report.jrxml` | Full org payroll detail |
| Attendance | `attendance_report.jrxml` | Attendance records |
| Employee Timecard | `employee_timecard_report.jrxml` | Per-employee timecard |
| Leave | `leave_report.jrxml` | Leave requests/credits |
| Audit | `audit_report.jrxml` | User access / audit trail |
| Support Ticket | `ticket_report.jrxml` | Support ticket report |

> *(insert snapshot of: one rendered JasperReports PDF, e.g. payroll summary or organization payroll)*

---

## 3. Advanced Data-Extraction Techniques Used

The reporting layer goes beyond basic single-table queries:

1. **Multi-table joins (11 tables)** combine normalized payroll, employee,
   organization, and government data into one report row.

2. **Pivoting via conditional aggregation** — component child rows
   (`payroll_earning` / `payroll_benefit` / `payroll_deduction`) are turned into
   named payslip columns using `SUM(CASE WHEN type.code = '…' THEN amount END)`.

3. **Pre-aggregated subqueries to prevent JOIN fan-out** — each child table is
   collapsed to one row per payslip *before* joining, so joining earnings,
   benefits, and deductions together does not multiply rows and inflate totals.
   *(This was a deliberate correctness fix.)*

4. **Stable type-code matching (not raw IDs)** — components are matched on stable
   codes (`BASIC_PAY`, `SSS`, `WITHHOLDING_TAX`, …) so the reports stay correct
   even if the reference tables are reseeded with different IDs.

5. **Effective-dated salary resolution** — a correlated subquery selects the
   `salary_history` row in effect on the pay-period end date, mirroring the
   application's `findCurrentByEmployeeId(...)` logic.

6. **Business-rule calculations in-report** — daily rate (`hourly_rate × 8`),
   days worked (`hours ÷ 8`), benefit/deduction roll-ups, and gross/net
   reconciliation are computed in the view.

7. **Single source of truth** — official `gross_pay` / `total_deductions` /
   `net_pay` are read back from the stored `payroll_result`; the broken-out
   component columns explain *how* those totals are composed.

---

## 4. Reusability & Reporting Architecture

- **One view, many consumers** — `vw_employee_payslip_report` feeds the payslip
  PDF, the result snapshot, *and* the database tests, with no consumer needing
  to know the underlying joins.
- **Encapsulation** — report logic lives in the view definition, not duplicated
  across queries or screens.
- **Extensible** — new payslip fields are added by extending a subquery column,
  not by rewriting consumers; additional reports are added as new views.
- **Verified** — `employee_payslip_report_tests.sql` validates the reporting
  output (reconciliation, referential integrity, completeness) — see the
  Database Testing documentation.

---

## 5. Files

| File | Role |
|------|------|
| `employee_payslip_report_view.sql` | Payslip report view definition (+ usage queries) |
| `employee_payslip_report_result.sql` | Result / snapshot queries |
| `payroll_summary_monthly_view.sql` | Monthly payroll summary view |
| `employee_payslip_report_tests.sql` | Database tests for the payslip report |
| `reporting_catalog.md` | This document |

---

*Prepared by Group 5 / ByteBabes — MotorPH Payroll System, Database Reporting.*
