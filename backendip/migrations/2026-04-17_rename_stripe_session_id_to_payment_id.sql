-- Turf Explorer migration: rename transactions.stripe_session_id to payment_id
-- Safe for existing databases: runs only when old column exists and new column does not.

SET @db_name = COALESCE(DATABASE(), 'turf_explorer');

SET @should_rename = (
  SELECT CASE
    WHEN EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'transactions'
        AND column_name = 'stripe_session_id'
    )
    AND NOT EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'transactions'
        AND column_name = 'payment_id'
    )
    THEN 1
    ELSE 0
  END
);

SET @sql_rename = IF(
  @should_rename = 1,
  CONCAT(
    'ALTER TABLE `', @db_name, '`.`transactions` ',
    'CHANGE COLUMN `stripe_session_id` `payment_id` VARCHAR(255) NOT NULL UNIQUE'
  ),
  'SELECT ''No rename needed'' AS info'
);

PREPARE stmt FROM @sql_rename;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
