ALTER TABLE `user_account`
  ADD COLUMN `failed_login_count` int NOT NULL DEFAULT '0';
