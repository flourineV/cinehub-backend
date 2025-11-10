package com.cinehub.auth.service;

import com.cinehub.auth.dto.response.UserListResponse;
import com.cinehub.auth.entity.User;
import com.cinehub.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Page<UserListResponse> getUsers(int page, int size, String role, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<User> spec = Specification.allOf();

        if (role != null && !role.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.join("role").get("name")), role.toLowerCase()));
        }

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
        }

        Page<User> users = userRepository.findAll(spec, pageable);
        return users.map(UserListResponse::fromEntity);
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

    public void updateUserRole(UUID id, String newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.getRole().setName(newRole.toUpperCase());
        userRepository.save(user);
    }

    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }
}
