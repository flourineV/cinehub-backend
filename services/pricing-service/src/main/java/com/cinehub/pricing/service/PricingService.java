package com.cinehub.pricing.service;

import com.cinehub.pricing.dto.request.SeatPriceRequest;
import com.cinehub.pricing.dto.response.SeatPriceResponse;
import com.cinehub.pricing.entity.SeatPrice;
import com.cinehub.pricing.repository.SeatPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService {

    private final SeatPriceRepository seatPriceRepository;

    // --- Phương thức tra cứu giá (giữ nguyên) ---
    public SeatPriceResponse getSeatBasePrice(String seatType, String ticketType) {
        // ... (Logic giữ nguyên)
        Optional<SeatPrice> optionalPrice = seatPriceRepository.findBySeatTypeAndTicketType(seatType, ticketType);

        if (optionalPrice.isEmpty()) {
            log.warn("❌ Price not found for SeatType: {} and TicketType: {}. Returning null.", seatType, ticketType);
            return null;
        }

        SeatPrice seatPrice = optionalPrice.get();
        return mapToResponse(seatPrice);
    }

    // ---------------------------------------------------------------------
    // ⬇️ PHƯƠNG THỨC QUẢN LÝ (ADMIN CRUD) ⬇️
    // ---------------------------------------------------------------------

    /**
     * Tạo mới một mức giá.
     */
    @Transactional
    public SeatPriceResponse createSeatPrice(SeatPriceRequest request) {
        // Kiểm tra xem mức giá đã tồn tại chưa để tránh lỗi UNIQUE CONSTRAINT
        if (seatPriceRepository.findBySeatTypeAndTicketType(request.getSeatType(), request.getTicketType())
                .isPresent()) {
            throw new IllegalArgumentException("Price for SeatType " + request.getSeatType() + " and TicketType "
                    + request.getTicketType() + " already exists.");
        }

        SeatPrice seatPrice = mapToEntity(request);
        SeatPrice savedPrice = seatPriceRepository.save(seatPrice);
        log.info("💰 Created new seat price: {}", savedPrice.getId());
        return mapToResponse(savedPrice);
    }

    /**
     * Cập nhật mức giá hiện có.
     */
    @Transactional
    public SeatPriceResponse updateSeatPrice(UUID id, SeatPriceRequest request) {
        SeatPrice existingPrice = seatPriceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SeatPrice not found with ID: " + id));

        // Cập nhật các trường
        existingPrice.setBasePrice(request.getBasePrice());
        existingPrice.setDescription(request.getDescription());

        // Lưu ý: Không cho phép thay đổi seatType và ticketType sau khi tạo để giữ tính
        // toàn vẹn của khóa.
        // Hoặc bạn có thể thêm logic kiểm tra nếu thay đổi.

        SeatPrice updatedPrice = seatPriceRepository.save(existingPrice);
        log.info("🔄 Updated seat price: {}", updatedPrice.getId());
        return mapToResponse(updatedPrice);
    }

    /**
     * Xóa một mức giá.
     */
    @Transactional
    public void deleteSeatPrice(UUID id) {
        if (!seatPriceRepository.existsById(id)) {
            throw new IllegalArgumentException("SeatPrice not found with ID: " + id);
        }
        seatPriceRepository.deleteById(id);
        log.warn("🗑️ Deleted seat price: {}", id);
    }

    /**
     * Lấy tất cả các mức giá hiện có.
     */
    public List<SeatPriceResponse> getAllSeatPrices() {
        return seatPriceRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    // --- Mapper Helpers ---

    private SeatPriceResponse mapToResponse(SeatPrice seatPrice) {
        return SeatPriceResponse.builder()
                .seatType(seatPrice.getSeatType())
                .ticketType(seatPrice.getTicketType())
                .basePrice(seatPrice.getBasePrice())
                .build();
    }

    private SeatPrice mapToEntity(SeatPriceRequest request) {
        return SeatPrice.builder()
                .seatType(request.getSeatType())
                .ticketType(request.getTicketType())
                .basePrice(request.getBasePrice())
                .description(request.getDescription())
                .build();
    }
}