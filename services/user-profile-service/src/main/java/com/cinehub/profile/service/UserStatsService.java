package com.cinehub.profile.service;

import com.cinehub.profile.dto.response.UserStatsResponse;
import com.cinehub.profile.entity.ManagerProfile;
import com.cinehub.profile.entity.StaffProfile;
import com.cinehub.profile.entity.UserProfile;
import com.cinehub.profile.repository.ManagerProfileRepository;
import com.cinehub.profile.repository.StaffProfileRepository;
import com.cinehub.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final UserProfileRepository userProfileRepository;
    private final ManagerProfileRepository managerRepository;
    private final StaffProfileRepository staffRepository;

    public UserStatsResponse getOverviewStats() {
        // --- Rank distribution ---
        List<UserProfile> allProfiles = userProfileRepository.findAll();
        long total = allProfiles.size();

        long bronze = allProfiles.stream()
                .filter(p -> p.getRank() != null && "bronze".equalsIgnoreCase(p.getRank().getName()))
                .count();

        long silver = allProfiles.stream()
                .filter(p -> p.getRank() != null && "silver".equalsIgnoreCase(p.getRank().getName()))
                .count();

        long gold = allProfiles.stream()
                .filter(p -> p.getRank() != null && "gold".equalsIgnoreCase(p.getRank().getName()))
                .count();

        double bronzePct = total > 0 ? (bronze * 100.0 / total) : 0;
        double silverPct = total > 0 ? (silver * 100.0 / total) : 0;
        double goldPct = total > 0 ? (gold * 100.0 / total) : 0;

        var rankDistribution = UserStatsResponse.RankDistribution.builder()
                .bronzeCount(bronze)
                .silverCount(silver)
                .goldCount(gold)
                .bronzePercentage(bronzePct)
                .silverPercentage(silverPct)
                .goldPercentage(goldPct)
                .build();

        // --- Staff/Manager per cinema ---
        Map<UUID, Long> managerCountMap = managerRepository.findAll().stream()
                .filter(m -> m.getManagedCinemaId() != null)
                .collect(Collectors.groupingBy(ManagerProfile::getManagedCinemaId, Collectors.counting()));

        Map<UUID, Long> staffCountMap = staffRepository.findAll().stream()
                .filter(s -> s.getCinemaId() != null)
                .collect(Collectors.groupingBy(StaffProfile::getCinemaId, Collectors.counting()));

        Set<UUID> allCinemaIds = new HashSet<>();
        allCinemaIds.addAll(managerCountMap.keySet());
        allCinemaIds.addAll(staffCountMap.keySet());

        List<UserStatsResponse.CinemaStaffCount> staffCounts = allCinemaIds.stream()
                .map(id -> UserStatsResponse.CinemaStaffCount.builder()
                        .cinemaId(id.toString())
                        .managerCount(managerCountMap.getOrDefault(id, 0L))
                        .staffCount(staffCountMap.getOrDefault(id, 0L))
                        .build())
                .toList();

        return UserStatsResponse.builder()
                .rankDistribution(rankDistribution)
                .staffCounts(staffCounts)
                .build();
    }
}
