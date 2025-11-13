package com.cinehub.auth.repository;

import com.cinehub.auth.entity.User;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Optional<User> findByNationalId(String nationalId);

    Boolean existsByNationalId(String nationalId);

    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.username = :identifier OR u.phoneNumber = :identifier")
    Optional<User> findByEmailOrUsernameOrPhoneNumber(@Param("identifier") String identifier);

    long countByRole_NameIgnoreCase(String roleName);

    @Query("SELECT YEAR(u.createdAt) AS year, MONTH(u.createdAt) AS month, COUNT(u.id) AS total " +
            "FROM User u " +
            "GROUP BY YEAR(u.createdAt), MONTH(u.createdAt) " +
            "ORDER BY year ASC, month ASC")
    List<Object[]> countUserRegistrationsByMonth();

    @EntityGraph(attributePaths = { "role" })
    Page<User> findAll(Specification<User> spec, Pageable pageable);

}
