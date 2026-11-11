-- Drops the single-column index on tasks.project_id that V1 created for the foreign key.
-- idx_tasks_project_status (project_id, status) from V4 starts with project_id, so it
-- serves the foreign key and every lookup by project_id alone; the old index only costs
-- extra disk space and extra work on every task insert, update and delete.
--
-- The foreign key constraint itself is NOT dropped, only the index it no longer needs.
-- The old index has the name Hibernate generated when V1 was captured. The guard below
-- makes this a no-op on a database that does not have an index with that name.

SET @old_index := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'tasks'
    AND index_name = 'FKsfhn82y57i3k9uxww1s007acc'
);
SET @drop_sql := IF(@old_index > 0, 'DROP INDEX `FKsfhn82y57i3k9uxww1s007acc` ON `tasks`', 'DO 0');
PREPARE drop_stmt FROM @drop_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;
