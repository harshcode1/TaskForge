package com.projectmgmttool.backend.repository;

import com.projectmgmttool.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByTaskId(UUID taskId);
    List<Comment> findByTaskIdOrderByCreatedAtDesc(UUID taskId);
}