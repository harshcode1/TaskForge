-- Speeds up the board's per-project task list when it is filtered by status
-- (GET /api/tasks/project/{id}?status=...): the database can jump straight to
-- one project's tasks with that status instead of reading all of the project's
-- tasks and discarding the rest. Measured results are in the README
-- ("Technical Decisions") and backend/perf/results-*.txt.
--
-- The foreign-key index on project_id from V1 is left in place; this index
-- also covers lookups by project_id alone, so that older index is now
-- redundant and could be dropped in a later migration.

CREATE INDEX `idx_tasks_project_status` ON `tasks` (`project_id`, `status`);
