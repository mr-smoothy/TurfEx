-- Turf Explorer migration: add turf-level availability and remove slot-level status
-- Safe for existing databases: uses metadata checks before each DDL operation.
-- Works even when no default DB is selected.

-- Target schema: current DB, or fallback to turf_explorer.
SET @db_name = COALESCE(DATABASE(), 'turf_explorer');

-- Validate target schema exists.
SELECT IF(
  EXISTS (
    SELECT 1
    FROM information_schema.schemata
    WHERE schema_name = @db_name
  ),
  CONCAT('Using database: ', @db_name),
  CONCAT('Database not found: ', @db_name, '. Create it first or set @db_name manually.')
) AS migration_context;

-- 1) Add turfs.available if it does not exist.
SET @sql_add_available = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'turfs'
        AND column_name = 'available'
    ),
    'SELECT ''turfs.available already exists'' AS info',
    CONCAT('ALTER TABLE `', @db_name, '`.`turfs` ADD COLUMN available TINYINT(1) NOT NULL DEFAULT 1 AFTER owner_id')
  )
);
PREPARE stmt FROM @sql_add_available;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) Backfill any unexpected NULL values to true.
SET @sql_backfill_available = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'turfs'
        AND column_name = 'available'
    ),
    CONCAT('UPDATE `', @db_name, '`.`turfs` SET available = 1 WHERE available IS NULL'),
    'SELECT ''Skipped backfill: turfs.available is missing'' AS info'
  )
);
PREPARE stmt FROM @sql_backfill_available;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) Normalize column definition to NOT NULL DEFAULT true.
SET @sql_normalize_available = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'turfs'
        AND column_name = 'available'
    ),
    CONCAT('ALTER TABLE `', @db_name, '`.`turfs` MODIFY COLUMN available TINYINT(1) NOT NULL DEFAULT 1'),
    'SELECT ''Skipped normalize: turfs.available is missing'' AS info'
  )
);
PREPARE stmt FROM @sql_normalize_available;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) Drop legacy slots.status index if present.
SET @sql_drop_slots_status_index = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.statistics
      WHERE table_schema = @db_name
        AND table_name = 'slots'
        AND index_name = 'idx_status'
    ),
    CONCAT('DROP INDEX idx_status ON `', @db_name, '`.`slots`'),
    'SELECT ''slots.idx_status not present'' AS info'
  )
);
PREPARE stmt FROM @sql_drop_slots_status_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5) Drop legacy slots.status column if present.
SET @sql_drop_slots_status_column = (
  SELECT IF(
    EXISTS (
      SELECT 1
      FROM information_schema.columns
      WHERE table_schema = @db_name
        AND table_name = 'slots'
        AND column_name = 'status'
    ),
    CONCAT('ALTER TABLE `', @db_name, '`.`slots` DROP COLUMN status'),
    'SELECT ''slots.status already removed'' AS info'
  )
);
PREPARE stmt FROM @sql_drop_slots_status_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6) Verification output.
SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = @db_name
  AND table_name = 'turfs'
  AND column_name = 'available';

SELECT column_name
FROM information_schema.columns
WHERE table_schema = @db_name
  AND table_name = 'slots'
  AND column_name = 'status';
