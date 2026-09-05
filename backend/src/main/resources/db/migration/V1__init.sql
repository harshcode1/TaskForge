-- Baseline schema, captured from the Hibernate-generated dev schema (ddl-auto=update)
-- so this migration matches entity mappings exactly (UUID -> binary(16), enum types, etc).
-- From here on, schema changes go through new versioned migrations, not ddl-auto.

CREATE TABLE `users` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` enum('ADMIN','MANAGER','MEMBER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `projects` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `owner_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKmueqy6cpcwpfl8gnnag4idjt9` (`owner_id`),
  CONSTRAINT `FKmueqy6cpcwpfl8gnnag4idjt9` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `tasks` (
  `id` binary(16) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `due_date` date DEFAULT NULL,
  `priority` enum('HIGH','LOW','MEDIUM') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('DONE','IN_PROGRESS','PENDING','TODO') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `assignee_id` binary(16) DEFAULT NULL,
  `project_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKekr1dgiqktpyoip3qmp6lxsit` (`assignee_id`),
  KEY `FKsfhn82y57i3k9uxww1s007acc` (`project_id`),
  CONSTRAINT `FKekr1dgiqktpyoip3qmp6lxsit` FOREIGN KEY (`assignee_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKsfhn82y57i3k9uxww1s007acc` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `project_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `joined_at` datetime(6) DEFAULT NULL,
  `role` enum('ADMIN','MANAGER','MEMBER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_id` binary(16) DEFAULT NULL,
  `user_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKdki1sp2homqsdcvqm9yrix31g` (`project_id`),
  KEY `FKgul2el0qjk5lsvig3wgajwm77` (`user_id`),
  CONSTRAINT `FKdki1sp2homqsdcvqm9yrix31g` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`),
  CONSTRAINT `FKgul2el0qjk5lsvig3wgajwm77` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comments` (
  `id` binary(16) NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime(6) DEFAULT NULL,
  `author_id` binary(16) DEFAULT NULL,
  `task_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKn2na60ukhs76ibtpt9burkm27` (`author_id`),
  KEY `FKi7pp0331nbiwd2844kg78kfwb` (`task_id`),
  CONSTRAINT `FKi7pp0331nbiwd2844kg78kfwb` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`),
  CONSTRAINT `FKn2na60ukhs76ibtpt9burkm27` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
