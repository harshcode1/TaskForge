package com.projectmgmttool.backend.controller;


import com.projectmgmttool.backend.entity.ProjectMember;
import com.projectmgmttool.backend.dto.ProjectMemberRequest;
import com.projectmgmttool.backend.service.ProjectMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/project-members")
public class ProjectMemberController {

    @Autowired
    private ProjectMemberService memberService;

    @Operation(summary = "Invite a user to a project", description = "Invites a user to join a project.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User invited successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectMember.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/invite")
    public ResponseEntity<ProjectMember> inviteUser(
            @Valid @RequestBody ProjectMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ProjectMember member = memberService.inviteMember(request, userDetails.getUsername());
        return ResponseEntity.ok(member);
    }

    @Operation(summary = "Get project members", description = "Retrieves all members of a specific project.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Members retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectMember.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{projectId}")
    public ResponseEntity<List<ProjectMember>> getMembers(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<ProjectMember> members = memberService.getMembers(projectId, userDetails.getUsername());
        return ResponseEntity.ok(members);
    }
}
