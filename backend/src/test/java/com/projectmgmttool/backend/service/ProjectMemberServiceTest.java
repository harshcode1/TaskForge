package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.dto.ProjectMemberRequest;
import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.ProjectMember;
import com.projectmgmttool.backend.entity.User;
import com.projectmgmttool.backend.entity.enums.Role;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.ProjectMemberRepository;
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
class ProjectMemberServiceTest {

    @InjectMocks
    private ProjectMemberService projectMemberService;

    @Mock
    private ProjectAccessService access;

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

    // ---- inviteMember ----

    @Test
    void inviteMember_asOwner_savesNewMember() {
        ProjectMemberRequest request = new ProjectMemberRequest(projectId, "invitee@example.com", Role.MEMBER);
        ProjectMember savedMember = new ProjectMember(invitee, project, Role.MEMBER);

        when(access.requireOwner(projectId, "owner@example.com")).thenReturn(project);
        when(userRepository.findByEmail("invitee@example.com")).thenReturn(Optional.of(invitee));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, invitee.getId())).thenReturn(false);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(savedMember);

        ProjectMember result = projectMemberService.inviteMember(request, "owner@example.com");

        assertNotNull(result);
        assertEquals(Role.MEMBER, result.getRole());
    }

    @Test
    void inviteMember_accessCheckFails_errorPropagatesAndNothingIsSaved() {
        ProjectMemberRequest request = new ProjectMemberRequest(projectId, "invitee@example.com", Role.MEMBER);
        when(access.requireOwner(projectId, "notowner@example.com"))
                .thenThrow(new CustomApiException("Only the project owner can do this", 403));

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> projectMemberService.inviteMember(request, "notowner@example.com"));

        assertEquals(403, ex.getErrorCode());
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void inviteMember_adminRole_throwsBadRequest() {
        ProjectMemberRequest request = new ProjectMemberRequest(projectId, "invitee@example.com", Role.ADMIN);
        when(access.requireOwner(projectId, "owner@example.com")).thenReturn(project);

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> projectMemberService.inviteMember(request, "owner@example.com"));

        assertEquals(400, ex.getErrorCode());
        verify(projectMemberRepository, never()).save(any());
    }

    @Test
    void inviteMember_alreadyMember_throwsConflict() {
        ProjectMemberRequest request = new ProjectMemberRequest(projectId, "invitee@example.com", Role.MEMBER);
        when(access.requireOwner(projectId, "owner@example.com")).thenReturn(project);
        when(userRepository.findByEmail("invitee@example.com")).thenReturn(Optional.of(invitee));
        when(projectMemberRepository.existsByProjectIdAndUserId(projectId, invitee.getId())).thenReturn(true);

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> projectMemberService.inviteMember(request, "owner@example.com"));

        assertEquals(409, ex.getErrorCode());
    }

    @Test
    void inviteMember_inviteeIsTheOwner_throwsConflict() {
        ProjectMemberRequest request = new ProjectMemberRequest(projectId, "owner@example.com", Role.MEMBER);
        when(access.requireOwner(projectId, "owner@example.com")).thenReturn(project);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> projectMemberService.inviteMember(request, "owner@example.com"));

        assertEquals(409, ex.getErrorCode());
        verify(projectMemberRepository, never()).save(any());
    }

    // ---- removeMember ----

    @Test
    void removeMember_asOwner_deletesTheMembership() {
        ProjectMember membership = new ProjectMember(invitee, project, Role.MEMBER);
        when(access.requireOwner(projectId, "owner@example.com")).thenReturn(project);
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, invitee.getId()))
                .thenReturn(Optional.of(membership));

        projectMemberService.removeMember(projectId, invitee.getId(), "owner@example.com");

        verify(projectMemberRepository).delete(membership);
    }

    @Test
    void removeMember_userIsNotAMember_throwsNotFound() {
        when(access.requireOwner(projectId, "owner@example.com")).thenReturn(project);
        when(projectMemberRepository.findByProjectIdAndUserId(projectId, invitee.getId()))
                .thenReturn(Optional.empty());

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> projectMemberService.removeMember(projectId, invitee.getId(), "owner@example.com"));

        assertEquals(404, ex.getErrorCode());
    }

    @Test
    void removeMember_accessCheckFails_errorPropagatesAndNothingIsDeleted() {
        when(access.requireOwner(projectId, "member@example.com"))
                .thenThrow(new CustomApiException("Only the project owner can do this", 403));

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> projectMemberService.removeMember(projectId, invitee.getId(), "member@example.com"));

        assertEquals(403, ex.getErrorCode());
        verify(projectMemberRepository, never()).delete(any());
    }

    // ---- getMembers ----

    @Test
    void getMembers_asMember_returnsTheList() {
        List<ProjectMember> members = List.of(new ProjectMember(invitee, project, Role.MEMBER));
        when(access.requireMember(projectId, "invitee@example.com")).thenReturn(project);
        when(projectMemberRepository.findByProjectId(projectId)).thenReturn(members);

        assertEquals(members, projectMemberService.getMembers(projectId, "invitee@example.com"));
    }

    @Test
    void getMembers_accessCheckFails_errorPropagates() {
        when(access.requireMember(projectId, "stranger@example.com"))
                .thenThrow(new CustomApiException("Project not found", 404));

        CustomApiException ex = assertThrows(CustomApiException.class,
                () -> projectMemberService.getMembers(projectId, "stranger@example.com"));

        assertEquals(404, ex.getErrorCode());
        verify(projectMemberRepository, never()).findByProjectId(any());
    }
}
