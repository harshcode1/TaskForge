package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.entity.Comment;
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

    @Autowired
    private ProjectAccessService projectAccessService;

    public Comment addComment(CommentRequest request, String authorEmail) {
        Task task = findTask(request.getTaskId());
        projectAccessService.requireMember(task.getProject().getId(), authorEmail);

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
        Task task = findTask(taskId);
        projectAccessService.requireMember(task.getProject().getId(), userEmail);

        return commentRepository.findByTaskIdOrderByCreatedAtDesc(taskId);
    }

    public void deleteComment(UUID commentId, String userEmail) {
        // A missing comment and a comment in someone else's project give the same 404.
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(ProjectAccessService::notFound);

        // You must still be in the project (a removed member loses access to their old comments)...
        projectAccessService.requireMember(comment.getTask().getProject().getId(), userEmail);

        // ...and only the author can delete a comment.
        if (!comment.getAuthor().getEmail().equals(userEmail)) {
            throw new CustomApiException("You are not authorized to delete this comment", 403);
        }

        commentRepository.delete(comment);
    }

    private Task findTask(UUID taskId) {
        return taskRepository.findById(taskId).orElseThrow(ProjectAccessService::notFound);
    }
}
