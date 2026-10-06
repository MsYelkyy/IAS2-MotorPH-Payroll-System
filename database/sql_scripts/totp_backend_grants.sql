-- Gives the backend database account the rights the authenticator (TOTP) tables need.
-- Run it once after totp_authentication.sql on a database whose volume already existed, because the Docker
-- account setup only grants rights on tables that exist when it runs. Safe to run more than once.
--
-- Run it as the MySQL root account. It finds every host the backend account exists for, so you do not have to
-- know it. If MOTORPH_DB_USER in .env is not motorph_backend, change the name below first.
--
-- Windows Command Prompt, from the repository folder:
--   docker exec -i motorph-mysql sh -c "MYSQL_PWD=\"$MYSQL_ROOT_PASSWORD\" mysql -uroot" < database\sql_scripts\totp_backend_grants.sql
--
-- SELECT and INSERT are already granted on the whole database. What the backend does to these tables:
--   user_totp_credential    UPDATE  records the last accepted time step (stops a code being reused)
--                           DELETE  an administrator resetting a user's authenticator
--   password_recovery_token UPDATE  uses up a reset token and cancels a user's unused tokens
-- The backend never deletes recovery tokens, so DELETE is not granted on that table.

USE payrollsystem_db;

DROP PROCEDURE IF EXISTS motorph_grant_totp_tables;

DELIMITER //

CREATE PROCEDURE motorph_grant_totp_tables()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE account_host VARCHAR(255);
    DECLARE hosts CURSOR FOR SELECT host FROM mysql.user WHERE user = 'motorph_backend';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN hosts;

    host_loop: LOOP
        FETCH hosts INTO account_host;

        IF done = 1 THEN
            LEAVE host_loop;
        END IF;

        SET @motorph_grant = CONCAT(
            'GRANT UPDATE, DELETE ON payrollsystem_db.user_totp_credential TO ''motorph_backend''@', QUOTE(account_host));
        PREPARE grant_statement FROM @motorph_grant;
        EXECUTE grant_statement;
        DEALLOCATE PREPARE grant_statement;

        SET @motorph_grant = CONCAT(
            'GRANT UPDATE ON payrollsystem_db.password_recovery_token TO ''motorph_backend''@', QUOTE(account_host));
        PREPARE grant_statement FROM @motorph_grant;
        EXECUTE grant_statement;
        DEALLOCATE PREPARE grant_statement;
    END LOOP;

    CLOSE hosts;
END//

DELIMITER ;

CALL motorph_grant_totp_tables();
DROP PROCEDURE motorph_grant_totp_tables;
FLUSH PRIVILEGES;
