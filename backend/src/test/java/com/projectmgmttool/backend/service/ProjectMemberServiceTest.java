package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.dto.ProjectMemberRequest;
import com.projectmgmttool.backend.entity.*;
import com.projectmgmttool.backend.entity.enums.Role;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.ProjectMemberRepository;
import com.projectmgmttool.backend.repository.ProjectRepository;
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
class ProjectMemberServiceTest {

    @InjectMocks
    private ProjectMemberService projectMemberService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    private User owner;
    private User invitee;
    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();

        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@example.com");

        invitee = new User();
        invitee.setId(UUID.randomUUID());
        invitee.setEmail("invitee@example.com");

        project = new Project();
        project.setId(projectId);
        project.setOwner(owner);
        project.setName("Test Project");
    }

    @Test
    void inviteMember_asOwner_savesNewMember() {
        ProjectMemberRequest request = new ProjectMemberRequest(projectId, "invitee@example.com", Role.MEMBER);

        ProjectMember savedMember = new ProjectMember(invitee, project, Role.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("invitee@example.com")).thenReturn(Optional.of(invitee));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, invitee.getId())).thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(savedMember);

        ProjectMember result = projectMemberService.inviteMember(request, "owner@example.com");

        assertNotNull(result);
        assertEquals(Role.MEMBER, result.getRole());
    }

    @Test
    void inviteMember_asNonOwner_throwsForbidden() {
        ProjectMemberRequest request = new ProjectMemberRequest(projectId, "invitee@example.com", Role.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> projectMemberService.inviteMember(request, "notowner@example.com"));
        assertEquals(403, ex.getErrorCode());
    }

    @Test
    void inviteMember_alreadyMember_throwsConflict() {
        ProjectMemberRequest request = new ProjectMemberRequest(projectId, "invitee@example.com", Role.MEMBER);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("invitee@example.com")).thenReturn(Optional.of(invitee));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, invitee.getId())).thenReturn(true);

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> projectMemberService.inviteMember(request, "owner@example.com"));
        assertEquals(409, ex.getErrorCode());
    }
}
