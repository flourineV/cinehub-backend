package com.cinehub.booking.service;

import com.cinehub.booking.dto.external.SeatPriceResponse;
import com.cinehub.booking.dto.external.PromotionValidationResponse;
import com.cinehub.booking.dto.external.FnbItemResponse;
import com.cinehub.booking.dto.request.FinalizeBookingRequest;
import com.cinehub.booking.dto.response.BookingResponse;
import com.cinehub.booking.dto.response.BookingSeatResponse;
import com.cinehub.booking.entity.*;
import com.cinehub.booking.events.booking.BookingCreatedEvent;
import com.cinehub.booking.events.booking.BookingStatusUpdatedEvent;
import com.cinehub.booking.events.booking.BookingSeatMappedEvent; // ✅ Dùng DTO đúng tên
import com.cinehub.booking.events.showtime.SeatLockedEvent;
import com.cinehub.booking.events.showtime.SeatUnlockedEvent;
import com.cinehub.booking.events.payment.PaymentCompletedEvent; // Giữ lại cho tương thích
import com.cinehub.booking.events.payment.PaymentFailedEvent; // Giữ lại cho tương thích
import com.cinehub.booking.exception.BookingException;
import com.cinehub.booking.exception.BookingNotFoundException;
import com.cinehub.booking.producer.BookingProducer;
import com.cinehub.booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatusCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

        private final BookingRepository bookingRepository;
        private final UsedPromotionRepository usedPromotionRepository;
        private final BookingPromotionRepository bookingPromotionRepository;
        private final BookingSeatRepository bookingSeatRepository;
        private final BookingFnbRepository bookingFnbRepository;

        @Qualifier("pricingWebClient")
        private final WebClient pricingWebClient;
        @Qualifier("promotionWebClient")
        private final WebClient promotionWebClient;
        @Qualifier("fnbWebClient")
        private final WebClient fnbWebClient;

        private final BookingProducer bookingProducer;

        // =======================================================================
        // 1. EVENT HANDLERS
        // =======================================================================

        @Transactional
        public void handleSeatLocked(SeatLockedEvent data) {
                log.info("Received SeatLocked event: showtime={}, seats={}, user={}",
                                data.showtimeId(), data.selectedSeats().size(), data.userId());

                Booking booking = Booking.builder()
                                .userId(data.userId())
                                .showtimeId(data.showtimeId())
                                .status(BookingStatus.PENDING)
                                .totalPrice(BigDecimal.ZERO)
                                .discountAmount(BigDecimal.ZERO)
                                .finalPrice(BigDecimal.ZERO)
                                .build();

                // 1. GỌI API BẰNG REACTIVE (Không dùng .block() trong vòng lặp)
                List<Mono<SeatPriceResponse>> pricingMonos = data.selectedSeats().stream()
                                .map(seatDetail -> pricingWebClient.get()
                                                .uri(uriBuilder -> uriBuilder.path("/api/pricing/seat-price")
                                                                .queryParam("seatType", seatDetail.getSeatType())
                                                                .queryParam("ticketType", seatDetail.getTicketType())
                                                                .build())
                                                .retrieve()
                                                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                                                        log.error("Pricing API error: {}/{}", seatDetail.getSeatType(),
                                                                        seatDetail.getTicketType());
                                                        return Mono.error(new BookingException(
                                                                        "Không tìm thấy mức giá cho loại ghế/vé này."));
                                                })
                                                .bodyToMono(SeatPriceResponse.class))
                                .toList();

                // 2. CHỜ TẤT CẢ KẾT QUẢ (Khối đồng bộ lớn hơn)
                List<SeatPriceResponse> seatPrices = Mono.zip(pricingMonos, responses -> (List<SeatPriceResponse>) List
                                .of(responses).stream().map(o -> (SeatPriceResponse) o).toList()).block(); // Vẫn cần
                                                                                                           // block ở
                                                                                                           // đây vì hàm
                                                                                                           // là
                                                                                                           // @Transactional
                                                                                                           // synchronous

                if (seatPrices == null || seatPrices.size() != data.selectedSeats().size()) {
                        throw new BookingException("Lỗi trong quá trình lấy giá ghế. Không đủ dữ liệu.");
                }

                // 3. TẠO BookingSeat và TÍNH TỔNG TIỀN
                List<BookingSeat> seats = new ArrayList<>();
                BigDecimal totalSeatPrice = BigDecimal.ZERO;

                for (int i = 0; i < data.selectedSeats().size(); i++) {
                        var seatDetail = data.selectedSeats().get(i);
                        SeatPriceResponse seatPrice = seatPrices.get(i);

                        if (seatPrice == null || seatPrice.getBasePrice() == null) {
                                throw new BookingException("Không tìm thấy mức giá cho loại ghế/vé này.");
                        }

                        BigDecimal price = seatPrice.getBasePrice();
                        totalSeatPrice = totalSeatPrice.add(price);

                        seats.add(BookingSeat.builder()
                                        .seatId(seatDetail.getSeatId())
                                        .seatType(seatDetail.getSeatType())
                                        .ticketType(seatDetail.getTicketType())
                                        .price(price)
                                        .createdAt(LocalDateTime.now())
                                        .booking(booking)
                                        .build());
                }

                // 4. LƯU VÀ CẬP NHẬT BOOKING
                booking.setSeats(seats);
                booking.setTotalPrice(totalSeatPrice);
                booking.setFinalPrice(totalSeatPrice); // Ban đầu FinalPrice = TotalPrice
                bookingRepository.save(booking);

                log.info("✅ Booking created: {} | total={} | seats={}",
                                booking.getId(), totalSeatPrice, seats.size());

                // 5. GỬI EVENTS

                // Event 1: BookingCreated (Cho Payment/Notification)
                bookingProducer.sendBookingCreatedEvent(
                                new BookingCreatedEvent(
                                                booking.getId(),
                                                booking.getUserId(),
                                                booking.getShowtimeId(),
                                                booking.getSeats().stream().map(BookingSeat::getSeatId).toList(),
                                                booking.getTotalPrice()));

                // Event 2: BookingSeatMapped (Cho Showtime - Ánh xạ Booking ID vào Redis Lock)
                bookingProducer.sendBookingSeatMappedEvent( // ✅ SỬA: Dùng BookingSeatMappedEvent
                                new BookingSeatMappedEvent(
                                                // ✅ THAM SỐ 1: bookingId
                                                booking.getId(),
                                                // ✅ THAM SỐ 2: showtimeId
                                                booking.getShowtimeId(),
                                                // ✅ THAM SỐ 3: seatIds
                                                booking.getSeats().stream().map(BookingSeat::getSeatId).toList(),
                                                // ✅ THAM SỐ 4: userId
                                                booking.getUserId()));
        }

        @Transactional
        public void handleSeatUnlocked(SeatUnlockedEvent data) {
                log.warn("Received SeatUnlocked event: bookingId={}, seats={}, reason={}",
                                data.bookingId(), data.seatIds().size(), data.reason());

                UUID bookingId = data.bookingId();
                if (bookingId == null) {
                        log.error("SeatUnlockedEvent received without bookingId. Cannot update status.");
                        return;
                }

                Booking booking = bookingRepository.findById(bookingId).orElse(null);

                // ✅ SỬA: CHỈ XỬ LÝ KHI booking TỒN TẠI và đang ở trạng thái CÓ THỂ BỊ HẾT HẠN
                if (booking == null || (booking.getStatus() != BookingStatus.PENDING
                                && booking.getStatus() != BookingStatus.AWAITING_PAYMENT)) {
                        log.warn("Booking {} not found or status is {}. Skipping unlock handler.",
                                        bookingId, booking != null ? booking.getStatus() : "N/A");
                        return;
                }

                // Đặt trạng thái là EXPIRED (vì sự kiện này thường do TTL của Redis lock hết
                // hạn)
                updateBookingStatus(booking, BookingStatus.EXPIRED);
        }

        @Transactional
        public void handlePaymentCompleted(PaymentCompletedEvent data) {
                log.info("Received PaymentCompleted event for booking: {}", data.bookingId());

                Booking booking = bookingRepository.findById(data.bookingId()).orElse(null);
                if (booking == null || (booking.getStatus() != BookingStatus.PENDING
                                && booking.getStatus() != BookingStatus.AWAITING_PAYMENT)) {
                        log.warn("Booking {} not found or status is not PENDING/AWAITING_PAYMENT. Current status: {}",
                                        data.bookingId(), booking != null ? booking.getStatus() : "N/A");
                        return;
                }

                // Cập nhật thông tin thanh toán
                booking.setPaymentMethod(data.method());
                booking.setTransactionId(data.transactionRef());

                // Cập nhật trạng thái CONFIRMED và gửi Event
                updateBookingStatus(booking, BookingStatus.CONFIRMED);
        }

        @Transactional
        public void handlePaymentFailed(PaymentFailedEvent data) {
                log.error("Received PaymentFailed event for booking: {} | Reason: {}", data.bookingId(), data.reason());

                Booking booking = bookingRepository.findById(data.bookingId()).orElse(null);
                if (booking == null || (booking.getStatus() != BookingStatus.PENDING
                                && booking.getStatus() != BookingStatus.AWAITING_PAYMENT)) {
                        log.warn("Booking {} not found or status is not PENDING/AWAITING_PAYMENT. Skipping failure handler.",
                                        data.bookingId());
                        return;
                }

                // Cập nhật trạng thái CANCELLED và gửi Event
                updateBookingStatus(booking, BookingStatus.CANCELLED);
        }

        // =======================================================================
        // 2. CORE BUSINESS LOGIC
        // =======================================================================

        @Transactional
        public BookingResponse finalizeBooking(UUID bookingId, FinalizeBookingRequest request) {
                // ... (Logic finalizeBooking giữ nguyên)
                // Lưu ý: Logic này chuyển trạng thái sang AWAITING_PAYMENT

                // 1. Tìm Booking và kiểm tra trạng thái
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

                if (booking.getStatus() != BookingStatus.PENDING) {
                        throw new BookingException("Booking đã được thanh toán hoặc hết hạn.");
                }

                BigDecimal fnbPrice = BigDecimal.ZERO;

                // 2. Xử lý F&B
                if (request.fnbItems() != null && !request.fnbItems().isEmpty()) {
                        bookingFnbRepository.deleteByBooking_Id(bookingId);
                        fnbPrice = processFnbItems(booking, request.fnbItems());
                }

                // 3. Cập nhật Total Price (Giá ghế + Giá F&B)
                BigDecimal seatPrice = booking.getSeats().stream()
                                .map(BookingSeat::getPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal newTotalPrice = seatPrice.add(fnbPrice);

                booking.setTotalPrice(newTotalPrice);
                booking.setFinalPrice(newTotalPrice);
                booking.setDiscountAmount(BigDecimal.ZERO);

                // 4. Xử lý Khuyến mãi
                if (request.promotionCode() != null && !request.promotionCode().isBlank()) {
                        bookingPromotionRepository.deleteByBooking_Id(bookingId);
                        processPromotion(booking, request.promotionCode());
                }

                // 5. Cập nhật trạng thái chính thức sang AWAITING_PAYMENT
                BookingStatus oldStatus = booking.getStatus();
                booking.setStatus(BookingStatus.AWAITING_PAYMENT); // Trạng thái mới
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);

                log.info("Booking {} finalized: Total Price={}, Final Price={}",
                                bookingId, booking.getTotalPrice(), booking.getFinalPrice());

                // Gửi Event cập nhật trạng thái
                bookingProducer.sendBookingStatusUpdatedEvent(
                                new BookingStatusUpdatedEvent(
                                                booking.getId(),
                                                booking.getShowtimeId(),
                                                booking.getUserId(),
                                                BookingStatus.AWAITING_PAYMENT,
                                                oldStatus.name()));

                return mapToResponse(booking);
        }

        // ... (Giữ nguyên processFnbItems và processPromotion)
        private BigDecimal processFnbItems(Booking booking, List<FinalizeBookingRequest.FnbItemRequest> fnbItems) {
                // ... (Giữ nguyên logic cũ)
                BigDecimal totalFnbPrice = BigDecimal.ZERO;
                List<BookingFnb> bookingFnbs = new ArrayList<>();

                for (var fnbRequest : fnbItems) {
                        FnbItemResponse fnbItem = fnbWebClient.get()
                                        .uri("/api/fnb/{fnbId}", fnbRequest.fnbId())
                                        .retrieve()
                                        .bodyToMono(FnbItemResponse.class)
                                        .block();

                        if (fnbItem == null || fnbItem.getUnitPrice() == null) {
                                throw new BookingException("Không tìm thấy món F&B hoặc thiếu giá.");
                        }

                        BigDecimal unitPrice = fnbItem.getUnitPrice();
                        BigDecimal itemTotalPrice = unitPrice.multiply(BigDecimal.valueOf(fnbRequest.quantity()));

                        totalFnbPrice = totalFnbPrice.add(itemTotalPrice);

                        bookingFnbs.add(BookingFnb.builder()
                                        .fnbItemId(fnbRequest.fnbId())
                                        .unitPrice(unitPrice)
                                        .quantity(fnbRequest.quantity())
                                        .totalFnbPrice(itemTotalPrice)
                                        .booking(booking)
                                        .build());
                }

                bookingFnbRepository.saveAll(bookingFnbs);

                return totalFnbPrice;
        }

        private void processPromotion(Booking booking, String promoCode) {
                // ... (Giữ nguyên logic cũ)
                PromotionValidationResponse validationResponse = promotionWebClient.get()
                                .uri(uriBuilder -> uriBuilder.path("/api/promotion/validate")
                                                .queryParam("code", promoCode)
                                                .queryParam("userId", booking.getUserId())
                                                .queryParam("totalPrice", booking.getTotalPrice())
                                                .build())
                                .retrieve()
                                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                                        return Mono.error(new BookingException(
                                                        "Mã khuyến mãi không hợp lệ hoặc đã hết hạn."));
                                })
                                .bodyToMono(PromotionValidationResponse.class)
                                .block();

                if (validationResponse == null || validationResponse.getDiscountValue() == null
                                || validationResponse.getDiscountType() == null) {
                        throw new BookingException("Lỗi xử lý khuyến mãi: Thiếu thông tin loại hoặc giá trị giảm.");
                }

                BigDecimal totalBeforeDiscount = booking.getTotalPrice();
                BigDecimal discountValue = validationResponse.getDiscountValue();
                DiscountType discountType = validationResponse.getDiscountType();
                BigDecimal calculatedDiscountAmount;

                if (discountType == DiscountType.PERCENTAGE) {
                        calculatedDiscountAmount = totalBeforeDiscount.multiply(discountValue);
                } else if (discountType == DiscountType.FIXED_AMOUNT) {
                        calculatedDiscountAmount = discountValue;
                } else {
                        calculatedDiscountAmount = BigDecimal.ZERO;
                }

                BigDecimal discountAmount = calculatedDiscountAmount.setScale(2, RoundingMode.HALF_UP);
                BigDecimal newFinalPrice = totalBeforeDiscount.subtract(discountAmount);

                if (newFinalPrice.compareTo(BigDecimal.ZERO) < 0) {
                        newFinalPrice = BigDecimal.ZERO;
                        discountAmount = totalBeforeDiscount;
                }

                booking.setDiscountAmount(discountAmount);
                booking.setFinalPrice(newFinalPrice.setScale(2, RoundingMode.HALF_UP));

                BookingPromotion bookingPromotion = BookingPromotion.builder()
                                .promotionCode(promoCode)
                                .discountType(discountType)
                                .discountValue(discountValue)
                                .booking(booking)
                                .build();
                bookingPromotionRepository.save(bookingPromotion);

                usedPromotionRepository.save(UsedPromotion.builder()
                                .userId(booking.getUserId())
                                .promotionCode(promoCode)
                                .usedAt(LocalDateTime.now())
                                .build());
        }

        // =======================================================================
        // 3. COMMON & CRUD
        // =======================================================================

        /**
         * ✅ HÀM MỚI: Cập nhật trạng thái chính thức của Booking và gửi Event.
         */
        @Transactional
        public void updateBookingStatus(UUID bookingId, BookingStatus newStatus) {
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new BookingException("Booking not found: " + bookingId));
                updateBookingStatus(booking, newStatus);
        }

        /**
         * ✅ HÀM MỚI: Cập nhật trạng thái chính thức của Booking và gửi Event.
         * Dùng cho các event handler đã tìm thấy booking.
         */
        @Transactional
        public void updateBookingStatus(Booking booking, BookingStatus newStatus) {

                BookingStatus oldStatus = booking.getStatus();

                // ⚠️ Kiểm tra chuyển đổi trạng thái cần được đặt TẠI ĐÂY nếu cần strictness cao
                // Ví dụ: Không thể chuyển từ CONFIRMED sang PENDING.
                // Hiện tại: Chỉ log cảnh báo nếu status là CONFIRMED (đã hoàn tất)
                if (oldStatus == BookingStatus.CONFIRMED && newStatus != BookingStatus.CONFIRMED) {
                        log.warn("Attempted to update CONFIRMED booking {} from {} to {}. Skipping.",
                                        booking.getId(), oldStatus, newStatus);
                        return;
                }

                // Cập nhật trạng thái mới
                booking.setStatus(newStatus);
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);

                log.info("Status updated: Booking {} from {} to {}.", booking.getId(), oldStatus, newStatus);

                // Logic giải phóng ghế chỉ khi trạng thái chuyển sang CANCELLED/EXPIRED
                if (newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.EXPIRED) {

                        // Lấy danh sách ghế đã đặt
                        List<UUID> seatIds = booking.getSeats().stream()
                                        .map(BookingSeat::getSeatId)
                                        .toList();

                        // Gửi sự kiện giải phóng ghế trong Showtime Service
                        bookingProducer.sendSeatUnlockedEvent(
                                        new SeatUnlockedEvent(
                                                        booking.getShowtimeId(),
                                                        // ✅ ĐÃ SỬA: bookingId phải đứng thứ 2
                                                        booking.getId(),
                                                        seatIds,
                                                        newStatus.name()));
                }

                // Gửi Event cho Showtime/các dịch vụ khác
                bookingProducer.sendBookingStatusUpdatedEvent(
                                new BookingStatusUpdatedEvent(
                                                booking.getId(),
                                                booking.getShowtimeId(),
                                                booking.getUserId(),
                                                newStatus,
                                                oldStatus.name()));
        }

        // ... (Giữ nguyên các hàm CRUD khác)
        public BookingResponse getBookingById(UUID id) {
                Booking booking = bookingRepository.findById(id)
                                .orElseThrow(() -> new BookingException("Booking not found: " + id)); // Dùng
                                                                                                      // BookingException
                return mapToResponse(booking);
        }

        public List<BookingResponse> getBookingsByUser(UUID userId) {
                return bookingRepository.findByUserId(userId).stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        @Transactional
        public void deleteBooking(UUID id) {
                bookingRepository.deleteById(id);
        }

        // Trong BookingService.java
        private BookingSeatResponse mapToSeatResponse(BookingSeat seat) {
                return BookingSeatResponse.builder()
                                .seatId(seat.getSeatId())
                                .seatType(seat.getSeatType())
                                .ticketType(seat.getTicketType())
                                .price(seat.getPrice())
                                .build();
        }

        // Helper mapper (Giữ nguyên)
        private BookingResponse mapToResponse(Booking booking) {

                // Ánh xạ danh sách ghế
                List<BookingSeatResponse> seatResponses = booking.getSeats().stream()
                                .map(this::mapToSeatResponse)
                                .toList();

                return BookingResponse.builder()
                                .bookingId(booking.getId())
                                .userId(booking.getUserId())
                                .showtimeId(booking.getShowtimeId())
                                .status(booking.getStatus().name())
                                .totalPrice(booking.getTotalPrice())
                                .discountAmount(booking.getDiscountAmount())
                                .finalPrice(booking.getFinalPrice())
                                .createdAt(booking.getCreatedAt())
                                .updatedAt(booking.getUpdatedAt())
                                .seats(seatResponses) // ✅ THÊM DANH SÁCH GHẾ
                                .build();
        }
}