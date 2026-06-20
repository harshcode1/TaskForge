package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.User;
import com.projectmgmttool.backend.entity.enums.Role;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.ProjectRepository;
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
class ProjectServiceTest {

    @InjectMocks
    private ProjectService projectService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    private User owner;
    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();

        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@example.com");
        owner.setName("Owner");
        owner.setRole(Role.MANAGER);

        project = new Project();
        project.setId(projectId);
        project.setName("Test Project");
        project.setDescription("Test description");
        project.setOwner(owner);
    }

    @Test
    void createProject_validOwner_returnsProject() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        Project result = projectService.createProject("Test Project", "Description", "owner@example.com");

        assertNotNull(result);
        assertEquals("Test Project", result.getName());
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void createProject_unknownUser_throwsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(CustomApiException.class,
                () -> projectService.createProject("Project", "Desc", "unknown@example.com"));
    }

    @Test
    void deleteProject_asOwner_deletesSuccessfully() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        assertDoesNotThrow(() -> projectService.deleteProject(projectId, "owner@example.com"));
        verify(projectRepository).delete(project);
    }

    @Test
    void deleteProject_asNonOwner_throwsForbidden() {
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> projectService.deleteProject(projectId, "other@example.com"));
        assertEquals(403, ex.getErrorCode());
    }

    @Test
    void getProjectsForUser_returnsVisibleProjects() {
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(projectRepository.findAllProjectsVisibleToUser(owner.getId())).thenReturn(List.of(project));

        List<Project> result = projectService.getProjectsForUser("owner@example.com");

        assertEquals(1, result.size());
    }
}
