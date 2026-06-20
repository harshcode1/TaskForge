package com.projectmgmttool.backend.repository;


import com.projectmgmttool.backend.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findByProjectId(UUID projectId);
    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);
    boolean existsByProjectIdAndUserEmail(UUID projectId, String email);
    Optional<ProjectMember> findByProjectIdAndUserEmail(UUID projectId, String email);
    List<ProjectMember> findByUserEmail(String email);
}
