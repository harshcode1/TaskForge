package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.entity.*;
import com.projectmgmttool.backend.dto.ProjectMemberRequest;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ProjectMemberService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    public ProjectMember inviteMember(ProjectMemberRequest request, String ownerEmail) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomApiException("Project not found", 404));

        if (!project.getOwner().getEmail().equals(ownerEmail)) {
            throw new CustomApiException("Only the project owner can invite members.", 403);
        }

        User userToInvite = userRepository.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new CustomApiException("User to invite not found", 404));

        boolean alreadyMember = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userToInvite.getId());

        if (alreadyMember) {
            throw new CustomApiException("User is already a member of this project.", 409);
        }

        ProjectMember member = new ProjectMember();
        member.setProject(project);
        member.setUser(userToInvite);
        member.setRole(request.getRole());
        member.setJoinedAt(LocalDateTime.now());

        return projectMemberRepository.save(member);
    }

    public List<ProjectMember> getMembers(UUID projectId, String userEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomApiException("Project not found", 404));

        // Check if user is a member of the project or the owner
        boolean isOwner = project.getOwner().getEmail().equals(userEmail);
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserEmail(projectId, userEmail);

        if (!isOwner && !isMember) {
            throw new CustomApiException("You are not authorized to view this project's members", 403);
        }

        return projectMemberRepository.findByProjectId(projectId);
    }
}
