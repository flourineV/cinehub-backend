package com.cinehub.profile.controller;

import com.cinehub.profile.entity.ManagerProfile;
import com.cinehub.profile.security.AuthChecker;
import com.cinehub.profile.service.ManagerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles/manager")
@RequiredArgsConstructor
public class ManagerProfileController {

    private final ManagerProfileService managerService;

    @PostMapping
    public ResponseEntity<ManagerProfile> createManager(
            @RequestParam UUID userProfileId,
            @RequestParam(required = false) UUID managedCinemaId,
            @RequestParam(required = false) LocalDate hireDate) {

        AuthChecker.requireAdmin();

        ManagerProfile created = managerService.createManager(userProfileId, managedCinemaId, hireDate);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/user/{userProfileId}")
    public ResponseEntity<ManagerProfile> getManagerByUser(@PathVariable UUID userProfileId) {
        AuthChecker.requireAdmin();
        return ResponseEntity.ok(managerService.getManagerByUserProfileId(userProfileId));
    }

    @GetMapping
    public ResponseEntity<List<ManagerProfile>> getAllManagers() {
        AuthChecker.requireAdmin();
        return ResponseEntity.ok(managerService.getAllManagers());
    }

    @GetMapping("/cinema/{cinemaId}")
    public ResponseEntity<List<ManagerProfile>> getManagersByCinema(@PathVariable UUID cinemaId) {
        AuthChecker.requireAdmin();
        return ResponseEntity.ok(managerService.getManagersByCinema(cinemaId));
    }

    @DeleteMapping("/{managerId}")
    public ResponseEntity<Void> deleteManager(@PathVariable UUID managerId) {
        AuthChecker.requireAdmin();
        managerService.deleteManager(managerId);
        return ResponseEntity.noContent().build();
    }
}
