package com.cinehub.booking.service.impl;

import com.cinehub.booking.dto.external.SeatPriceResponse;
import com.cinehub.booking.dto.external.ShowtimeResponse;
import com.cinehub.booking.dto.external.PromotionValidationResponse;
import com.cinehub.booking.dto.external.FnbCalculationResponse;
import com.cinehub.booking.dto.request.FinalizeBookingRequest;
import com.cinehub.booking.dto.request.SeatSelectionDetail;
import com.cinehub.booking.dto.response.BookingResponse;
import com.cinehub.booking.dto.external.FnbCalculationRequest;
import com.cinehub.booking.dto.external.MovieTitleResponse;
import com.cinehub.booking.dto.external.SeatResponse;
import com.cinehub.booking.dto.external.FnbItemResponse;
import com.cinehub.booking.dto.external.RankAndDiscountResponse;
import com.cinehub.booking.dto.request.CreateBookingRequest;

import com.cinehub.booking.entity.*;
import com.cinehub.booking.events.booking.*;
import com.cinehub.booking.events.notification.*;
import com.cinehub.booking.events.showtime.*;
import com.cinehub.booking.events.payment.*;
import com.cinehub.booking.exception.BookingException;
import com.cinehub.booking.exception.BookingNotFoundException;
import com.cinehub.booking.producer.BookingProducer;
import com.cinehub.booking.repository.*;
import com.cinehub.booking.adapter.client.*;
import com.cinehub.booking.service.BookingService;
import com.cinehub.booking.service.SeatLockRedisService;
import com.cinehub.booking.mapper.BookingMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

        private final BookingRepository bookingRepository;
        private final UsedPromotionRepository usedPromotionRepository;
        private final BookingPromotionRepository bookingPromotionRepository;
        private final BookingFnbRepository bookingFnbRepository;

        private final PricingClient pricingClient;
        private final PromotionClient promotionClient;
        private final FnbClient fnbClient;
        private final ShowtimeClient showtimeClient;
        private final MovieClient movieClient;
        private final UserProfileClient userProfileClient;
        private final BookingMapper bookingMapper;
        private final BookingProducer bookingProducer;
        private final SeatLockRedisService seatLockRedisService;

        @Transactional
        public BookingResponse createBooking(CreateBookingRequest request) {

                // ======== 0. VALIDATE REQUEST ========
                if (request.getSelectedSeats() == null || request.getSelectedSeats().isEmpty()) {
                        throw new BookingException("At least one seat must be selected");
                }

                log.info("Creating booking: showtime={}, seats={}, user={}, guest={}",
                                request.getShowtimeId(), request.getSelectedSeats().size(),
                                request.getUserId(), request.getGuestSessionId());

                // ======== 1. VALIDATE OWNERSHIP ========
                if (request.getUserId() == null && request.getGuestSessionId() == null) {
                        throw new BookingException("Either userId or guestSessionId must be provided");
                }

                if (request.getUserId() != null && request.getGuestSessionId() != null) {
                        throw new BookingException("Cannot provide both userId and guestSessionId");
                }

                List<UUID> seatIds = request.getSelectedSeats().stream()
                                .map(SeatSelectionDetail::getSeatId)
                                .toList();

                boolean ownsSeats;
                if (request.getGuestSessionId() != null) {
                        ownsSeats = seatLockRedisService.validateGuestSessionOwnsSeats(
                                        request.getShowtimeId(),
                                        seatIds,
                                        request.getGuestSessionId());
                        if (!ownsSeats) {
                                throw new BookingException("Guest session does not own the selected seats");
                        }
                } else {
                        ownsSeats = seatLockRedisService.validateUserOwnsSeats(
                                        request.getShowtimeId(),
                                        seatIds,
                                        request.getUserId());
                        if (!ownsSeats) {
                                throw new BookingException("User does not own the selected seats");
                        }
                }

                Booking booking = Booking.builder()
                                .userId(request.getUserId())
                                .showtimeId(request.getShowtimeId())
                                .status(BookingStatus.PENDING)
                                .totalPrice(BigDecimal.ZERO)
                                .discountAmount(BigDecimal.ZERO)
                                .finalPrice(BigDecimal.ZERO)
                                .guestName(request.getGuestName())
                                .guestEmail(request.getGuestEmail())
                                .build();

                List<BookingSeat> seats = new ArrayList<>();
                BigDecimal totalSeatPrice = BigDecimal.ZERO;

                for (SeatSelectionDetail seatDetail : request.getSelectedSeats()) {
                        SeatPriceResponse seatPrice = pricingClient.getSeatPrice(
                                        seatDetail.getSeatType(),
                                        seatDetail.getTicketType());

                        if (seatPrice == null || seatPrice.getBasePrice() == null) {
                                throw new BookingException("Cannot get price for seat: " + seatDetail.getSeatId());
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

                booking.setSeats(seats);
                booking.setTotalPrice(totalSeatPrice);
                booking.setFinalPrice(totalSeatPrice);

                // ======== 4. SAVE BOOKING ========
                bookingRepository.save(booking);

                log.info("Booking created: {} | total={} | seats={}",
                                booking.getId(), totalSeatPrice, seats.size());

                // ======== 5. PUBLISH EVENTS ========
                bookingProducer.sendBookingCreatedEvent(
                                new BookingCreatedEvent(
                                                booking.getId(),
                                                booking.getUserId(),
                                                booking.getGuestName(),
                                                booking.getGuestEmail(),
                                                booking.getShowtimeId(),
                                                seatIds,
                                                booking.getTotalPrice()));

                bookingProducer.sendBookingSeatMappedEvent(
                                new BookingSeatMappedEvent(
                                                booking.getId(),
                                                booking.getShowtimeId(),
                                                seatIds,
                                                booking.getUserId(),
                                                booking.getGuestName(),
                                                booking.getGuestEmail()));

                return bookingMapper.toBookingResponse(booking);
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

                updateBookingStatus(booking, BookingStatus.CANCELLED);
        }

        @Transactional
        public BookingResponse finalizeBooking(UUID bookingId, FinalizeBookingRequest request) {

                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

                if (booking.getStatus() != BookingStatus.PENDING) {
                        throw new BookingException("Booking đã được thanh toán hoặc hết hạn.");
                }

                // ======== Tính tổng ban đầu ========
                BigDecimal fnbPrice = BigDecimal.ZERO;

                if (request.getFnbItems() != null && !request.getFnbItems().isEmpty()) {
                        bookingFnbRepository.deleteByBooking_Id(bookingId);
                        fnbPrice = processFnbItems(booking, request.getFnbItems());
                }

                BigDecimal seatPrice = booking.getSeats().stream()
                                .map(BookingSeat::getPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalPrice = seatPrice.add(fnbPrice);
                booking.setTotalPrice(totalPrice);

                BigDecimal discountAmount = BigDecimal.ZERO;
                BigDecimal finalPrice = totalPrice;

                // ======== Áp dụng giảm giá (mutually exclusive) ========

                // Ưu tiên: Promotion code
                if (request.getPromotionCode() != null && !request.getPromotionCode().isBlank()) {

                        bookingPromotionRepository.deleteByBooking_Id(bookingId);
                        processPromotion(booking, request.getPromotionCode());

                        // processPromotion() đã tự set discountAmount + finalPrice
                        log.info("Applied promotion code: {}", request.getPromotionCode());

                }
                // Nếu không có promotion → dùng refund voucher
                else if (request.getRefundVoucherCode() != null && !request.getRefundVoucherCode().isBlank()) {

                        var voucher = promotionClient.markRefundVoucherAsUsed(request.getRefundVoucherCode());

                        if (voucher == null) {
                                throw new BookingException("Không thể sử dụng voucher hoàn tiền.");
                        }

                        if (voucher.getUserId() != null && !voucher.getUserId().equals(booking.getUserId())) {
                                throw new BookingException("Voucher không thuộc về người dùng.");
                        }
                        if (Boolean.TRUE.equals(voucher.getIsUsed())) {
                                throw new BookingException("Voucher đã được sử dụng.");
                        }
                        if (voucher.getExpiredAt() != null && voucher.getExpiredAt().isBefore(LocalDateTime.now())) {
                                throw new BookingException("Voucher đã hết hạn.");
                        }

                        BigDecimal voucherValue = voucher.getValue() != null ? voucher.getValue() : BigDecimal.ZERO;
                        finalPrice = totalPrice.subtract(voucherValue).max(BigDecimal.ZERO);
                        discountAmount = voucherValue;

                        booking.setDiscountAmount(discountAmount);
                        booking.setFinalPrice(finalPrice.setScale(0, RoundingMode.HALF_UP));

                        log.info("Applied refund voucher: {} | value={}", voucher.getCode(), voucherValue);
                }
                // Nếu không có promotion hoặc voucher → áp dụng rank discount
                else {
                        RankAndDiscountResponse rankInfo = userProfileClient
                                        .getUserRankAndDiscount(booking.getUserId());

                        if (rankInfo != null && rankInfo.getDiscountRate() != null
                                        && rankInfo.getDiscountRate().compareTo(BigDecimal.ZERO) > 0) {

                                discountAmount = totalPrice.multiply(rankInfo.getDiscountRate()).setScale(0,
                                                RoundingMode.HALF_UP);
                                finalPrice = totalPrice.subtract(discountAmount).max(BigDecimal.ZERO);

                                booking.setDiscountAmount(discountAmount);
                                booking.setFinalPrice(finalPrice);

                                log.info("Applied rank discount: {} ({})", rankInfo.getRankName(),
                                                rankInfo.getDiscountRate());
                        } else {
                                booking.setDiscountAmount(BigDecimal.ZERO);
                                booking.setFinalPrice(totalPrice);
                        }
                }

                // ======== Lưu & gửi event ========
                booking.setStatus(BookingStatus.AWAITING_PAYMENT);
                booking.setUpdatedAt(LocalDateTime.now());
                bookingRepository.save(booking);

                bookingProducer.sendBookingFinalizedEvent(
                                new BookingFinalizedEvent(
                                                booking.getId(),
                                                booking.getUserId(),
                                                booking.getGuestName(),
                                                booking.getGuestEmail(),
                                                booking.getShowtimeId(),
                                                booking.getFinalPrice()));

                log.info("Booking {} finalized: Total={}, Final={}, Discount={}",
                                bookingId, booking.getTotalPrice(), booking.getFinalPrice(),
                                booking.getDiscountAmount());

                return bookingMapper.toBookingResponse(booking);
        }

        private BigDecimal processFnbItems(Booking booking,
                        List<FinalizeBookingRequest.CalculatedFnbItemDto> fnbItems) {

                FnbCalculationRequest fnbRequest = new FnbCalculationRequest();
                fnbRequest.setSelectedFnbItems(fnbItems);

                FnbCalculationResponse fnbResponse = fnbClient.calculatePrice(fnbRequest);

                if (fnbResponse == null || fnbResponse.getCalculatedFnbItems() == null) {
                        throw new BookingException("Không nhận được dữ liệu F&B từ service FNB.");
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

                bookingFnbRepository.saveAll(bookingFnbs);

                return totalFnbPrice;
        }

        private void processPromotion(Booking booking, String promoCode) {

                PromotionValidationResponse validationResponse = promotionClient.validatePromotionCode(promoCode);
                boolean isOneTimeUse = validationResponse.getIsOneTimeUse();

                if (validationResponse == null || validationResponse.getDiscountValue() == null
                                || validationResponse.getDiscountType() == null) {
                        throw new BookingException("Lỗi xử lý khuyến mãi: Thiếu thông tin loại hoặc giá trị giảm.");
                }

                if (isOneTimeUse && usedPromotionRepository.existsByUserIdAndPromotionCode(booking.getUserId(),
                                promoCode)) {
                        throw new BookingException("Người dùng đã sử dụng mã khuyến mãi này rồi!");
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

                // Cho phép hoàn tiền nếu có userId
                if (newStatus == BookingStatus.CANCELLED && booking.getUserId() != null) {
                        try {
                                ShowtimeResponse showtime = showtimeClient.getShowtimeById(booking.getShowtimeId());
                                if (showtime != null) {
                                        LocalDateTime startTime = showtime.getStartTime();
                                        LocalDateTime now = LocalDateTime.now();

                                        // Hủy trước giờ chiếu ít nhất 60 phút → được hoàn
                                        if (now.isBefore(startTime.minusMinutes(60))) {
                                                BigDecimal refundValue = booking.getFinalPrice();

                                                promotionClient.createRefundVoucher(
                                                                booking.getUserId(),
                                                                refundValue);

                                                log.info("Refund voucher created for booking {} | user={} | value={}",
                                                                booking.getId(), booking.getUserId(), refundValue);

                                                bookingProducer.sendBookingRefundedEvent(
                                                                new BookingRefundedEvent(
                                                                                booking.getId(),
                                                                                booking.getUserId(),
                                                                                refundValue));

                                                log.info("BookingRefundedEvent sent for booking {}", booking.getId());
                                        } else {
                                                log.info("Booking {} canceled too close to showtime (<60m), no refund voucher.",
                                                                booking.getId());
                                        }
                                }
                        } catch (Exception e) {
                                log.error("Error creating refund voucher for booking {}: {}", booking.getId(),
                                                e.getMessage());
                        }
                }

                if (newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.EXPIRED) {
                        bookingProducer.sendSeatUnlockedEvent(
                                        new SeatUnlockedEvent(
                                                        booking.getShowtimeId(),
                                                        booking.getId(),
                                                        seatIds,
                                                        newStatus.name()));
                }

                bookingProducer.sendBookingStatusUpdatedEvent(
                                new BookingStatusUpdatedEvent(
                                                booking.getId(),
                                                booking.getShowtimeId(),
                                                seatIds,
                                                newStatus.toString(),
                                                oldStatus.name()));

                BookingTicketGeneratedEvent bookingticketGeneratedEvent = buildBookingTicketGeneratedEvent(booking);

                bookingProducer.sendBookingTicketGeneratedEvent(bookingticketGeneratedEvent);
        }

        @Transactional
        private BookingTicketGeneratedEvent buildBookingTicketGeneratedEvent(Booking booking) {

                ShowtimeResponse showtime = showtimeClient.getShowtimeById(booking.getShowtimeId());

                if (showtime == null) {
                        throw new BookingException("Không thể lấy thông tin suất chiếu cho booking " + booking.getId());
                }

                MovieTitleResponse movie = movieClient.getMovieTitleById(showtime.getMovieId());

                if (movie == null) {
                        throw new BookingException("Không thể lấy thông tin phim cho booking " + booking.getId());
                }

                String roomName = booking.getSeats().isEmpty() ? "Unknown Room"
                                : showtimeClient.getSeatInfoById(booking.getSeats().get(0).getSeatId()).getRoomName();

                List<SeatDetail> seatDetails = booking.getSeats().stream()
                                .map(seat -> {
                                        SeatResponse seatInfo = showtimeClient.getSeatInfoById(seat.getSeatId());
                                        if (seatInfo == null)
                                                throw new BookingException(
                                                                "Không tìm thấy thông tin ghế " + seat.getSeatId());
                                        return new SeatDetail(
                                                        seatInfo.getSeatNumber(),
                                                        seat.getSeatType(),
                                                        seat.getTicketType(),
                                                        1,
                                                        seat.getPrice());
                                })
                                .toList();

                List<BookingFnb> bookingFnbs = bookingFnbRepository.findByBooking_Id(booking.getId());
                List<FnbDetail> fnbDetails = bookingFnbs.stream()
                                .map(fnb -> {
                                        FnbItemResponse fnbInfo = fnbClient.getFnbUItemById(fnb.getFnbItemId());

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

                RankAndDiscountResponse rank = userProfileClient.getUserRankAndDiscount(booking.getUserId());

                BigDecimal rankDiscountAmount = BigDecimal.ZERO;
                BigDecimal rankDiscountRate = BigDecimal.ZERO;
                String rankName = "Bronze";

                if (rank != null && rank.getDiscountRate() != null) {
                        rankDiscountRate = rank.getDiscountRate();
                        rankDiscountAmount = booking.getTotalPrice().multiply(rankDiscountRate)
                                        .setScale(2, RoundingMode.HALF_UP);
                        rankName = rank.getRankName();
                }

                return new BookingTicketGeneratedEvent(
                                booking.getId(),
                                booking.getUserId(),
                                movie.getTitle(),
                                showtime.getTheaterName(),
                                roomName,
                                showtime.getStartTime().toString(),
                                seatDetails,
                                fnbDetails,
                                promotionDetail,
                                booking.getTotalPrice(),
                                rankName,
                                rankDiscountAmount,
                                booking.getFinalPrice(),
                                booking.getPaymentMethod(),
                                booking.getCreatedAt());

        }

        public BookingResponse getBookingById(UUID id) {
                Booking booking = bookingRepository.findById(id)
                                .orElseThrow(() -> new BookingException("Booking not found: " + id)); // Dùng
                                                                                                      // BookingException
                return bookingMapper.toBookingResponse(booking);
        }

        public List<BookingResponse> getBookingsByUser(UUID userId) {
                return bookingRepository.findByUserId(userId).stream()
                                .map(r -> bookingMapper.toBookingResponse(r))
                                .toList();
        }

        @Transactional
        public void deleteBooking(UUID id) {
                bookingRepository.deleteById(id);
        }
}