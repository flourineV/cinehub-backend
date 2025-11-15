package com.cinehub.analytics.service;

import com.cinehub.analytics.dto.request.TrackActivityRequest;
import com.cinehub.analytics.dto.response.DailyStatsResponse;
import com.cinehub.analytics.dto.response.UserActivityResponse;
import com.cinehub.analytics.entity.DailyStats;
import com.cinehub.analytics.entity.UserActivity;
import com.cinehub.analytics.repository.DailyStatsRepository;
import com.cinehub.analytics.repository.UserActivityRepository;
import com.cinehub.analytics.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserActivityRepository userActivityRepository;
    private final DailyStatsRepository dailyStatsRepository;

    @Transactional
    public void trackActivity(TrackActivityRequest request, HttpServletRequest httpRequest) {
        UserContext context = UserContext.get();

        UUID userId = null;
        if (context != null && context.getUserId() != null && !context.getUserId().isEmpty()) {
            try {
                userId = UUID.fromString(context.getUserId());
            } catch (Exception e) {
                log.warn("Invalid userId in context: {}", context.getUserId());
            }
        }

        UserActivity activity = UserActivity.builder()
                .userId(userId)
                .activityType(request.getActivityType())
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .metadata(request.getMetadata())
                .ipAddress(getClientIP(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .sessionId(request.getSessionId())
                .build();

        userActivityRepository.save(activity);
        log.info("Tracked activity: {} for user: {}", request.getActivityType(), userId);
    }

    public Page<UserActivityResponse> getUserActivities(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserActivity> activities = userActivityRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return activities.map(this::mapToResponse);
    }

    public Map<String, Long> getActivityStats(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = userActivityRepository.countByActivityTypeAndDateRange(start, end);

        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]));
    }

    public Long getUniqueVisitors(LocalDateTime start, LocalDateTime end) {
        return userActivityRepository.countUniqueVisitors(start, end);
    }

    public List<DailyStatsResponse> getDailyStats(LocalDate start, LocalDate end) {
        List<DailyStats> stats = dailyStatsRepository.findByDateBetweenOrderByDateDesc(start, end);

        return stats.stream()
                .map(this::mapToDailyStatsResponse)
                .collect(Collectors.toList());
    }

    private UserActivityResponse mapToResponse(UserActivity activity) {
        return UserActivityResponse.builder()
                .id(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getActivityType())
                .entityType(activity.getEntityType())
                .entityId(activity.getEntityId())
                .metadata(activity.getMetadata())
                .createdAt(activity.getCreatedAt())
                .build();
    }

    private DailyStatsResponse mapToDailyStatsResponse(DailyStats stats) {
        return DailyStatsResponse.builder()
                .date(stats.getDate())
                .totalPageViews(stats.getTotalPageViews())
                .uniqueVisitors(stats.getUniqueVisitors())
                .totalBookings(stats.getTotalBookings())
                .totalRevenue(stats.getTotalRevenue())
                .newUsers(stats.getNewUsers())
                .guestUsers(stats.getGuestUsers())
                .build();
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
