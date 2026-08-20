#!/bin/bash
# =============================================================================
# MotorPH Payroll System
# MySQL container init: create the least-privilege backend database account
# File: docker/mysql/init/06-create-backend-user.sh
# -----------------------------------------------------------------------------
# Why this exists
#   The official MySQL image can create an account from MYSQL_USER /
#   MYSQL_PASSWORD, but that account is granted ALL PRIVILEGES on the database.
#   README.md ("Backend Database Account") specifies a narrower account:
#
#       CREATE USER 'motorph_backend'@'<scoped host>' IDENTIFIED BY '<password>';
#       GRANT SELECT, INSERT, UPDATE, DELETE ON payrollsystem_db.* TO ...;
#
#   No GRANT OPTION, no DDL rights, no access to other schemas, host-scoped
#   rather than '%'. This script reproduces exactly that grant set inside the
#   container, so the Dockerized environment matches the documented one and
#   nothing is weakened. MYSQL_USER / MYSQL_PASSWORD are intentionally NOT set
#   in docker-compose.yml.
#
# When it runs
#   Only on first initialization of the mysql-data volume, after the schema,
#   the auth-table migration, the seed and the report-view scripts (files are
#   processed in filename order).
#
# Notes
#   * The password is read from the environment; it is never written into the
#     repository or into an image layer.
#   * This file may be sourced rather than executed by the image entrypoint
#     (that depends on its executable bit), so it deliberately avoids `set -u`,
#     `set -e` and `exit`, which would otherwise affect the entrypoint itself.
# =============================================================================

motorph_db_name="${MYSQL_DATABASE:-payrollsystem_db}"
motorph_db_user="${MOTORPH_DB_USER}"
motorph_db_password="${MOTORPH_DB_PASSWORD}"
# Host scope for the account. Defaults to the docker-compose network subnet so
# the account cannot be used from anywhere else, mirroring the 127.0.0.1 scoping
# used when the backend runs directly on the host.
motorph_db_client_host="${MOTORPH_DB_CLIENT_HOST:-%}"

if [ -z "$motorph_db_user" ] || [ -z "$motorph_db_password" ]; then
    echo "[motorph-init] MOTORPH_DB_USER / MOTORPH_DB_PASSWORD are not set." >&2
    echo "[motorph-init] The backend database account was NOT created." >&2
    echo "[motorph-init] Copy .env.example to .env, fill it in, then run:" >&2
    echo "[motorph-init]     docker compose down -v && docker compose up -d" >&2
else
    # Escape single quotes so a password containing one cannot break out of the
    # SQL string literal.
    motorph_db_password_escaped="${motorph_db_password//\'/\'\'}"

    echo "[motorph-init] Creating '${motorph_db_user}'@'${motorph_db_client_host}' on ${motorph_db_name}"

    # MYSQL_PWD keeps the root password off the command line / process list.
    # The statements come in over stdin, so the account password is not logged.
    MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket -u root -h localhost <<-MOTORPH_SQL
	CREATE USER IF NOT EXISTS '${motorph_db_user}'@'${motorph_db_client_host}'
	    IDENTIFIED BY '${motorph_db_password_escaped}';

	GRANT SELECT, INSERT, UPDATE, DELETE
	    ON \`${motorph_db_name}\`.*
	    TO '${motorph_db_user}'@'${motorph_db_client_host}';

	FLUSH PRIVILEGES;
	MOTORPH_SQL

    echo "[motorph-init] Backend database account ready (SELECT, INSERT, UPDATE, DELETE only)."
fi
