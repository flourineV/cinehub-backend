package com.cinehub.profile.service;

import com.cinehub.profile.dto.request.RankRequest;
import com.cinehub.profile.dto.response.RankResponse;
import com.cinehub.profile.entity.UserRank;
import com.cinehub.profile.exception.ResourceNotFoundException;
import com.cinehub.profile.repository.UserRankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserRankService {

    private final UserRankRepository rankRepository;

    public RankResponse createRank(RankRequest request) {
        // Bổ sung: Logic nghiệp vụ để đảm bảo minPoints < maxPoints
        if (request.getMaxPoints() != null && request.getMinPoints() >= request.getMaxPoints()) {
            throw new IllegalArgumentException("Minimum points must be less than maximum points.");
        }

        UserRank rank = UserRank.builder()
                .name(request.getName())
                .minPoints(request.getMinPoints())
                .maxPoints(request.getMaxPoints())
                .discountRate(request.getDiscountRate())
                .build();

        return mapToResponse(rankRepository.save(rank));
    }

    // BỔ SUNG: Chức năng cập nhật Rank (PATCH/PUT)
    public RankResponse updateRank(UUID rankId, RankRequest request) {
        UserRank existingRank = rankRepository.findById(rankId)
                .orElseThrow(() -> new ResourceNotFoundException("Rank not found with id: " + rankId));

        if (request.getName() != null)
            existingRank.setName(request.getName());

        if (request.getMinPoints() != null) {
            existingRank.setMinPoints(request.getMinPoints());
        }

        if (request.getMaxPoints() != null) {
            existingRank.setMaxPoints(request.getMaxPoints());
        }

        if (request.getDiscountRate() != null) {
            existingRank.setDiscountRate(request.getDiscountRate());
        }

        // Cần kiểm tra lại ràng buộc minPoints < maxPoints sau khi cập nhật
        if (existingRank.getMaxPoints() != null && existingRank.getMinPoints() >= existingRank.getMaxPoints()) {
            throw new IllegalArgumentException("Cập nhật thất bại: Minimum points phải nhỏ hơn maximum points.");
        }

        return mapToResponse(rankRepository.save(existingRank));
    }

    @Transactional(readOnly = true)
    public List<RankResponse> getAllRanks() {
        return rankRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RankResponse getRankById(UUID rankId) {
        return rankRepository.findById(rankId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Rank not found with id: " + rankId));
    }

    public void deleteRank(UUID rankId) {
        UserRank existingRank = rankRepository.findById(rankId)
                .orElseThrow(() -> new ResourceNotFoundException("Rank not found with id: " + rankId));
        rankRepository.delete(existingRank);
    }

    @Transactional(readOnly = true)
    public Optional<UserRank> findDefaultRank() {
        return rankRepository.findByMinPoints(0);
    }

    // Hàm tìm Rank phù hợp với điểm số
    @Transactional(readOnly = true)
    public Optional<UserRank> findRankByLoyaltyPoint(Integer points) {
        return rankRepository.findBestRankByPoints(points);
    }

    private RankResponse mapToResponse(UserRank entity) {
        if (entity == null)
            return null;

        return RankResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .minPoints(entity.getMinPoints())
                .maxPoints(entity.getMaxPoints())
                .discountRate(entity.getDiscountRate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}