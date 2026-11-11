package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.entity.Project;
import com.projectmgmttool.backend.entity.ProjectMember;
import com.projectmgmttool.backend.entity.enums.Role;
import com.projectmgmttool.backend.exception.CustomApiException;
import com.projectmgmttool.backend.repository.ProjectMemberRepository;
import com.projectmgmttool.backend.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * The one place that decides who may touch a project. Every service that works
 * with a project (or a task/comment inside one) calls this first.
 *
 * Rules:
 * - The project owner is Project.owner; project_members holds MANAGER and MEMBER.
 * - Someone who is neither owner nor member gets 404, exactly like a project
 *   that doesn't exist, so a project id can't be probed for existence.
 * - A member who lacks the rights for an action gets 403 (they already know
 *   the project exists, so there is nothing to hide).
 */
@Service
public class ProjectAccessService {

    public static final String NOT_FOUND_MESSAGE = "Not found";

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    /** Owner or any member (MANAGER or MEMBER). */
    public Project requireMember(UUID projectId, String userEmail) {
        Project project = findProject(projectId);
        if (!isOwner(project, userEmail) && findMembership(projectId, userEmail).isEmpty()) {
            throw notFound();
        }
        return project;
    }

    /** Owner or a MANAGER member; a plain MEMBER gets 403. */
    public Project requireManagerOrOwner(UUID projectId, String userEmail) {
        Project project = findProject(projectId);
        if (isOwner(project, userEmail)) {
            return project;
        }
        ProjectMember membership = findMembership(projectId, userEmail).orElseThrow(ProjectAccessService::notFound);
        if (membership.getRole() != Role.MANAGER) {
            throw new CustomApiException("Only the project owner or a manager can do this", 403);
        }
        return project;
    }

    /** Owner only; any member gets 403. */
    public Project requireOwner(UUID projectId, String userEmail) {
        Project project = findProject(projectId);
        if (isOwner(project, userEmail)) {
            return project;
        }
        findMembership(projectId, userEmail).orElseThrow(ProjectAccessService::notFound);
        throw new CustomApiException("Only the project owner can do this", 403);
    }

    private Project findProject(UUID projectId) {
        return projectRepository.findById(projectId).orElseThrow(ProjectAccessService::notFound);
    }

    private boolean isOwner(Project project, String userEmail) {
        return project.getOwner().getEmail().equals(userEmail);
    }

    private Optional<ProjectMember> findMembership(UUID projectId, String userEmail) {
        return projectMemberRepository.findByProjectIdAndUserEmail(projectId, userEmail);
    }

    /**
     * The one 404 used for "doesn't exist" and "not yours" alike. Same status and
     * same message, so a caller can't tell a real id from a made-up one. Services
     * that look up a task or comment before checking access reuse it.
     */
    public static CustomApiException notFound() {
        return new CustomApiException(NOT_FOUND_MESSAGE, 404);
    }
}
