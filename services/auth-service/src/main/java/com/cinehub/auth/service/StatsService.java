package com.cinehub.auth.service;

import com.cinehub.auth.dto.response.StatsOverviewResponse;
import com.cinehub.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

        double customerRatio = totalUsers == 0 ? 0.0 : (double) totalCustomers / totalUsers * 100;

        return StatsOverviewResponse.builder()
                .totalUsers(totalUsers)
                .totalCustomers(totalCustomers)
                .totalStaff(totalStaff)
                .totalManagers(totalManagers)
                .totalAdmins(totalAdmins)
                .customerRatio(customerRatio)
                .build();
    }
}
