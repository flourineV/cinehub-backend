package com.cinehub.promotion.repository;

import com.cinehub.promotion.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    /**
     * Tìm mã khuyến mãi hợp lệ:
     * - Mã code phải khớp
     * - Ngày hiện tại phải nằm giữa start_date và end_date
     * - is_active phải là TRUE
     */
    @Query("SELECT p FROM Promotion p WHERE p.code = :code " +
            "AND p.startDate <= :now AND p.endDate >= :now AND p.isActive = TRUE")
    Optional<Promotion> findValidPromotionByCode(String code, LocalDateTime now);

    // -----------------------------------------------------------------
    // PHƯƠNG THỨC THIẾU CẦN BỔ SUNG: Kiểm tra trùng lặp (dành cho CRUD)
    // -----------------------------------------------------------------

    /**
     * Tìm kiếm một Promotion bằng code bất kể trạng thái hay ngày hết hạn.
     * Được sử dụng để kiểm tra trùng lặp trong logic tạo/cập nhật (CRUD).
     */
    Optional<Promotion> findByCode(String code);
}