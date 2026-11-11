package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.dto.TaskRequest;
import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.Task;
import com.projectmgmttool.backend.entity.User;
import com.projectmgmttool.backend.entity.enums.Priority;
import com.projectmgmttool.backend.entity.enums.Role;
import com.projectmgmttool.backend.entity.enums.TaskStatus;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.ProjectMemberRepository;
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
class TaskServiceTest {

    @InjectMocks
    private TaskService taskService;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectAccessService access;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    private User owner;
    private User outsider;
    private Project project;
    private Task task;
    private UUID projectId;
    private UUID taskId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        taskId = UUID.randomUUID();

        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@example.com");
        owner.setName("Owner");
        owner.setRole(Role.MANAGER);

        outsider = new User();
        outsider.setId(UUID.randomUUID());
        outsider.setEmail("outsider@example.com");

        project = new Project();
        project.setId(projectId);
        project.setName("Test Project");
        project.setOwner(owner);

        task = new Task();
        task.setId(taskId);
        task.setTitle("Test Task");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(Priority.MEDIUM);
        task.setProject(project);
        task.setAssignee(owner);
    }

    private TaskRequest requestFor(String assigneeEmail) {
        TaskRequest request = new TaskRequest();
        request.setProjectId(projectId);
        request.setTitle("New Task");
        request.setDescription("Description");
        request.setStatus("TODO");
        request.setPriority(Priority.HIGH);
        request.setAssigneeEmail(assigneeEmail);
        return request;
    }

    private CustomApiException notFound() {
        return new CustomApiException("Not found", 404);
    }

    // ---- createTask ----

    @Test
    void createTask_asMember_savesTask() {
        when(access.requireMember(projectId, "owner@example.com")).thenReturn(project);
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Task result = taskService.createTask(requestFor(null), "owner@example.com");

        assertNotNull(result);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_accessCheckFails_errorPropagatesAndNothingIsSaved() {
        when(access.requireMember(projectId, "outsider@example.com")).thenThrow(notFound());

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> taskService.createTask(requestFor(null), "outsider@example.com"));

        assertEquals(404, ex.getErrorCode());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_withoutProjectId_throwsBadRequest() {
        TaskRequest request = requestFor(null);
        request.setProjectId(null);

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> taskService.createTask(request, "owner@example.com"));

        assertEquals(400, ex.getErrorCode());
    }

    @Test
    void createTask_assigneeIsTheOwner_isAllowed() {
        when(access.requireMember(projectId, "member@example.com")).thenReturn(project);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        Task result = taskService.createTask(requestFor("owner@example.com"), "member@example.com");

        assertSame(owner, result.getAssignee());
    }

    @Test
    void createTask_assigneeIsAMember_isAllowed() {
        User member = new User();
        member.setId(UUID.randomUUID());
        member.setEmail("member@example.com");
        when(access.requireMember(projectId, "owner@example.com")).thenReturn(project);
        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, member.getId())).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        Task result = taskService.createTask(requestFor("member@example.com"), "owner@example.com");

        assertSame(member, result.getAssignee());
    }

    @Test
    void createTask_assigneeNotInProject_throwsBadRequest() {
        when(access.requireMember(projectId, "owner@example.com")).thenReturn(project);
        when(userRepository.findByEmail("outsider@example.com")).thenReturn(Optional.of(outsider));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, outsider.getId())).thenReturn(false);

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> taskService.createTask(requestFor("outsider@example.com"), "owner@example.com"));

        assertEquals(400, ex.getErrorCode());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_assigneeDoesNotExist_throwsNotFound() {
        when(access.requireMember(projectId, "owner@example.com")).thenReturn(project);
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> taskService.createTask(requestFor("ghost@example.com"), "owner@example.com"));

        assertEquals(404, ex.getErrorCode());
    }

    // ---- getTasksForProject ----

    @Test
    void getTasksForProject_asMember_noFilter_returnsAllTasks() {
        when(access.requireMember(projectId, "owner@example.com")).thenReturn(project);
        when(taskRepository.findByProjectId(projectId)).thenReturn(List.of(task));

        List<Task> result = taskService.getTasksForProject(projectId, Optional.empty(), "owner@example.com");

        assertEquals(1, result.size());
    }

    @Test
    void getTasksForProject_withStatus_queriesByThatStatus() {
        when(access.requireMember(projectId, "owner@example.com")).thenReturn(project);
        when(taskRepository.findByProjectIdAndStatus(projectId, TaskStatus.DONE)).thenReturn(List.of(task));

        List<Task> result = taskService.getTasksForProject(projectId, Optional.of("DONE"), "owner@example.com");

        assertEquals(1, result.size());
        verify(taskRepository, never()).findByProjectId(any());
    }

    @Test
    void getTasksForProject_withBlankStatus_behavesLikeNoFilter() {
        when(access.requireMember(projectId, "owner@example.com")).thenReturn(project);
        when(taskRepository.findByProjectId(projectId)).thenReturn(List.of(task));

        List<Task> result = taskService.getTasksForProject(projectId, Optional.of(""), "owner@example.com");

        assertEquals(1, result.size());
    }

    @Test
    void getTasksForProject_withUnknownStatus_throwsBadRequest() {
        when(access.requireMember(projectId, "owner@example.com")).thenReturn(project);

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> taskService.getTasksForProject(projectId, Optional.of("BOGUS"), "owner@example.com"));

        assertEquals(400, ex.getErrorCode());
    }

    @Test
    void getTasksForProject_accessCheckFails_errorPropagatesAndNothingIsQueried() {
        when(access.requireMember(projectId, "outsider@example.com")).thenThrow(notFound());

        assertThrows(CustomApiException.class,
                () -> taskService.getTasksForProject(projectId, Optional.empty(), "outsider@example.com"));

        verifyNoInteractions(taskRepository);
    }

    // ---- updateTask ----

    @Test
    void updateTask_asMember_updatesFields() {
        TaskRequest request = requestFor(null);
        request.setTitle("Renamed");
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(access.requireMember(projectId, "member@example.com")).thenReturn(project);
        when(taskRepository.save(task)).thenReturn(task);

        Task result = taskService.updateTask(taskId, request, "member@example.com");

        assertEquals("Renamed", result.getTitle());
        assertNull(result.getAssignee());
    }

    @Test
    void updateTask_accessCheckFails_errorPropagatesAndNothingIsSaved() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(access.requireMember(projectId, "outsider@example.com")).thenThrow(notFound());

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> taskService.updateTask(taskId, requestFor(null), "outsider@example.com"));

        assertEquals(404, ex.getErrorCode());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateTask_unknownTask_throwsNotFound_withTheSameMessageAsAnAccessFailure() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> taskService.updateTask(taskId, requestFor(null), "owner@example.com"));

        assertEquals(404, ex.getErrorCode());
        assertEquals(ProjectAccessService.NOT_FOUND_MESSAGE, ex.getMessage());
    }

    @Test
    void updateTask_assigneeNotInProject_throwsBadRequest() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(access.requireMember(projectId, "owner@example.com")).thenReturn(project);
        when(userRepository.findByEmail("outsider@example.com")).thenReturn(Optional.of(outsider));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, outsider.getId())).thenReturn(false);

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> taskService.updateTask(taskId, requestFor("outsider@example.com"), "owner@example.com"));

        assertEquals(400, ex.getErrorCode());
        verify(taskRepository, never()).save(any());
    }

    // ---- deleteTask ----

    @Test
    void deleteTask_asManagerOrOwner_deletesTask() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(access.requireManagerOrOwner(projectId, "owner@example.com")).thenReturn(project);

        assertDoesNotThrow(() -> taskService.deleteTask(taskId, "owner@example.com"));

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_accessCheckFails_errorPropagatesAndNothingIsDeleted() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(access.requireManagerOrOwner(projectId, "member@example.com"))
                .thenThrow(new CustomApiException("Only the project owner or a manager can do this", 403));

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> taskService.deleteTask(taskId, "member@example.com"));

        assertEquals(403, ex.getErrorCode());
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void deleteTask_unknownTask_throwsNotFound() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> taskService.deleteTask(taskId, "owner@example.com"));

        assertEquals(404, ex.getErrorCode());
    }
}
