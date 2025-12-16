package com.cinehub.profile.service;

import com.cinehub.profile.dto.response.LoyaltyHistoryResponse;
import com.cinehub.profile.dto.response.PagedLoyaltyHistoryResponse;
import com.cinehub.profile.entity.LoyaltyHistory;
import com.cinehub.profile.entity.UserProfile;
import com.cinehub.profile.repository.LoyaltyHistoryRepository;
import com.cinehub.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyHistoryService {

        private final LoyaltyHistoryRepository loyaltyHistoryRepository;
        private final UserProfileRepository userProfileRepository;

        @Transactional
        public void recordLoyaltyTransaction(
                        UUID userId,
                        UUID bookingId,
                        String bookingCode,
                        Integer pointsChange,
                        BigDecimal amountSpent,
                        String description) {

                UserProfile user = userProfileRepository.findByUserId(userId)
                                .orElseThrow(() -> new RuntimeException("User profile not found"));

                Integer pointsBefore = user.getLoyaltyPoint();
                Integer pointsAfter = pointsBefore + pointsChange;

                LoyaltyHistory history = LoyaltyHistory.builder()
                                .user(user)
                                .bookingId(bookingId)
                                .bookingCode(bookingCode)
                                .pointsChange(pointsChange)
                                .pointsBefore(pointsBefore)
                                .pointsAfter(pointsAfter)
                                .amountSpent(amountSpent)
                                .description(description)
                                .build();

                loyaltyHistoryRepository.save(history);

                log.info("Recorded loyalty transaction for user {}: {} points ({})",
                                userId, pointsChange);
        }

        public PagedLoyaltyHistoryResponse getUserLoyaltyHistory(UUID userId, int page, int size) {
                Pageable pageable = PageRequest.of(page - 1, size);
                Page<LoyaltyHistory> historyPage = loyaltyHistoryRepository
                                .findByUser_UserIdOrderByCreatedAtDesc(userId, pageable);

                List<LoyaltyHistoryResponse> content = historyPage.getContent().stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());

                return PagedLoyaltyHistoryResponse.builder()
                                .data(content)
                                .page(page)
                                .size(size)
                                .totalElements(historyPage.getTotalElements())
                                .totalPages(historyPage.getTotalPages())
                                .build();
        }

        private LoyaltyHistoryResponse mapToResponse(LoyaltyHistory history) {
                return LoyaltyHistoryResponse.builder()
                                .id(history.getId())
                                .bookingId(history.getBookingId())
                                .pointsChange(history.getPointsChange())
                                .pointsBefore(history.getPointsBefore())
                                .pointsAfter(history.getPointsAfter())
                                .amountSpent(history.getAmountSpent())
                                .description(history.getDescription())
                                .createdAt(history.getCreatedAt())
                                .build();
        }
}
