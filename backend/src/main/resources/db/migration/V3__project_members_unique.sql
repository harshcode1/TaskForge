-- A user can be a member of a project only once, and project-level roles are
-- MANAGER or MEMBER only (the owner is projects.owner_id, not a row here).
-- Clean up existing data first so the unique key can be created.

-- 1. ADMIN is no longer a valid project role: demote to MANAGER.
--    (The enum column still lists 'ADMIN' - it is shared in shape with users.role -
--    but the application no longer writes it here.)
UPDATE `project_members` SET `role` = 'MANAGER' WHERE `role` = 'ADMIN';

-- 2. Keep the oldest row of any duplicate (project_id, user_id) pair.
DELETE pm1 FROM `project_members` pm1
JOIN `project_members` pm2
  ON pm1.`project_id` = pm2.`project_id`
 AND pm1.`user_id` = pm2.`user_id`
 AND pm1.`id` > pm2.`id`;

-- 3. Enforce it from now on.
ALTER TABLE `project_members`
  ADD CONSTRAINT `uk_project_members_project_user` UNIQUE (`project_id`, `user_id`);
