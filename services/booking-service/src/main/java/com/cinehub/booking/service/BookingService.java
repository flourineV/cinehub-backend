package com.cinehub.booking.service;

import com.cinehub.booking.dto.external.SeatPriceResponse;
import com.cinehub.booking.dto.external.ShowtimeResponse;
import com.cinehub.booking.dto.external.PromotionValidationResponse;
import com.cinehub.booking.dto.external.FnbCalculationResponse;
import com.cinehub.booking.dto.request.FinalizeBookingRequest;
import com.cinehub.booking.dto.response.BookingResponse;
import com.cinehub.booking.dto.response.BookingSeatResponse;
import com.cinehub.booking.dto.external.FnbCalculationRequest;
import com.cinehub.booking.dto.external.MovieSimpleResponse;
import com.cinehub.booking.dto.external.SeatResponse;
import com.cinehub.booking.dto.external.FnbItemResponse;
import com.cinehub.booking.dto.external.RankAndDiscountResponse;

import com.cinehub.booking.entity.*;
import com.cinehub.booking.events.booking.*;
import com.cinehub.booking.events.notification.BookingTicketGeneratedEvent;
import com.cinehub.booking.events.notification.FnbDetail;
import com.cinehub.booking.events.notification.PromotionDetail;
import com.cinehub.booking.events.notification.SeatDetail;
import com.cinehub.booking.events.showtime.*;
import com.cinehub.booking.events.payment.*;
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
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

        private final BookingRepository bookingRepository;
        private final UsedPromotionRepository usedPromotionRepository;
        private final BookingPromotionRepository bookingPromotionRepository;
        private final BookingFnbRepository bookingFnbRepository;

        @Qualifier("pricingWebClient")
        private final WebClient pricingWebClient;
        @Qualifier("promotionWebClient")
        private final WebClient promotionWebClient;
        @Qualifier("fnbWebClient")
        private final WebClient fnbWebClient;
        @Qualifier("showtimeWebClient")
        private final WebClient showtimeWebClient;
        @Qualifier("movieWebClient")
        private final WebClient movieWebClient;
        @Qualifier("userProfileWebClient")
        private final WebClient userProfileWebClient;

        private final BookingProducer bookingProducer;

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

                List<SeatPriceResponse> seatPrices = Mono.zip(pricingMonos, responses -> (List<SeatPriceResponse>) List
                                .of(responses).stream().map(o -> (SeatPriceResponse) o).toList()).block();

                if (seatPrices == null || seatPrices.size() != data.selectedSeats().size()) {
                        throw new BookingException("Lỗi trong quá trình lấy giá ghế. Không đủ dữ liệu.");
                }

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

                log.info("Booking created: {} | total={} | seats={}",
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
                bookingProducer.sendBookingSeatMappedEvent(
                                new BookingSeatMappedEvent(
                                                booking.getId(),
                                                booking.getShowtimeId(),
                                                booking.getSeats().stream().map(BookingSeat::getSeatId).toList(),
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

                if (booking == null || (booking.getStatus() != BookingStatus.PENDING
                                && booking.getStatus() != BookingStatus.AWAITING_PAYMENT)) {
                        log.warn("Booking {} not found or status is {}. Skipping unlock handler.",
                                        bookingId, booking != null ? booking.getStatus() : "N/A");
                        return;
                }

                bookingFnbRepository.deleteByBooking_Id(booking.getId());
                bookingPromotionRepository.deleteByBooking_Id(booking.getId());
                usedPromotionRepository.deleteByBooking_Id(booking.getId());

                updateBookingStatus(booking, BookingStatus.EXPIRED);
        }

        @Transactional
        public void handlePaymentSuccess(PaymentSuccessEvent data) {

                log.info("Received PaymentCompleted event for booking: {}", data.bookingId());

                Booking booking = bookingRepository.findById(data.bookingId()).orElse(null);
                if (booking == null || (booking.getStatus() != BookingStatus.PENDING
                                && booking.getStatus() != BookingStatus.AWAITING_PAYMENT)) {
                        log.warn("Booking {} not found or status is not PENDING/AWAITING_PAYMENT. Current status: {}",
                                        data.bookingId(), booking != null ? booking.getStatus() : "N/A");
                        return;
                }

                booking.setPaymentMethod(data.method());
                booking.setPaymentId(data.paymentId());

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

                bookingFnbRepository.deleteByBooking_Id(booking.getId());
                bookingPromotionRepository.deleteByBooking_Id(booking.getId());
                usedPromotionRepository.deleteByBooking_Id(booking.getId());

                // Cập nhật trạng thái CANCELLED và gửi Event
                updateBookingStatus(booking, BookingStatus.CANCELLED);
        }

        @Transactional
        public BookingResponse finalizeBooking(UUID bookingId, FinalizeBookingRequest request) {

                RankAndDiscountResponse rankAndDiscountResponse = userProfileWebClient.get()
                                .uri("/api/profiles/rank/")
                                .retrieve()
                                .bodyToMono(FnbCalculationResponse.class)
                                .block();

                UUID userByBooking = bookingRepository.findById(bookingId).orElseThrow().getUserId();

                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

                if (booking.getStatus() != BookingStatus.PENDING) {
                        throw new BookingException("Booking đã được thanh toán hoặc hết hạn.");
                }

                BigDecimal fnbPrice = BigDecimal.ZERO;

                // 2. Xử lý F&B
                if (request.getFnbItems() != null && !request.getFnbItems().isEmpty()) {
                        bookingFnbRepository.deleteByBooking_Id(bookingId);
                        fnbPrice = processFnbItems(booking, request.getFnbItems());
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
                if (request.getPromotionCode() != null && !request.getPromotionCode().isBlank()) {
                        bookingPromotionRepository.deleteByBooking_Id(bookingId);
                        processPromotion(booking, request.getPromotionCode());
                }

                // 5. Cập nhật trạng thái chính thức sang AWAITING_PAYMENT
                BookingStatus oldStatus = booking.getStatus();
                booking.setStatus(BookingStatus.AWAITING_PAYMENT); // Trạng thái mới
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);

                log.info("Booking {} finalized: Total Price={}, Final Price={}",
                                bookingId, booking.getTotalPrice(), booking.getFinalPrice());
                bookingProducer.sendBookingFinalizedEvent(
                                new BookingFinalizedEvent(
                                                booking.getId(),
                                                booking.getUserId(),
                                                booking.getShowtimeId(),
                                                booking.getFinalPrice()));

                return mapToResponse(booking);
        }

        private BigDecimal processFnbItems(Booking booking,
                        List<FinalizeBookingRequest.CalculatedFnbItemDto> fnbItems) {
                FnbCalculationRequest fnbRequest = new FnbCalculationRequest();
                fnbRequest.setSelectedFnbItems(fnbItems);

                FnbCalculationResponse fnbResponse = fnbWebClient.post()
                                .uri("/api/fnb/calculate")
                                .bodyValue(fnbRequest)
                                .retrieve()
                                .bodyToMono(FnbCalculationResponse.class)
                                .block();

                if (fnbResponse == null || fnbResponse.getCalculatedFnbItems() == null) {
                        throw new BookingException("❌ Không nhận được dữ liệu F&B từ service FNB.");
                }
                BigDecimal totalFnbPrice = fnbResponse.getTotalFnbPrice();
                List<FinalizeBookingRequest.CalculatedFnbItemDto> calculatedItems = fnbResponse.getCalculatedFnbItems();

                List<BookingFnb> bookingFnbs = new ArrayList<>();

                for (var item : calculatedItems) {
                        BigDecimal unitPrice = item.getUnitPrice();
                        BigDecimal itemTotalPrice = item.getTotalFnbItemPrice();

                        bookingFnbs.add(BookingFnb.builder()
                                        .fnbItemId(item.getFnbItemId())
                                        .unitPrice(unitPrice)
                                        .quantity(item.getQuantity())
                                        .totalFnbPrice(itemTotalPrice)
                                        .booking(booking)
                                        .build());
                }

                // 💾 Lưu danh sách FNB vào DB
                bookingFnbRepository.saveAll(bookingFnbs);

                return totalFnbPrice;
        }

        private void processPromotion(Booking booking, String promoCode) {
                // ... (Giữ nguyên logic cũ)
                PromotionValidationResponse validationResponse = promotionWebClient.get()
                                .uri(uriBuilder -> uriBuilder.path("/api/promotions/validate")
                                                .queryParam("code", promoCode)
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
                                .booking(booking)
                                .usedAt(LocalDateTime.now())
                                .build());
        }

        @Transactional
        public void updateBookingStatus(UUID bookingId, BookingStatus newStatus) {
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new BookingException("Booking not found: " + bookingId));
                updateBookingStatus(booking, newStatus);
        }

        @Transactional
        public void updateBookingStatus(Booking booking, BookingStatus newStatus) {

                BookingStatus oldStatus = booking.getStatus();

                if (oldStatus == BookingStatus.CONFIRMED && newStatus != BookingStatus.CONFIRMED) {
                        log.warn("Attempted to update CONFIRMED booking {} from {} to {}. Skipping.",
                                        booking.getId(), oldStatus, newStatus);
                        return;
                }

                booking.setStatus(newStatus);
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);

                log.info("Status updated: Booking {} from {} to {}.", booking.getId(), oldStatus, newStatus);

                List<UUID> seatIds = booking.getSeats().stream()
                                .map(BookingSeat::getSeatId)
                                .toList();
                if (newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.EXPIRED) {

                        // Gửi sự kiện giải phóng ghế trong Showtime Service
                        bookingProducer.sendSeatUnlockedEvent(
                                        new SeatUnlockedEvent(
                                                        booking.getShowtimeId(),
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
                                                seatIds,
                                                newStatus.toString(),
                                                oldStatus.name()));
        }

        @Transactional
        private BookingTicketGeneratedEvent buildBookingTicketGeneratedEvent(Booking booking) {

                ShowtimeResponse showtime = showtimeWebClient.get()
                                .uri("/api/showtimes/{id}", booking.getShowtimeId())
                                .retrieve()
                                .bodyToMono(ShowtimeResponse.class)
                                .block();

                if (showtime == null) {
                        throw new BookingException("Không thể lấy thông tin suất chiếu cho booking " + booking.getId());
                }

                MovieSimpleResponse movie = movieWebClient.get()
                                .uri("/api/movies/{id}", showtime.getMovieId())
                                .retrieve()
                                .bodyToMono(MovieSimpleResponse.class)
                                .block();

                if (movie == null) {
                        throw new BookingException("Không thể lấy thông tin phim cho booking " + booking.getId());
                }

                AtomicReference<String> roomNameRef = new AtomicReference<>("");

                List<SeatDetail> seatDetails = booking.getSeats().stream()
                                .map(seat -> {
                                        SeatResponse seatInfo = showtimeWebClient.get()
                                                        .uri("/api/showtimes/seats/{id}", seat.getSeatId())
                                                        .retrieve()
                                                        .bodyToMono(SeatResponse.class)
                                                        .block();

                                        if (seatInfo == null) {
                                                throw new BookingException(
                                                                "Không tìm thấy thông tin ghế " + seat.getSeatId());
                                        }

                                        roomNameRef.set(seatInfo.getRoomName());
                                        String seatName = seatInfo.getSeatNumber();

                                        return new SeatDetail(
                                                        seatName,
                                                        seat.getSeatType(),
                                                        seat.getTicketType(),
                                                        1,
                                                        seat.getPrice());
                                })
                                .toList();

                List<BookingFnb> bookingFnbs = bookingFnbRepository.findByBooking_Id(booking.getId());
                List<FnbDetail> fnbDetails = bookingFnbs.stream()
                                .map(fnb -> {
                                        FnbItemResponse fnbInfo = fnbWebClient.get()
                                                        .uri("/api/fnb/{id}", fnb.getFnbItemId())
                                                        .retrieve()
                                                        .bodyToMono(FnbItemResponse.class)
                                                        .block();

                                        String itemName = (fnbInfo != null) ? fnbInfo.getName() : "Unknown Item";
                                        return new FnbDetail(
                                                        itemName,
                                                        fnb.getQuantity(),
                                                        fnb.getUnitPrice(),
                                                        fnb.getTotalFnbPrice());
                                })
                                .toList();
                BookingPromotion promo = bookingPromotionRepository.findByBooking_Id(booking.getId()).orElse(null);
                PromotionDetail promotionDetail = (promo != null)
                                ? new PromotionDetail(promo.getPromotionCode(), booking.getDiscountAmount())
                                : null;

                return new BookingTicketGeneratedEvent(
                                booking.getId(),
                                booking.getUserId(),
                                movie.getTitle(),
                                showtime.getTheaterName(),
                                roomNameRef.get(),
                                showtime.getStartTime().toString(),
                                seatDetails,
                                fnbDetails,
                                promotionDetail,
                                booking.getTotalPrice(),
                                booking.getFinalPrice(),
                                booking.getPaymentMethod(),
                                booking.getCreatedAt());
        }

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