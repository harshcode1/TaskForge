package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.entity.Comment;
import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.Task;
import com.projectmgmttool.backend.entity.User;
import com.projectmgmttool.backend.dto.CommentRequest;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.CommentRepository;
import com.projectmgmttool.backend.repository.TaskRepository;
import com.projectmgmttool.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    public Comment addComment(CommentRequest request, String authorEmail) {
        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new CustomApiException("Task not found", 404));

        User user = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new CustomApiException("User not found", 404));

        Comment comment = new Comment();
        comment.setContent(request.getText());
        comment.setTask(task);
        comment.setAuthor(user);
        comment.setCreatedAt(LocalDateTime.now());

        return commentRepository.save(comment);
    }

    public List<Comment> getCommentsForTask(UUID taskId, String userEmail) {
        // First check if the task exists
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomApiException("Task not found", 404));

        // Verify user is authorized to view comments (must be a member of the project)
        Project project = task.getProject();
        boolean isOwner = project.getOwner().getEmail().equals(userEmail);

        // Check if user is a project member
        if (!isOwner) {
            // Only project members can view comments
            boolean isMember = userRepository.findByEmail(userEmail)
                    .map(user -> project.getMembers().stream()
                            .anyMatch(member -> member.getUser().getId().equals(user.getId())))
                    .orElse(false);

            if (!isMember) {
                throw new CustomApiException("You are not authorized to view comments for this task", 403);
            }
        }

        return commentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

    public void deleteComment(UUID commentId, String userEmail) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomApiException("Comment not found", 404));

        // Only the author of the comment can delete it
        if (!comment.getAuthor().getEmail().equals(userEmail)) {
            throw new CustomApiException("You are not authorized to delete this comment", 403);
        }

        commentRepository.delete(comment);
    }
}