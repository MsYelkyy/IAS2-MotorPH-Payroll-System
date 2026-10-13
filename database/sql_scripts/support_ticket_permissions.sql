-- Adds the support ticket permissions and grants them to the roles that handle tickets.
-- Only adds rows: existing permissions, roles and grants are not changed or removed.
-- New databases get the same rows from payrollsystem_db.sql. Safe to run more than once.
--
--   VIEW_TICKETS      ADMIN, HR, FINANCE, IT   view and count tickets routed to the caller's team
--   RESOLVE_TICKETS   ADMIN, HR, FINANCE, IT   resolve open tickets routed to the caller's team
--   CANCEL_TICKETS    ADMIN                    cancel open tickets
--   VIEW_ALL_TICKETS  ADMIN                    view and resolve tickets from every team

USE payrollsystem_db;

INSERT INTO permission (permission_key, module, action, record_type, description)
SELECT 'VIEW_TICKETS', 'Support Tickets', 'VIEW', 'support_ticket',
       'Allows user to view and count support tickets routed to their own team.'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE permission_key = 'VIEW_TICKETS');

INSERT INTO permission (permission_key, module, action, record_type, description)
SELECT 'RESOLVE_TICKETS', 'Support Tickets', 'RESOLVE', 'support_ticket',
       'Allows user to resolve open support tickets routed to their own team.'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE permission_key = 'RESOLVE_TICKETS');

INSERT INTO permission (permission_key, module, action, record_type, description)
SELECT 'CANCEL_TICKETS', 'Support Tickets', 'CANCEL', 'support_ticket',
       'Allows user to cancel open support tickets.'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE permission_key = 'CANCEL_TICKETS');

INSERT INTO permission (permission_key, module, action, record_type, description)
SELECT 'VIEW_ALL_TICKETS', 'Support Tickets', 'VIEW_ALL', 'support_ticket',
       'Allows user to view and resolve support tickets from every team.'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE permission_key = 'VIEW_ALL_TICKETS');

INSERT INTO role_permission (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
JOIN permission p
  ON (r.role_name = 'ADMIN'
          AND p.permission_key IN ('VIEW_TICKETS', 'RESOLVE_TICKETS', 'CANCEL_TICKETS', 'VIEW_ALL_TICKETS'))
  OR (r.role_name IN ('HR', 'FINANCE', 'IT')
          AND p.permission_key IN ('VIEW_TICKETS', 'RESOLVE_TICKETS'))
WHERE NOT EXISTS (
    SELECT 1
    FROM role_permission rp
    WHERE rp.role_id = r.role_id
      AND rp.permission_id = p.permission_id
);
