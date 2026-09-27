-- MySQL 8+, run ONCE against the original Loot schema after taking a backup.
-- Stop the old application before migration. Hibernate ddl-auto is validate by default.
-- For a new empty development schema use DDL_AUTO=update once instead of this ALTER script.

ALTER TABLE `user`
  ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'USER',
  ADD COLUMN reset_code_hash VARCHAR(255) NULL,
  ADD COLUMN reset_expires_at DATETIME(6) NULL,
  ADD COLUMN reset_attempts INT NOT NULL DEFAULT 0,
  ADD COLUMN auth_version INT NOT NULL DEFAULT 0;

ALTER TABLE system_recipe ADD COLUMN image_url VARCHAR(2048) NULL;
ALTER TABLE user_recipe ADD COLUMN image_url VARCHAR(2048) NULL;
ALTER TABLE pantry_item ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cooking_history MODIFY COLUMN description VARCHAR(500) NULL,
  MODIFY COLUMN instructions TEXT NOT NULL;

-- Destroy legacy plaintext passwords rather than allowing insecure fallback login.
-- Affected users must use the email reset flow (configure SMTP first), or recreate
-- disposable development accounts. This statement never prints the old values.
UPDATE `user` SET password = '!RESET_REQUIRED!', auth_version = auth_version + 1
WHERE NOT (LENGTH(password) = 60 AND
  (password LIKE '$2a$%' OR password LIKE '$2b$%' OR password LIKE '$2y$%'));

-- Review duplicate emails ignoring case before normalizing old account addresses:
-- SELECT LOWER(TRIM(email)), COUNT(*) FROM `user` GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1;
-- After resolving any duplicates: UPDATE `user` SET email = LOWER(TRIM(email));

-- Promote a specific existing account manually, never via registration:
-- UPDATE `user` SET role='ADMIN', auth_version=auth_version+1 WHERE email='your-admin@example.com';
