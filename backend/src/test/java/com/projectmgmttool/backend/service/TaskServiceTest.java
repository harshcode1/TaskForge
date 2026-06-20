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
import com.projectmgmttool.backend.repository.ProjectRepository;
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
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private UserRepository userRepository;

    private User owner;
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

    @Test
    void createTask_validRequest_returnsTask() {
        TaskRequest request = new TaskRequest();
        request.setProjectId(projectId);
        request.setTitle("New Task");
        request.setDescription("Description");
        request.setStatus("TODO");
        request.setPriority(Priority.HIGH);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Task result = taskService.createTask(request, "owner@example.com");

        assertNotNull(result);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void createTask_projectNotFound_throwsException() {
        TaskRequest request = new TaskRequest();
        request.setProjectId(projectId);
        request.setTitle("New Task");
        request.setStatus("TODO");
        request.setPriority(Priority.MEDIUM);

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThrows(CustomApiException.class, () -> taskService.createTask(request, "owner@example.com"));
    }

    @Test
    void getTasksForProject_noFilter_returnsAllTasks() {
        when(taskRepository.findByProjectId(projectId)).thenReturn(List.of(task));

        List<Task> result = taskService.getTasksForProject(projectId, Optional.empty());

        assertEquals(1, result.size());
    }

    @Test
    void deleteTask_asOwner_deletesSuccessfully() {
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(projectMemberRepository.findByProjectIdAndUserEmail(projectId, "owner@example.com"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> taskService.deleteTask(taskId, "owner@example.com"));
        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_asUnauthorizedUser_throwsForbidden() {
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setEmail("other@example.com");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));
        when(projectMemberRepository.findByProjectIdAndUserEmail(projectId, "other@example.com"))
                .thenReturn(Optional.empty());

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> taskService.deleteTask(taskId, "other@example.com"));
        assertEquals(403, ex.getErrorCode());
    }
}
