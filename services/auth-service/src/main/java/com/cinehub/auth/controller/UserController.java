package com.cinehub.auth.controller;

import com.cinehub.auth.dto.response.PagedResponse;
import com.cinehub.auth.dto.response.UserListResponse;
import com.cinehub.auth.security.AuthChecker;
import com.cinehub.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<PagedResponse<UserListResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {

        AuthChecker.requireAdmin();
        return ResponseEntity.ok(userService.getUsers(page, size, role, status));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserListResponse> getUserById(@PathVariable UUID id) {
        AuthChecker.requireAdmin();
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<String> updateUserStatus(
            @PathVariable UUID id,
            @RequestParam String newStatus) {

        AuthChecker.requireAdmin();
        userService.updateUserStatus(id, newStatus);
        return ResponseEntity.ok("User status updated successfully");
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<String> updateUserRole(
            @PathVariable UUID id,
            @RequestParam String newRole) {

        AuthChecker.requireAdmin();
        userService.updateUserRole(id, newRole);
        return ResponseEntity.ok("User role updated successfully");
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable UUID id) {
        AuthChecker.requireAdmin();
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully");
    }
}
