package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.dto.ProjectMemberRequest;
import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.ProjectMember;
import com.projectmgmttool.backend.entity.User;
import com.projectmgmttool.backend.entity.enums.Role;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.ProjectMemberRepository;
import com.projectmgmttool.backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectMemberService {

    @Autowired
    private ProjectAccessService projectAccessService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    public ProjectMember inviteMember(ProjectMemberRequest request, String ownerEmail) {
        Project project = projectAccessService.requireOwner(request.getProjectId(), ownerEmail);

        // Project-level roles are MANAGER and MEMBER only; the owner is Project.owner.
        if (request.getRole() == Role.ADMIN) {
            throw new CustomApiException("ADMIN is not a project role - use MANAGER or MEMBER", 400);
        }

        User userToInvite = userRepository.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new CustomApiException("User to invite not found", 404));

        boolean isOwner = project.getOwner().getId().equals(userToInvite.getId());
        boolean alreadyMember = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userToInvite.getId());

        if (isOwner || alreadyMember) {
            throw new CustomApiException("User is already a member of this project.", 409);
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(userToInvite);
        member.setRole(request.getRole());
        member.setJoinedAt(LocalDateTime.now());

        return projectMemberRepository.save(member);
    }

    public void removeMember(UUID projectId, UUID userId, String ownerEmail) {
        projectAccessService.requireOwner(projectId, ownerEmail);

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new CustomApiException("Member not found in this project", 404));

        projectMemberRepository.delete(member);
    }

    public List<ProjectMember> getMembers(UUID projectId, String userEmail) {
        projectAccessService.requireMember(projectId, userEmail);
        return projectMemberRepository.findByProjectId(projectId);
    }
}
