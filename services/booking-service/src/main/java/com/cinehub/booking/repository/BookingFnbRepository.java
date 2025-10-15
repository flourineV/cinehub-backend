package com.cinehub.booking.repository;

import com.cinehub.booking.entity.BookingFnb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

public interface BookingFnbRepository extends JpaRepository<BookingFnb, UUID> {

    /**
     * Xóa tất cả các bản ghi BookingFnb liên kết với một Booking ID cụ thể.
     * Tương tự, sử dụng convention deleteBy[Tên field Booking]_[Primary Key của
     * Booking]
     */
    @Transactional
    void deleteByBooking_Id(UUID bookingId);

    /**
     * Tìm tất cả BookingFnb dựa trên Booking ID.
     * Hữu ích cho việc lấy chi tiết F&B khi map DTO hoặc kiểm tra.
     */
    List<BookingFnb> findByBooking_Id(UUID bookingId);
}