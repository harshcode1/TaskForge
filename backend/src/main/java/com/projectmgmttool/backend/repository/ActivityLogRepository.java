package com.projectmgmttool.backend.repository;

import com.projectmgmttool.backend.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
    List<ActivityLog> findTop50ByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
