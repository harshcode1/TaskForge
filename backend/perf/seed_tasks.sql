-- Performance experiment data. DEV / MEASUREMENT ONLY - never run against a real database.
-- Run against a throwaway MySQL 8 schema that already has the Flyway migrations applied:
--   docker exec -i taskforge-perf mysql -uroot -pperfroot perfdb < backend/perf/seed_tasks.sql
--
-- Creates: 100 users, 1,000 projects, 100,000 tasks.
--   * project "Perf project 1" is one BIG project holding 25,000 tasks
--   * the other 999 projects share the remaining 75,000 (about 75 each)
--   * status mix (by i % 20): TODO 40%, IN_PROGRESS 25%, PENDING 10%, DONE 25%

SET SESSION cte_max_recursion_depth = 200000;

INSERT INTO users (id, created_at, email, name, password, role)
WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 100)
SELECT UUID_TO_BIN(UUID()), NOW(6), CONCAT('perf-user-', i, '@example.com'),
       CONCAT('Perf User ', i), 'not-a-real-hash', 'MEMBER'
FROM n;

-- Lookup tables so the big insert can join by number instead of by name
CREATE TEMPORARY TABLE user_map (n INT PRIMARY KEY, id BINARY(16));
INSERT INTO user_map
SELECT ROW_NUMBER() OVER (ORDER BY email) - 1, id FROM users WHERE email LIKE 'perf-user-%';

INSERT INTO projects (id, created_at, description, name, owner_id)
WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 1000)
SELECT UUID_TO_BIN(UUID()), NOW(6), 'perf data', CONCAT('Perf project ', i),
       (SELECT id FROM user_map WHERE user_map.n = n.i % 100)
FROM n;

CREATE TEMPORARY TABLE project_map (n INT PRIMARY KEY, id BINARY(16));
INSERT INTO project_map
SELECT CAST(SUBSTRING(name, 14) AS UNSIGNED), id FROM projects WHERE name LIKE 'Perf project %';

INSERT INTO tasks (id, created_at, description, due_date, priority, status, title, assignee_id, project_id)
WITH RECURSIVE n(i) AS (SELECT 1 UNION ALL SELECT i + 1 FROM n WHERE i < 100000)
SELECT UUID_TO_BIN(UUID()),
       DATE_ADD('2026-01-01', INTERVAL n.i MINUTE),
       CONCAT('Generated description for perf task ', n.i),
       DATE_ADD('2026-01-01', INTERVAL n.i % 365 DAY),
       ELT(1 + n.i % 3, 'LOW', 'MEDIUM', 'HIGH'),
       CASE WHEN n.i % 20 < 8 THEN 'TODO'
            WHEN n.i % 20 < 13 THEN 'IN_PROGRESS'
            WHEN n.i % 20 < 15 THEN 'PENDING'
            ELSE 'DONE' END,
       CONCAT('Perf task ', n.i),
       (SELECT id FROM user_map WHERE user_map.n = n.i % 100),
       (SELECT id FROM project_map WHERE project_map.n = IF(n.i <= 25000, 1, 2 + n.i % 999))
FROM n;

ANALYZE TABLE users, projects, tasks;
