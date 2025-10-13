package com.cinehub.fnb.repository;

import com.cinehub.fnb.entity.FnbItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FnbItemRepository extends JpaRepository<FnbItem, UUID> {

    /**
     * Phương thức tùy chỉnh để tra cứu nhiều mục F&B bằng ID.
     * Cần thiết cho việc tính toán tổng tiền khi Booking Service gửi danh sách ID.
     */
    List<FnbItem> findAllByIdIn(List<UUID> ids);
}