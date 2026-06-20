package com.projectmgmttool.backend.repository;


import com.projectmgmttool.backend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByOwnerId(UUID ownerId);

    @Query("SELECT p FROM Project p " +
           "WHERE p.owner.id = :userId " +
           "OR p.id IN (SELECT pm.project.id FROM ProjectMember pm WHERE pm.user.id = :userId)")
    List<Project> findAllProjectsVisibleToUser(@Param("userId") UUID userId);
}
