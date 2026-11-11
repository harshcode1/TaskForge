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

import java.util.List;
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

    @Mock
    private ProjectAccessService access;

    private User author;
    private Project project;
    private Task task;
    private UUID taskId;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        author = new User();
        author.setId(UUID.randomUUID());
        author.setEmail("author@example.com");
        author.setRole(Role.MEMBER);

        User projectOwner = new User();
        projectOwner.setId(UUID.randomUUID());
        projectOwner.setEmail("owner@example.com");

        project = new Project();
        project.setId(projectId);
        project.setOwner(projectOwner);

        task = new Task();
        task.setId(taskId);
        task.setProject(project);
    }

    private CustomApiException notFound() {
        return new CustomApiException(ProjectAccessService.NOT_FOUND_MESSAGE, 404);
    }

    private Comment commentBy(User by, UUID commentId) {
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthor(by);
        comment.setTask(task);
        return comment;
    }

    // ---- addComment ----

    @Test
    void addComment_asMember_savesComment() {
        CommentRequest request = new CommentRequest();
        request.setTaskId(taskId);
        request.setText("Great progress!");

        Comment savedComment = new Comment();
        savedComment.setContent("Great progress!");
        savedComment.setAuthor(author);
        savedComment.setTask(task);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(access.requireMember(projectId, "author@example.com")).thenReturn(project);
        when(userRepository.findByEmail("author@example.com")).thenReturn(Optional.of(author));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        Comment result = commentService.addComment(request, "author@example.com");

        assertNotNull(result);
        assertEquals("Great progress!", result.getContent());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void addComment_accessCheckFails_errorPropagatesAndNothingIsSaved() {
        CommentRequest request = new CommentRequest();
        request.setTaskId(taskId);
        request.setText("Comment");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(access.requireMember(projectId, "stranger@example.com")).thenThrow(notFound());

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> commentService.addComment(request, "stranger@example.com"));

        assertEquals(404, ex.getErrorCode());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_taskNotFound_throwsTheSameNotFoundAsAnAccessFailure() {
        CommentRequest request = new CommentRequest();
        request.setTaskId(taskId);
        request.setText("Comment");

        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> commentService.addComment(request, "author@example.com"));

        assertEquals(404, ex.getErrorCode());
        assertEquals(ProjectAccessService.NOT_FOUND_MESSAGE, ex.getMessage());
    }

    // ---- getCommentsForTask ----

    @Test
    void getCommentsForTask_asMember_returnsComments() {
        List<Comment> comments = List.of(commentBy(author, UUID.randomUUID()));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(access.requireMember(projectId, "author@example.com")).thenReturn(project);
        when(commentRepository.findByTaskIdOrderByCreatedAtDesc(taskId)).thenReturn(comments);

        assertEquals(comments, commentService.getCommentsForTask(taskId, "author@example.com"));
    }

    @Test
    void getCommentsForTask_accessCheckFails_errorPropagatesAndNothingIsQueried() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(access.requireMember(projectId, "stranger@example.com")).thenThrow(notFound());

        assertThrows(CustomApiException.class,
                () -> commentService.getCommentsForTask(taskId, "stranger@example.com"));

        verifyNoInteractions(commentRepository);
    }

    @Test
    void getCommentsForTask_taskNotFound_throwsNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> commentService.getCommentsForTask(taskId, "author@example.com"));

        assertEquals(404, ex.getErrorCode());
    }

    // ---- deleteComment ----

    @Test
    void deleteComment_byAuthorWhoIsStillAMember_deletesSuccessfully() {
        UUID commentId = UUID.randomUUID();
        Comment comment = commentBy(author, commentId);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(access.requireMember(projectId, "author@example.com")).thenReturn(project);

        assertDoesNotThrow(() -> commentService.deleteComment(commentId, "author@example.com"));
        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteComment_byAnotherMember_throwsForbidden() {
        UUID commentId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(commentBy(author, commentId)));
        when(access.requireMember(projectId, "other@example.com")).thenReturn(project);

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> commentService.deleteComment(commentId, "other@example.com"));

        assertEquals(403, ex.getErrorCode());
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void deleteComment_authorNoLongerInProject_throwsNotFound() {
        UUID commentId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(commentBy(author, commentId)));
        when(access.requireMember(projectId, "author@example.com")).thenThrow(notFound());

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> commentService.deleteComment(commentId, "author@example.com"));

        assertEquals(404, ex.getErrorCode());
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void deleteComment_unknownComment_throwsNotFound() {
        UUID commentId = UUID.randomUUID();
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> commentService.deleteComment(commentId, "author@example.com"));

        assertEquals(404, ex.getErrorCode());
        assertEquals(ProjectAccessService.NOT_FOUND_MESSAGE, ex.getMessage());
    }
}
