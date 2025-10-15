package com.cinehub.booking.repository;

import com.cinehub.booking.entity.UsedPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UsedPromotionRepository extends JpaRepository<UsedPromotion, UUID> {

    /**
     * Phương thức dùng để kiểm tra xem một người dùng đã sử dụng một mã khuyến mãi
     * giới hạn (is_one_time_use=TRUE) cụ thể hay chưa.
     */
    Optional<UsedPromotion> findByUserIdAndPromotionCode(UUID userId, String promotionCode);

    // Lưu ý: Nhờ DDL (UNIQUE INDEX) và Entity, DB sẽ đảm bảo tính duy nhất.
}