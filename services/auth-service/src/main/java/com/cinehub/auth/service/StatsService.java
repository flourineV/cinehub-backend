package com.cinehub.auth.service;

import com.cinehub.auth.dto.response.StatsOverviewResponse;
import com.cinehub.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.cinehub.auth.dto.response.UserRegistrationStatsResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserRepository userRepository;

    public StatsOverviewResponse getOverview() {
        long totalUsers = userRepository.count();
        long totalCustomers = userRepository.countByRole_NameIgnoreCase("CUSTOMER");
        long totalStaff = userRepository.countByRole_NameIgnoreCase("STAFF");
        long totalManagers = userRepository.countByRole_NameIgnoreCase("MANAGER");
        long totalAdmins = userRepository.countByRole_NameIgnoreCase("ADMIN");

        return StatsOverviewResponse.builder()
                .totalUsers(totalUsers)
                .totalCustomers(totalCustomers)
                .totalStaff(totalStaff)
                .totalManagers(totalManagers)
                .totalAdmins(totalAdmins)
                .build();
    }

    public List<UserRegistrationStatsResponse> getUserRegistrationsByMonth() {
        List<Object[]> results = userRepository.countUserRegistrationsByMonth();

        return results.stream()
                .map(r -> new UserRegistrationStatsResponse(
                        ((Number) r[0]).intValue(), // year
                        ((Number) r[1]).intValue(), // month
                        ((Number) r[2]).longValue() // total
                ))
                .collect(Collectors.toList());
    }
}
