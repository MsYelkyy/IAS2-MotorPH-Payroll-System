package motorph.infrastructure.repository.report;


import motorph.application.dto.report.payroll.OrganizationPayrollReportDTO;
import motorph.application.dto.report.payroll.PayrollSummaryReportDTO;
import motorph.application.dto.report.payroll.PayslipReportDTO;
import motorph.infrastructure.config.DatabaseConnection;


import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


/**
* Handles payroll report data retrieval.
*/
public class PayrollReportRepository {


   // finds organization payroll report data using optional filters
   public List<OrganizationPayrollReportDTO> findOrganizationPayrollReport(
           Integer departmentId,
           Integer employeeId,
           Integer payrunId,
           LocalDate startDate,
           LocalDate endDate) {


       List<OrganizationPayrollReportDTO> reportRows = new ArrayList<>();


       StringBuilder sql = new StringBuilder("""
               SELECT pr.payroll_result_id,
                      e.employee_id,
                      CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
                      pos.position_name,
                      d.department_name,
                      p.pay_run_id,
                      p.start_date,
                      p.end_date,
                      p.pay_date,
                      pr.total_hours_worked,
                      pr.gross_pay,
                      gd.sss_number,
                      gd.philhealth_number,
                      gd.pagibig_number,
                      gd.tin_number,
                      COALESCE(pd.sss_deduction, 0) AS sss_deduction,
                      COALESCE(pd.philhealth_deduction, 0) AS philhealth_deduction,
                      COALESCE(pd.pagibig_deduction, 0) AS pagibig_deduction,
                      COALESCE(pd.withholding_tax, 0) AS withholding_tax,
                      pr.total_deductions,
                      pr.net_pay
               FROM payroll_result pr
               JOIN employee e
                 ON pr.employee_id = e.employee_id
               JOIN department d
                 ON e.department_id = d.department_id
               JOIN position pos
                 ON e.position_id = pos.position_id
               JOIN payrun p
                 ON pr.pay_run_id = p.pay_run_id
               LEFT JOIN government_details gd
                 ON e.employee_id = gd.employee_id
               LEFT JOIN (
                   SELECT pd.payroll_result_id,
                          SUM(CASE WHEN dt.deduction_name = 'SSS Contribution' THEN pd.amount ELSE 0 END) AS sss_deduction,
                          SUM(CASE WHEN dt.deduction_name = 'PhilHealth Contribution' THEN pd.amount ELSE 0 END) AS philhealth_deduction,
                          SUM(CASE WHEN dt.deduction_name = 'Pag-IBIG Contribution' THEN pd.amount ELSE 0 END) AS pagibig_deduction,
                          SUM(CASE WHEN dt.deduction_name = 'Withholding Tax' THEN pd.amount ELSE 0 END) AS withholding_tax
                   FROM payroll_deduction pd
                   JOIN deduction_type dt
                     ON pd.deduction_type_id = dt.deduction_type_id
                   GROUP BY pd.payroll_result_id
               ) pd
                 ON pr.payroll_result_id = pd.payroll_result_id
               WHERE EXISTS (
                   SELECT 1
                   FROM attendance a
                   WHERE a.employee_id = pr.employee_id
                     AND a.work_date BETWEEN p.start_date AND p.end_date
               )
               """);


       List<Object> parameters = new ArrayList<>();


       appendPayrollFilters(sql, parameters, departmentId, employeeId, payrunId, startDate, endDate);
       sql.append(" ORDER BY d.department_name, e.last_name, e.first_name, p.start_date");


       try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql.toString())) {


           setParameters(stmt, parameters);


           try (ResultSet rs = stmt.executeQuery()) {
               while (rs.next()) {
                   reportRows.add(mapOrganizationPayrollRow(rs));
               }
           }


       } catch (SQLException e) {
           throw new RuntimeException("Failed to find organization payroll report data.", e);
       }


       return reportRows;
   }


   // finds payroll summary report data using optional filters
   public List<PayrollSummaryReportDTO> findPayrollSummaryReport(
           Integer departmentId,
           Integer payrunId,
           LocalDate startDate,
           LocalDate endDate) {


       List<PayrollSummaryReportDTO> reportRows = new ArrayList<>();


       StringBuilder sql = new StringBuilder("""
               SELECT d.department_name,
                      COUNT(DISTINCT e.employee_id) AS employee_count,
                      COALESCE(SUM(pr.gross_pay), 0) AS gross_pay,
                      COALESCE(SUM(pb.total_benefits), 0) AS total_benefits,
                      COALESCE(SUM(po.total_overtime), 0) AS total_overtime,
                      COALESCE(SUM(pr.total_deductions), 0) AS total_deductions,
                      COALESCE(SUM(pr.net_pay), 0) AS net_pay
               FROM payroll_result pr
               JOIN employee e
                 ON pr.employee_id = e.employee_id
               JOIN department d
                 ON e.department_id = d.department_id
               JOIN payrun p
                 ON pr.pay_run_id = p.pay_run_id
               LEFT JOIN (
                   SELECT payroll_result_id,
                          SUM(amount) AS total_benefits
                   FROM payroll_benefit
                   GROUP BY payroll_result_id
               ) pb
                 ON pr.payroll_result_id = pb.payroll_result_id
               LEFT JOIN (
                   SELECT payroll_result_id,
                          SUM(amount) AS total_overtime
                   FROM payroll_overtime
                   GROUP BY payroll_result_id
               ) po
                 ON pr.payroll_result_id = po.payroll_result_id
               WHERE EXISTS (
                   SELECT 1
                   FROM attendance a
                   WHERE a.employee_id = pr.employee_id
                     AND a.work_date BETWEEN p.start_date AND p.end_date
               )
               """);


       List<Object> parameters = new ArrayList<>();


       appendPayrollFilters(sql, parameters, departmentId, null, payrunId, startDate, endDate);
       sql.append(" GROUP BY d.department_name");
       sql.append(" ORDER BY d.department_name");


       try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql.toString())) {


           setParameters(stmt, parameters);


           try (ResultSet rs = stmt.executeQuery()) {
               while (rs.next()) {
                   reportRows.add(mapPayrollSummaryRow(rs));
               }
           }


       } catch (SQLException e) {
           throw new RuntimeException("Failed to find payroll summary report data.", e);
       }


       return reportRows;
   }


   // finds payslip report data for one employee and payrun
   public PayslipReportDTO findPayslipReport(int employeeId, int payrunId) {
       String sql = """
           SELECT e.employee_id,
                 CONCAT(e.first_name, ' ', e.last_name) AS employee_name,
                 d.department_name,
                 pos.position_name,
                 gd.tin_number,
                 gd.sss_number,
                 gd.philhealth_number,
                 gd.pagibig_number,
                 p.pay_run_id,
                 p.start_date,
                 p.end_date,
                 p.pay_date,
                 pr.total_hours_worked,
                 COALESCE(pr.gross_pay, 0)
                     - COALESCE(pb.total_benefits, 0)
                     - COALESCE(po.total_overtime, 0) AS basic_pay,
                 COALESCE(pb.total_benefits, 0) AS total_benefits,
                 COALESCE(po.total_overtime, 0) AS total_overtime,
                 pr.gross_pay,
                 COALESCE(pd.sss_deduction, 0) AS sss_deduction,
                 COALESCE(pd.philhealth_deduction, 0) AS philhealth_deduction,
                 COALESCE(pd.pagibig_deduction, 0) AS pagibig_deduction,
                 COALESCE(pd.withholding_tax, 0) AS withholding_tax,
                 COALESCE(pd.other_deductions, 0) AS other_deductions,
                 pr.total_deductions,
                 pr.net_pay,
                 COALESCE(ytd.ytd_gross_pay, 0) AS ytd_gross_pay,
                 COALESCE(ytd.ytd_withholding_tax, 0) AS ytd_withholding_tax,
                 COALESCE(ytd.ytd_sss, 0) AS ytd_sss,
                 COALESCE(ytd.ytd_philhealth, 0) AS ytd_philhealth,
                 COALESCE(ytd.ytd_pagibig, 0) AS ytd_pagibig,
                 COALESCE(ytd.ytd_net_pay, 0) AS ytd_net_pay
           FROM payroll_result pr
           JOIN employee e
            ON pr.employee_id = e.employee_id
           JOIN department d
            ON e.department_id = d.department_id
           JOIN position pos
            ON e.position_id = pos.position_id
           JOIN payrun p
            ON pr.pay_run_id = p.pay_run_id
           JOIN salary_history sh
            ON e.employee_id = sh.employee_id
           AND p.start_date >= sh.start_date
           AND (sh.end_date IS NULL OR p.end_date <= sh.end_date)
           LEFT JOIN government_details gd
            ON e.employee_id = gd.employee_id
           LEFT JOIN (
              SELECT payroll_result_id,
                     SUM(amount) AS total_benefits
              FROM payroll_benefit
              GROUP BY payroll_result_id
           ) pb
            ON pr.payroll_result_id = pb.payroll_result_id
           LEFT JOIN (
              SELECT payroll_result_id,
                     SUM(amount) AS total_overtime
              FROM payroll_overtime
              GROUP BY payroll_result_id
           ) po
            ON pr.payroll_result_id = po.payroll_result_id
           LEFT JOIN (
              SELECT pd.payroll_result_id,
                     SUM(CASE WHEN dt.deduction_name = 'SSS Contribution' THEN pd.amount ELSE 0 END) AS sss_deduction,
                     SUM(CASE WHEN dt.deduction_name = 'PhilHealth Contribution' THEN pd.amount ELSE 0 END) AS philhealth_deduction,
                     SUM(CASE WHEN dt.deduction_name = 'Pag-IBIG Contribution' THEN pd.amount ELSE 0 END) AS pagibig_deduction,
                     SUM(CASE WHEN dt.deduction_name = 'Withholding Tax' THEN pd.amount ELSE 0 END) AS withholding_tax,
                     SUM(CASE
                             WHEN dt.deduction_name NOT IN (
                                 'SSS Contribution',
                                 'PhilHealth Contribution',
                                 'Pag-IBIG Contribution',
                                 'Withholding Tax'
                             )
                             THEN pd.amount
                             ELSE 0
                         END) AS other_deductions
              FROM payroll_deduction pd
              JOIN deduction_type dt
                ON pd.deduction_type_id = dt.deduction_type_id
              GROUP BY pd.payroll_result_id
           ) pd
            ON pr.payroll_result_id = pd.payroll_result_id
                          LEFT JOIN (
              SELECT pr2.employee_id,
                     MAX(sh2.basic_salary) * 12 AS ytd_gross_pay,
                     COALESCE(MAX(CASE WHEN dt2.deduction_name = 'Withholding Tax' THEN pd2.amount ELSE 0 END), 0) * 12 AS ytd_withholding_tax,
                     COALESCE(MAX(CASE WHEN dt2.deduction_name = 'SSS Contribution' THEN pd2.amount ELSE 0 END), 0) * 12 AS ytd_sss,
                     COALESCE(MAX(CASE WHEN dt2.deduction_name = 'PhilHealth Contribution' THEN pd2.amount ELSE 0 END), 0) * 12 AS ytd_philhealth,
                     COALESCE(MAX(CASE WHEN dt2.deduction_name = 'Pag-IBIG Contribution' THEN pd2.amount ELSE 0 END), 0) * 12 AS ytd_pagibig,
                     COALESCE(MAX(pr2.net_pay), 0) * 12 AS ytd_net_pay
              FROM payroll_result pr2
              JOIN payrun p2
                ON pr2.pay_run_id = p2.pay_run_id
              JOIN payrun selected_payrun
                ON selected_payrun.pay_run_id = ?
              JOIN salary_history sh2
                ON pr2.employee_id = sh2.employee_id
               AND p2.pay_date >= sh2.start_date
               AND (sh2.end_date IS NULL OR p2.pay_date <= sh2.end_date)
              LEFT JOIN payroll_deduction pd2
                ON pr2.payroll_result_id = pd2.payroll_result_id
              LEFT JOIN deduction_type dt2
                ON pd2.deduction_type_id = dt2.deduction_type_id
              WHERE YEAR(p2.pay_date) = YEAR(selected_payrun.pay_date)
                AND p2.pay_date <= selected_payrun.pay_date
              GROUP BY pr2.employee_id
                          ) ytd
            ON pr.employee_id = ytd.employee_id
           WHERE e.employee_id = ?
            AND p.pay_run_id = ?
           LIMIT 1
           """;


       try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {


           stmt.setInt(1, payrunId);
           stmt.setInt(2, employeeId);
           stmt.setInt(3, payrunId);


           try (ResultSet rs = stmt.executeQuery()) {
               if (rs.next()) {
                   return mapPayslipRow(rs);
               }
           }


       } catch (SQLException e) {
           throw new RuntimeException("Failed to find payslip report data.", e);
       }


       return null;
   }


   // adds common payroll filters
   private void appendPayrollFilters(StringBuilder sql,
                                     List<Object> parameters,
                                     Integer departmentId,
                                     Integer employeeId,
                                     Integer payrunId,
                                     LocalDate startDate,
                                     LocalDate endDate) {


       if (departmentId != null) {
           sql.append(" AND d.department_id = ?");
           parameters.add(departmentId);
       }


       if (employeeId != null) {
           sql.append(" AND e.employee_id = ?");
           parameters.add(employeeId);
       }


       if (payrunId != null) {
           sql.append(" AND p.pay_run_id = ?");
           parameters.add(payrunId);
       }


       if (startDate != null && endDate != null) {
           sql.append(" AND p.start_date >= ? AND p.end_date <= ?");
           parameters.add(Date.valueOf(startDate));
           parameters.add(Date.valueOf(endDate));
       }
   }


   // maps result set to organization payroll report dto
   private OrganizationPayrollReportDTO mapOrganizationPayrollRow(ResultSet rs) throws SQLException {
       return new OrganizationPayrollReportDTO(
               rs.getInt("payroll_result_id"),
               rs.getInt("employee_id"),
               rs.getString("employee_name"),
               rs.getString("position_name"),
               rs.getString("department_name"),
               rs.getInt("pay_run_id"),
               rs.getDate("start_date").toLocalDate(),
               rs.getDate("end_date").toLocalDate(),
               rs.getDate("pay_date").toLocalDate(),
               rs.getDouble("total_hours_worked"),
               rs.getDouble("gross_pay"),
               rs.getString("sss_number"),
               rs.getString("philhealth_number"),
               rs.getString("pagibig_number"),
               rs.getString("tin_number"),
               rs.getDouble("sss_deduction"),
               rs.getDouble("philhealth_deduction"),
               rs.getDouble("pagibig_deduction"),
               rs.getDouble("withholding_tax"),
               rs.getDouble("total_deductions"),
               rs.getDouble("net_pay")
       );
   }


   // maps result set to payroll summary report dto
   private PayrollSummaryReportDTO mapPayrollSummaryRow(ResultSet rs) throws SQLException {
       return new PayrollSummaryReportDTO(
               rs.getString("department_name"),
               rs.getInt("employee_count"),
               rs.getDouble("gross_pay"),
               rs.getDouble("total_benefits"),
               rs.getDouble("total_overtime"),
               rs.getDouble("total_deductions"),
               rs.getDouble("net_pay")
       );
   }


   // maps result set to payslip report dto
   private PayslipReportDTO mapPayslipRow(ResultSet rs) throws SQLException {
       return new PayslipReportDTO(
               rs.getInt("employee_id"),
               rs.getString("employee_name"),
               rs.getString("department_name"),
               rs.getString("position_name"),
               rs.getString("tin_number"),
               rs.getString("sss_number"),
               rs.getString("philhealth_number"),
               rs.getString("pagibig_number"),
               rs.getInt("pay_run_id"),
               rs.getDate("start_date").toLocalDate(),
               rs.getDate("end_date").toLocalDate(),
               rs.getDate("pay_date").toLocalDate(),
               rs.getDouble("total_hours_worked"),
               rs.getDouble("basic_pay"),
               rs.getDouble("total_benefits"),
               rs.getDouble("total_overtime"),
               rs.getDouble("gross_pay"),
               rs.getDouble("sss_deduction"),
               rs.getDouble("philhealth_deduction"),
               rs.getDouble("pagibig_deduction"),
               rs.getDouble("withholding_tax"),
               rs.getDouble("other_deductions"),
               rs.getDouble("total_deductions"),
               rs.getDouble("net_pay"),
               rs.getDouble("ytd_gross_pay"),
               rs.getDouble("ytd_withholding_tax"),
               rs.getDouble("ytd_sss"),
               rs.getDouble("ytd_philhealth"),
               rs.getDouble("ytd_pagibig"),
               rs.getDouble("ytd_net_pay")
       );
   }


   // sets prepared statement parameters
   private void setParameters(PreparedStatement stmt, List<Object> parameters) throws SQLException {
       for (int i = 0; i < parameters.size(); i++) {
           Object value = parameters.get(i);


           if (value instanceof Integer intValue) {
               stmt.setInt(i + 1, intValue);
           } else if (value instanceof Date dateValue) {
               stmt.setDate(i + 1, dateValue);
           } else {
               stmt.setObject(i + 1, value);
           }
       }
   }
}



