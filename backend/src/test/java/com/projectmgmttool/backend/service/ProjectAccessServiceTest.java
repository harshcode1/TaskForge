package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.ProjectMember;
import com.projectmgmttool.backend.entity.User;
import com.projectmgmttool.backend.entity.enums.Role;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.ProjectMemberRepository;
import com.projectmgmttool.backend.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Rule under test: a user who is not the owner or a member of a project gets
 * 404 (same as a project that doesn't exist, so its id can't be probed); a
 * member who lacks the rights for an action gets 403.
 */
@ExtendWith(MockitoExtension.class)
class ProjectAccessServiceTest {

    private static final String OWNER = "owner@example.com";
    private static final String MANAGER = "manager@example.com";
    private static final String MEMBER = "member@example.com";
    private static final String STRANGER = "stranger@example.com";

    @InjectMocks
    private ProjectAccessService access;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    private UUID projectId;
    private Project project;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail(OWNER);
        project = new Project();
        project.setId(projectId);
        project.setOwner(owner);
        // lenient: the "unknown project" tests deliberately never look this project up
        lenient().when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    }

    private void givenMember(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        when(projectMemberRepository.findByProjectIdAndUserEmail(projectId, email))
                .thenReturn(Optional.of(new ProjectMember(user, project, role)));
    }

    private void assertStatus(int expected, Runnable call) {
        CustomApiException ex = assertThrows(CustomApiException.class, call::run);
        assertEquals(expected, ex.getErrorCode());
    }

    // ---- requireMember ----

    @Test
    void requireMember_owner_returnsProject() {
        assertSame(project, access.requireMember(projectId, OWNER));
    }

    @Test
    void requireMember_manager_returnsProject() {
        givenMember(MANAGER, Role.MANAGER);
        assertSame(project, access.requireMember(projectId, MANAGER));
    }

    @Test
    void requireMember_plainMember_returnsProject() {
        givenMember(MEMBER, Role.MEMBER);
        assertSame(project, access.requireMember(projectId, MEMBER));
    }

    @Test
    void requireMember_nonMember_throws404() {
        assertStatus(404, () -> access.requireMember(projectId, STRANGER));
    }

    @Test
    void requireMember_unknownProject_throws404() {
        UUID unknown = UUID.randomUUID();
        when(projectRepository.findById(unknown)).thenReturn(Optional.empty());
        assertStatus(404, () -> access.requireMember(unknown, OWNER));
    }

    // ---- requireManagerOrOwner ----

    @Test
    void requireManagerOrOwner_owner_returnsProject() {
        assertSame(project, access.requireManagerOrOwner(projectId, OWNER));
    }

    @Test
    void requireManagerOrOwner_manager_returnsProject() {
        givenMember(MANAGER, Role.MANAGER);
        assertSame(project, access.requireManagerOrOwner(projectId, MANAGER));
    }

    @Test
    void requireManagerOrOwner_plainMember_throws403() {
        givenMember(MEMBER, Role.MEMBER);
        assertStatus(403, () -> access.requireManagerOrOwner(projectId, MEMBER));
    }

    @Test
    void requireManagerOrOwner_nonMember_throws404() {
        assertStatus(404, () -> access.requireManagerOrOwner(projectId, STRANGER));
    }

    @Test
    void requireManagerOrOwner_unknownProject_throws404() {
        UUID unknown = UUID.randomUUID();
        when(projectRepository.findById(unknown)).thenReturn(Optional.empty());
        assertStatus(404, () -> access.requireManagerOrOwner(unknown, OWNER));
    }

    // ---- requireOwner ----

    @Test
    void requireOwner_owner_returnsProject() {
        assertSame(project, access.requireOwner(projectId, OWNER));
    }

    @Test
    void requireOwner_manager_throws403() {
        givenMember(MANAGER, Role.MANAGER);
        assertStatus(403, () -> access.requireOwner(projectId, MANAGER));
    }

    @Test
    void requireOwner_plainMember_throws403() {
        givenMember(MEMBER, Role.MEMBER);
        assertStatus(403, () -> access.requireOwner(projectId, MEMBER));
    }

    @Test
    void requireOwner_nonMember_throws404() {
        assertStatus(404, () -> access.requireOwner(projectId, STRANGER));
    }

    @Test
    void requireOwner_unknownProject_throws404() {
        UUID unknown = UUID.randomUUID();
        when(projectRepository.findById(unknown)).thenReturn(Optional.empty());
        assertStatus(404, () -> access.requireOwner(unknown, OWNER));
    }
}
