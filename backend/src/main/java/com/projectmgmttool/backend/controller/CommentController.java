package com.projectmgmttool.backend.controller;

import com.projectmgmttool.backend.entity.Comment;
import com.projectmgmttool.backend.dto.CommentRequest;
import com.projectmgmttool.backend.service.CommentService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Operation(summary = "Add a new comment", description = "Adds a new comment to a task.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comment added successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Comment.class))),
        @ApiResponse(responseCode = "404", description = "Task not found",
            content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/add")
    public ResponseEntity<Comment> addComment(
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Comment comment = commentService.addComment(request, userDetails.getUsername());
        return ResponseEntity.ok(comment);
    }

    @Operation(summary = "Get comments for a task", description = "Retrieves all comments for a specific task.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comments retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Comment.class))),
        @ApiResponse(responseCode = "404", description = "Task not found",
            content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<Comment>> getComments(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(commentService.getCommentsForTask(taskId, userDetails.getUsername()));
    }

    @Operation(summary = "Delete a comment", description = "Deletes a comment by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comment deleted successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Comment not found",
            content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        commentService.deleteComment(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
