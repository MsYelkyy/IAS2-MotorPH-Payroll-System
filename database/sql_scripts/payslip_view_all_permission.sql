-- Adds the VIEW_ALL_PAYSLIPS permission and grants it to ADMIN, FINANCE and HR.
-- Only adds rows: existing permissions, roles and grants are not changed or removed.
-- New databases get the same rows from payrollsystem_db.sql. Safe to run more than once.
--
--   VIEW_PAYSLIP       every role           view the caller's own payslips (unchanged)
--   VIEW_ALL_PAYSLIPS  ADMIN, FINANCE, HR   view the payslips of other employees

USE payrollsystem_db;

INSERT INTO permission (permission_key, module, action, record_type, description)
SELECT 'VIEW_ALL_PAYSLIPS', 'Payroll', 'VIEW_ALL', 'payslip',
       'Allows user to view the payslips of other employees.'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE permission_key = 'VIEW_ALL_PAYSLIPS');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
JOIN permission p
  ON p.permission_key = 'VIEW_ALL_PAYSLIPS'
WHERE r.role_name IN ('ADMIN', 'FINANCE', 'HR')
  AND NOT EXISTS (
    SELECT 1
    FROM role_permission rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);
