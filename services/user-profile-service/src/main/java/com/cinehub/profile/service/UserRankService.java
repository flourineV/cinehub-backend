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

    // Giả định UserRankRepository có phương thức findByMinPoints(0)

    // --------------------------------------------------------------------------------
    // CRUD Operations
    // --------------------------------------------------------------------------------

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

        // Cập nhật từng phần (PATCH semantics)
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
        UserRank rank = rankRepository.findById(rankId)
                .orElseThrow(() -> new ResourceNotFoundException("Rank not found with id: " + rankId));
        return mapToResponse(rank);
    }

    public void deleteRank(UUID rankId) {
        // Kiểm tra sự tồn tại (để ném 404 tùy chỉnh)
        if (!rankRepository.existsById(rankId)) {
            throw new ResourceNotFoundException("Rank not found with id: " + rankId);
        }
        // Lưu ý: Nếu có User đang tham chiếu Rank này, database sẽ ném lỗi Foreign Key.
        // Logic nghiệp vụ cần đảm bảo Rank không còn được sử dụng trước khi xóa.
        rankRepository.deleteById(rankId);
    }

    // --------------------------------------------------------------------------------
    // Business Logic Helpers (Phục vụ UserProfileService)
    // --------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<UserRank> findDefaultRank() {
        // Giả định Rank mặc định là Rank thấp nhất, thường có minPoints = 0
        // Phương thức này cần được định nghĩa trong UserRankRepository
        return rankRepository.findByMinPoints(0);
    }

    // BỔ SUNG: Hàm tìm Rank phù hợp với điểm số (Dùng cho logic Nâng Rank)
    @Transactional(readOnly = true)
    public Optional<UserRank> findRankByLoyaltyPoint(Integer points) {
        // Phương thức này tìm Rank có min_points <= điểm, sắp xếp giảm dần và lấy Rank
        // đầu tiên
        // Bạn sẽ cần định nghĩa một truy vấn tùy chỉnh trong Repository:
        // @Query("SELECT r FROM UserRank r WHERE r.minPoints <= :points ORDER BY
        // r.minPoints DESC")
        // Optional<UserRank> findBestRankByPoints(Integer points);
        return rankRepository.findBestRankByPoints(points);
    }

    // --------------------------------------------------------------------------------
    // Mapping
    // --------------------------------------------------------------------------------

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