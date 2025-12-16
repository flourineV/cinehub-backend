package com.cinehub.profile.service;

import com.cinehub.profile.dto.response.UserStatsResponse;
import com.cinehub.profile.dto.response.UserPersonalStatsResponse;
import com.cinehub.profile.entity.ManagerProfile;
import com.cinehub.profile.entity.StaffProfile;
import com.cinehub.profile.entity.UserProfile;
import com.cinehub.profile.repository.ManagerProfileRepository;
import com.cinehub.profile.repository.StaffProfileRepository;
import com.cinehub.profile.repository.UserProfileRepository;
import com.cinehub.profile.repository.UserFavoriteMovieRepository;
import com.cinehub.profile.adapter.client.BookingClient;

import jakarta.transaction.Transactional;
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
        private final UserFavoriteMovieRepository favoriteMovieRepository;
        private final BookingClient bookingClient;

        @Transactional
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
                Map<String, Long> managerCountMap = managerRepository.findAll().stream()
                                .filter(m -> m.getManagedCinemaName() != null)
                                .collect(Collectors.groupingBy(ManagerProfile::getManagedCinemaName,
                                                Collectors.counting()));

                Map<String, Long> staffCountMap = staffRepository.findAll().stream()
                                .filter(s -> s.getCinemaName() != null)
                                .collect(Collectors.groupingBy(StaffProfile::getCinemaName, Collectors.counting()));

                Set<String> allCinemaNames = new HashSet<>();
                allCinemaNames.addAll(managerCountMap.keySet());
                allCinemaNames.addAll(staffCountMap.keySet());

                return UserStatsResponse.builder()
                                .rankDistribution(rankDistribution)
                                .build();
        }

        public UserPersonalStatsResponse getUserPersonalStats(UUID userId) {
                UserProfile profile = userProfileRepository.findByUserId(userId)
                                .orElseThrow(() -> new RuntimeException("User profile not found"));

                long favoriteMoviesCount = favoriteMovieRepository.countById_UserId(userId);
                long bookingsCount = bookingClient.getBookingCountByUserId(userId);

                return UserPersonalStatsResponse.builder()
                                .totalBookings(bookingsCount)
                                .totalFavoriteMovies(favoriteMoviesCount)
                                .loyaltyPoints(profile.getLoyaltyPoint())
                                .build();
        }
}
