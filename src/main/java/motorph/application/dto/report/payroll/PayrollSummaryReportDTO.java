package motorph.application.dto.report.payroll;


/**
* Stores payroll summary report data.
*/
public class PayrollSummaryReportDTO {


   private final String departmentName;
   private final int employeeCount;
   private final double grossPay;
   private final double totalBenefits;
   private final double totalOvertime;
   private final double totalDeductions;
   private final double netPay;


   // constructors
   public PayrollSummaryReportDTO(String departmentName,
                                  int employeeCount,
                                  double grossPay,
                                  double totalBenefits,
                                  double totalOvertime,
                                  double totalDeductions,
                                  double netPay) {


       this.departmentName = departmentName;
       this.employeeCount = employeeCount;
       this.grossPay = grossPay;
       this.totalBenefits = totalBenefits;
       this.totalOvertime = totalOvertime;
       this.totalDeductions = totalDeductions;
       this.netPay = netPay;
   }


   // getters
   public String getDepartmentName() {
       return departmentName;
   }


   public int getEmployeeCount() {
       return employeeCount;
   }


   public double getGrossPay() {
       return grossPay;
   }


   public double getTotalBenefits() {
       return totalBenefits;
   }


   public double getTotalOvertime() {
       return totalOvertime;
   }


   public double getTotalDeductions() {
       return totalDeductions;
   }


   public double getNetPay() {
       return netPay;
   }
}



