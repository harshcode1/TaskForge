package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.dto.CommentRequest;
import com.projectmgmttool.backend.entity.*;
import com.projectmgmttool.backend.entity.enums.Role;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.CommentRepository;
import com.projectmgmttool.backend.repository.TaskRepository;
import com.projectmgmttool.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    private User author;
    private Task task;
    private UUID taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();

        author = new User();
        author.setId(UUID.randomUUID());
        author.setEmail("author@example.com");
        author.setRole(Role.MEMBER);

        User projectOwner = new User();
        projectOwner.setId(UUID.randomUUID());
        projectOwner.setEmail("owner@example.com");

        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setOwner(projectOwner);

        task = new Task();
        task.setId(taskId);
        task.setProject(project);
    }

    @Test
    void addComment_validRequest_savesComment() {
        CommentRequest request = new CommentRequest();
        request.setTaskId(taskId);
        request.setText("Great progress!");

        Comment savedComment = new Comment();
        savedComment.setContent("Great progress!");
        savedComment.setAuthor(author);
        savedComment.setTask(task);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail("author@example.com")).thenReturn(Optional.of(author));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        Comment result = commentService.addComment(request, "author@example.com");

        assertNotNull(result);
        assertEquals("Great progress!", result.getContent());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void addComment_taskNotFound_throwsException() {
        CommentRequest request = new CommentRequest();
        request.setTaskId(taskId);
        request.setText("Comment");

        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(CustomApiException.class,
                () -> commentService.addComment(request, "author@example.com"));
    }

    @Test
    void deleteComment_byAuthor_deletesSuccessfully() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthor(author);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertDoesNotThrow(() -> commentService.deleteComment(commentId, "author@example.com"));
        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_byNonAuthor_throwsForbidden() {
        UUID commentId = UUID.randomUUID();
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthor(author);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> commentService.deleteComment(commentId, "other@example.com"));
        assertEquals(403, ex.getErrorCode());
    }
}
