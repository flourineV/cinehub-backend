package com.cinehub.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatsOverviewResponse {
    private long totalUsers;
    private long totalCustomers;
    private long totalStaff;
    private long totalManagers;
    private long totalAdmins;
}
