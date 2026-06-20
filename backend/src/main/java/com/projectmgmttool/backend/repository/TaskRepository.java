package com.projectmgmttool.backend.repository;

import com.projectmgmttool.backend.entity.Task;
import com.projectmgmttool.backend.entity.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByProjectId(UUID projectId);
    List<Task> findByProjectIdAndStatus(UUID projectId, String status);
    List<Task> findByAssigneeEmail(String email);
    List<Task> findByDueDateBetweenAndAssigneeEmail(LocalDate start, LocalDate end, String email);

    @Query("SELECT t FROM Task t WHERE t.dueDate BETWEEN :startDate AND :endDate AND t.status IN :statuses")
    List<Task> findByDueDateBetweenAndStatusIn(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("statuses") List<TaskStatus> statuses);

    @Query("SELECT t FROM Task t WHERE t.assignee.id = :userId")
    List<Task> findTasksAssignedToUser(@Param("userId") UUID userId);
}
