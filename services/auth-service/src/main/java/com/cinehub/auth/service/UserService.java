package com.cinehub.auth.service;

import com.cinehub.auth.dto.response.PagedResponse;
import com.cinehub.auth.dto.response.UserListResponse;
import com.cinehub.auth.entity.User;
import com.cinehub.auth.entity.Role;
import com.cinehub.auth.repository.UserRepository;
import com.cinehub.auth.repository.RoleRepository;

import jakarta.persistence.criteria.JoinType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public PagedResponse<UserListResponse> getUsers(
            String keyword,
            String status,
            String role,
            int page,
            int size,
            String sortBy,
            String sortType) {

        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";

        List<String> allowedSort = List.of("createdAt", "username", "email", "status");
        if (!allowedSort.contains(sortField)) {
            sortField = "createdAt";
        }

        String order = (sortType != null && !sortType.isBlank()) ? sortType : "DESC";
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(order);
        } catch (IllegalArgumentException ex) {
            direction = Sort.Direction.DESC;
        }

        int pageIndex = Math.max(1, page) - 1;
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(direction, sortField));

        Specification<User> spec = (root, query, cb) -> null;

        if (keyword != null && !keyword.isBlank()) {
            final String kw = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.or(
                        cb.like(cb.lower(root.get("username")), kw),
                        cb.like(cb.lower(root.get("email")), kw),
                        cb.like(cb.lower(root.get("phoneNumber")), kw));
            });
        }

        // --- role filter (DB stores role lowercase in your case) ---
        if (role != null && !role.isBlank()) {
            final String roleNormalized = role.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> {
                // fetch role to avoid N+1 and use LEFT JOIN
                root.fetch("role", JoinType.LEFT);
                query.distinct(true);
                return cb.equal(cb.lower(root.join("role", JoinType.LEFT).get("name")), roleNormalized);
            });
        }

        // --- status filter ---
        if (status != null && !status.isBlank()) {
            final String statusNormalized = status.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                return cb.equal(cb.lower(root.get("status")), statusNormalized);
            });
        }

        Page<User> users = userRepository.findAll(spec, pageable);

        return PagedResponse.<UserListResponse>builder()
                .data(users.map(UserListResponse::fromEntity).getContent())
                .page(users.getNumber() + 1) // trả về page 1-based cho client
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .build();
    }

    public UserListResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserListResponse.fromEntity(user);
    }

    public void updateUserStatus(UUID id, String newStatus) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(newStatus.toUpperCase());
        userRepository.save(user);
    }

    @Transactional
    public void updateUserRole(UUID id, String newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != null
                && "admin".equalsIgnoreCase(user.getRole().getName())) {
            throw new RuntimeException("Cannot modify role of an admin user");
        }

        if (newRole == null || newRole.isBlank()) {
            throw new IllegalArgumentException("newRole is required");
        }

        String normalized = newRole.trim().toLowerCase();

        Role role = roleRepository.findByName(normalized)
                .orElseThrow(() -> new RuntimeException("Role not found: " + normalized));

        user.setRole(role);
        userRepository.save(user);
    }

    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }
}
