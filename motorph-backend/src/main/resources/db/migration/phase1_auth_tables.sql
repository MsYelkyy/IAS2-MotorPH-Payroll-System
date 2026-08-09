-- Phase 1 backend migration: tables needed for the JWT + revocation + OTP-challenge auth scheme.
-- Run manually against payrollsystem_db (no migration framework wired up yet - see plan Phase 1).

CREATE TABLE IF NOT EXISTS login_challenge (
    challenge_id     VARCHAR(64) NOT NULL,
    user_account_id  INT NOT NULL,
    expires_at       DATETIME NOT NULL,
    consumed         TINYINT(1) NOT NULL DEFAULT 0,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (challenge_id),
    KEY idx_login_challenge_user_account (user_account_id),
    CONSTRAINT fk_login_challenge_user_account
        FOREIGN KEY (user_account_id) REFERENCES user_account (user_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS token_revocation (
    jti              VARCHAR(64) NOT NULL,
    user_account_id  INT NOT NULL,
    expires_at       DATETIME NOT NULL,
    revoked_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (jti),
    KEY idx_token_revocation_user_account (user_account_id),
    CONSTRAINT fk_token_revocation_user_account
        FOREIGN KEY (user_account_id) REFERENCES user_account (user_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
