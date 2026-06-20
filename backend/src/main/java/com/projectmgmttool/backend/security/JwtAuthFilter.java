package com.projectmgmttool.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Log the request for debugging
        logger.debug("Checking shouldNotFilter for path: " + path + ", method: " + method);

        // Skip JWT validation for public endpoints
        boolean shouldSkip = path.equals("/api/auth/register") ||
                            path.equals("/api/auth/login") ||
                            path.startsWith("/api/auth/register") ||
                            path.startsWith("/api/auth/login") ||
                            path.startsWith("/swagger-ui") ||
                            path.startsWith("/v3/api-docs") ||
                            path.equals("/error") ||
                            path.startsWith("/actuator");

        if (shouldSkip) {
            logger.info("Skipping JWT filter for public endpoint: " + path);
        }

        return shouldSkip;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Double-check: if this is a public endpoint, skip JWT processing entirely
        if (path.equals("/api/auth/register") || path.equals("/api/auth/login") ||
            path.startsWith("/api/auth/")) {
            logger.info("Bypassing JWT processing for public endpoint: " + path);
            filterChain.doFilter(request, response);
            return;
        }

        logger.debug("Processing JWT filter for path: " + path);

        try {
            String authHeader = request.getHeader("Authorization");
            String token = null;
            String username = null;

            // Only process if Authorization header exists and is properly formatted
            if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
                token = authHeader.substring(7);
                logger.debug("Extracted JWT token from Authorization header");

                // Validate token format before attempting to decode
                if (isValidTokenFormat(token)) {
                    try {
                        username = jwtUtil.extractUsername(token);
                        logger.debug("Successfully extracted username: " + username);
                    } catch (io.jsonwebtoken.io.DecodingException e) {
                        logger.warn("JWT DecodingException for path " + path + ": " + e.getMessage());
                        // Continue without authentication
                        filterChain.doFilter(request, response);
                        return;
                    } catch (Exception e) {
                        logger.warn("Failed to extract username from JWT token for path " + path + ": " + e.getMessage());
                        // Continue without authentication
                        filterChain.doFilter(request, response);
                        return;
                    }
                } else {
                    logger.warn("Invalid JWT token format detected for path: " + path);
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // Only proceed with authentication if we have a valid username and no existing authentication
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtUtil.validateToken(token, userDetails.getUsername())) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        logger.debug("Successfully authenticated user: " + username);
                    }
                } catch (Exception e) {
                    logger.warn("Authentication failed for user: " + username + ", error: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Unexpected error in JWT filter for path " + path + ": " + e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Basic validation to check if token has valid base64 format
     */
    private boolean isValidTokenFormat(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        // JWT tokens should have 3 parts separated by dots
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        // Check if each part contains valid base64 characters
        for (String part : parts) {
            if (!isValidBase64(part)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if string contains valid base64 characters
     */
    private boolean isValidBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        // Base64 characters include A-Z, a-z, 0-9, +, /, and = for padding
        return str.matches("^[A-Za-z0-9+/_-]*={0,2}$");
    }
}
