-- Persisted activity feed / audit log (see entity/ActivityLog.java). project_id
-- and task_id are deliberately plain columns, not foreign keys — a deleted
-- task's history should survive the delete rather than cascading away.

CREATE TABLE `activity_logs` (
  `id` binary(16) NOT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `task_id` binary(16) DEFAULT NULL,
  `task_title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` binary(16) DEFAULT NULL,
  `actor_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `actor_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_activity_logs_project_created` (`project_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
