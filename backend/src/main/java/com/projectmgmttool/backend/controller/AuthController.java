package com.projectmgmttool.backend.controller;

import com.projectmgmttool.backend.dto.AuthRequest;
import com.projectmgmttool.backend.dto.AuthResponse;
import com.projectmgmttool.backend.dto.RegisterRequest;
import com.projectmgmttool.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user authentication")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided details.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User registered successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "409", description = "Email already exists",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Authenticate a user", description = "Authenticates a user and returns a JWT token.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User authenticated successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.login(request);
    }
}
