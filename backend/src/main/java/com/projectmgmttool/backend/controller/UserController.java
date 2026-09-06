package com.projectmgmttool.backend.controller;

import com.projectmgmttool.backend.dto.ProfileDTO;
import com.projectmgmttool.backend.dto.UpdateProfileRequest;
import com.projectmgmttool.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "Endpoints for the authenticated user's own profile")
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "Get the current user's profile")
    @GetMapping("/me")
    public ResponseEntity<ProfileDTO> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getProfile(userDetails.getUsername()));
    }

    @Operation(summary = "Update the current user's profile")
    @PutMapping("/me")
    public ResponseEntity<ProfileDTO> updateMe(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.updateProfile(userDetails.getUsername(), request));
    }
}
