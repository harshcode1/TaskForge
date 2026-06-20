package com.projectmgmttool.backend.controller;

import com.projectmgmttool.backend.dto.DashboardResponse;
import com.projectmgmttool.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Operation(summary = "Get project dashboard", description = "Retrieves dashboard data for a specific project.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DashboardResponse.class))),
        @ApiResponse(responseCode = "404", description = "Project not found",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{projectId}")
    public ResponseEntity<DashboardResponse> getDashboard(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        DashboardResponse data = dashboardService.getDashboardData(projectId, userDetails.getUsername());
        return ResponseEntity.ok(data);
    }

    @Operation(summary = "Get personal dashboard", description = "Retrieves personal dashboard data for the authenticated user.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = DashboardResponse.class)))
    })
    @GetMapping("/my-dashboard")
    public ResponseEntity<DashboardResponse> getPersonalDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        DashboardResponse data = dashboardService.getUserDashboardData(userDetails.getUsername());
        return ResponseEntity.ok(data);
    }
}
