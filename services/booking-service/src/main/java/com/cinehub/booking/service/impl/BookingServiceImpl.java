package com.cinehub.booking.service.impl;

import com.cinehub.booking.dto.external.SeatPriceResponse;
import com.cinehub.booking.dto.external.ShowtimeResponse;
import com.cinehub.booking.dto.external.PromotionValidationResponse;
import com.cinehub.booking.dto.external.FnbCalculationResponse;
import com.cinehub.booking.dto.request.FinalizeBookingRequest;
import com.cinehub.booking.dto.request.SeatSelectionDetail;
import com.cinehub.booking.dto.response.BookingResponse;
import com.cinehub.booking.dto.response.PagedResponse;
import com.cinehub.booking.dto.external.FnbCalculationRequest;
import com.cinehub.booking.dto.external.FnbItemDto;
import com.cinehub.booking.dto.external.MovieTitleResponse;
import com.cinehub.booking.dto.external.SeatResponse;
import com.cinehub.booking.dto.external.FnbItemResponse;
import com.cinehub.booking.dto.external.RankAndDiscountResponse;
import com.cinehub.booking.dto.request.BookingCriteria;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        private final BookingSeatRepository bookingSeatRepository;
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

                if (request.getSelectedSeats() == null || request.getSelectedSeats().isEmpty()) {
                        throw new BookingException("At least one seat must be selected");
                }

                log.info("Creating booking: showtime={}, seats={}, user={}, guest={}",
                                request.getShowtimeId(), request.getSelectedSeats().size(),
                                request.getUserId(), request.getGuestSessionId());

                ShowtimeResponse showtime = showtimeClient.getShowtimeById(request.getShowtimeId());
                if (showtime == null) {
                        throw new BookingException("Showtime not found");
                }

                if ("SUSPENDED".equals(showtime.getStatus())) {
                        throw new BookingException(
                                        "This showtime has been suspended and is no longer available for booking");
                }

                // Check if showtime is in the past
                if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
                        throw new BookingException("Cannot book a showtime that has already started");
                }

                // ======== 2. VALIDATE OWNERSHIP ========
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

                // Lấy thông tin movie để lưu snapshot
                MovieTitleResponse movie = movieClient.getMovieTitleById(showtime.getMovieId());
                String movieTitle = (movie != null) ? movie.getTitle() : null;

                // Lấy roomName từ seat đầu tiên
                String roomName = null;
                if (!request.getSelectedSeats().isEmpty()) {
                        SeatResponse seatInfo = showtimeClient.getSeatInfoById(
                                        request.getSelectedSeats().get(0).getSeatId());
                        roomName = (seatInfo != null) ? seatInfo.getRoomName() : null;
                }

                Booking booking = Booking.builder()
                                .userId(request.getUserId())
                                .showtimeId(request.getShowtimeId())
                                .movieId(showtime.getMovieId())
                                // Snapshot fields
                                .movieTitle(movieTitle)
                                .theaterName(showtime.getTheaterName())
                                .roomName(roomName)
                                .showDateTime(showtime.getStartTime())
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

                        // Lấy seatNumber từ showtime-service
                        SeatResponse seatInfo = showtimeClient.getSeatInfoById(seatDetail.getSeatId());
                        String seatNumber = (seatInfo != null) ? seatInfo.getSeatNumber() : null;

                        BigDecimal price = seatPrice.getBasePrice();
                        totalSeatPrice = totalSeatPrice.add(price);

                        seats.add(BookingSeat.builder()
                                        .seatId(seatDetail.getSeatId())
                                        .seatNumber(seatNumber)
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

                bookingRepository.save(booking);

                log.info("Booking created: {} | total={} | seats={}",
                                booking.getId(), totalSeatPrice, seats.size());

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
                        log.info("SeatUnlockedEvent received without bookingId (manual unlock). No booking to update.");
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
                // Keep used_promotion record for history
                bookingSeatRepository.deleteByBooking_Id(booking.getId());

                updateBookingStatus(booking, BookingStatus.EXPIRED);
        }

        @Transactional
        public void handlePaymentSuccess(PaymentBookingSuccessEvent data) {

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

                if (booking.getUserId() != null) {
                        // Tích điểm: 10,000 VND = 1 điểm (Silver 1000 điểm = 10 triệu, Gold 5000 điểm =
                        // 50
                        // triệu)
                        BigDecimal divisor = new BigDecimal("10000");
                        int pointsEarned = booking.getFinalPrice().divide(divisor, 0, RoundingMode.DOWN).intValue();

                        if (pointsEarned > 0) {
                                log.info("💎 Earning {} loyalty points for booking {} (amount: {})",
                                                pointsEarned, booking.getId(), booking.getFinalPrice());
                                userProfileClient.updateLoyaltyPoints(
                                        booking.getUserId(), 
                                        booking.getId(), 
                                        booking.getBookingCode(),
                                        pointsEarned, 
                                        booking.getFinalPrice()
                                );
                        }
                }

                // Send notification event for booking confirmation
                try {
                        BookingTicketGeneratedEvent ticketEvent = buildBookingTicketGeneratedEvent(booking);
                        bookingProducer.sendBookingTicketGeneratedEvent(ticketEvent);
                        log.info("📧 Sent BookingTicketGeneratedEvent for booking {}", booking.getId());
                } catch (Exception e) {
                        log.error("Failed to send booking ticket notification for booking {}: {}",
                                        booking.getId(), e.getMessage(), e);
                }
        }

        @Transactional
        public void handlePaymentFailed(PaymentBookingFailedEvent data) {
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
                // Keep used_promotion record for history

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

                // Convert CalculatedFnbItemDto to FnbItemDto (remove price fields)
                List<FnbItemDto> fnbItemDtos = fnbItems.stream()
                                .map(item -> {
                                        FnbItemDto dto = new FnbItemDto();
                                        dto.setFnbItemId(item.getFnbItemId());
                                        dto.setQuantity(item.getQuantity());
                                        return dto;
                                })
                                .toList();

                fnbRequest.setSelectedFnbItems(fnbItemDtos);

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

                if (validationResponse == null || validationResponse.getDiscountValue() == null
                                || validationResponse.getDiscountType() == null) {
                        throw new BookingException("Lỗi xử lý khuyến mãi: Thiếu thông tin loại hoặc giá trị giảm.");
                }

                // Check if user can use this promotion (handled by promotion-service)
                Boolean canUse = promotionClient.canUsePromotion(booking.getUserId(), promoCode);
                if (!canUse) {
                        throw new BookingException("Người dùng đã sử dụng mã khuyến mãi này rồi!");
                }

                BigDecimal totalBeforeDiscount = booking.getTotalPrice();
                BigDecimal discountValue = validationResponse.getDiscountValue();
                DiscountType discountType = validationResponse.getDiscountType();

                log.info("🎟️ Processing promotion: code={}, type={}, value={}, totalPrice={}",
                                promoCode, discountType, discountValue, totalBeforeDiscount);

                BigDecimal calculatedDiscountAmount;

                if (discountType == DiscountType.PERCENTAGE) {
                        // Percentage is stored as whole number (e.g., 25 for 25%, 30 for 30%)
                        // Need to divide by 100 to get the decimal multiplier
                        calculatedDiscountAmount = totalBeforeDiscount
                                        .multiply(discountValue)
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        log.info("📊 Calculated PERCENTAGE discount: {} * {}% / 100 = {}",
                                        totalBeforeDiscount, discountValue, calculatedDiscountAmount);
                } else if (discountType == DiscountType.FIXED_AMOUNT) {
                        calculatedDiscountAmount = discountValue;
                        log.info("💵 Using FIXED_AMOUNT discount: {}", calculatedDiscountAmount);
                } else {
                        calculatedDiscountAmount = BigDecimal.ZERO;
                }

                BigDecimal discountAmount = calculatedDiscountAmount.setScale(2, RoundingMode.HALF_UP);
                BigDecimal newFinalPrice = totalBeforeDiscount.subtract(discountAmount);

                // Validate: không cho phép discount 100% (finalPrice = 0) vì ZaloPay không chấp
                // nhận
                if (newFinalPrice.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BookingException(
                                        "Khuyến mãi này không thể áp dụng vì sẽ làm tổng tiền = 0. Vui lòng chọn khuyến mãi khác hoặc không dùng khuyến mãi.");
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

                // Record promotion usage in promotion-service
                try {
                        promotionClient.recordPromotionUsage(booking.getUserId(), promoCode, booking.getId());
                } catch (Exception e) {
                        log.error("Failed to record promotion usage: {}", e.getMessage());
                        // Don't fail the booking if promotion recording fails
                }
        }

        @Override
        public PagedResponse<BookingResponse> getBookingsByCriteria(
                        BookingCriteria criteria,
                        int page,
                        int size,
                        String sortBy,
                        String sortType) {

                // If username search is provided, get matching userIds first
                if (criteria.getUsername() != null && !criteria.getUsername().trim().isEmpty()) {
                        try {
                                List<UUID> matchingUserIds = userProfileClient
                                                .searchUserIdsByUsername(criteria.getUsername());
                                if (matchingUserIds.isEmpty()) {
                                        // No users found matching username, return empty result
                                        return PagedResponse.<BookingResponse>builder()
                                                        .data(java.util.Collections.emptyList())
                                                        .page(page)
                                                        .size(size)
                                                        .totalElements(0)
                                                        .totalPages(0)
                                                        .build();
                                }
                                // Set matching userIds to criteria for repository search
                                criteria.setUserIds(matchingUserIds);
                                log.debug("Found {} matching users for username '{}'", matchingUserIds.size(),
                                                criteria.getUsername());
                        } catch (Exception e) {
                                log.error("Failed to search userIds by username: {}", e.getMessage());
                        }
                }

                Sort sort = sortType.equalsIgnoreCase("desc")
                                ? Sort.by(sortBy).descending()
                                : Sort.by(sortBy).ascending();

                Pageable pageable = PageRequest.of(page, size, sort);

                Page<Booking> bookingsPage = bookingRepository.searchWithCriteria(criteria, pageable);

                List<BookingResponse> bookingResponses = bookingsPage.getContent().stream()
                                .map(bookingMapper::toBookingResponse)
                                .toList();

                // Batch fetch userNames
                List<UUID> userIds = bookingResponses.stream()
                                .map(BookingResponse::getUserId)
                                .filter(java.util.Objects::nonNull)
                                .distinct()
                                .toList();

                java.util.Map<UUID, String> userNames = java.util.Collections.emptyMap();
                if (!userIds.isEmpty()) {
                        try {
                                userNames = userProfileClient.getBatchUserNames(userIds);
                        } catch (Exception e) {
                                log.error("Failed to fetch batch user names: {}", e.getMessage());
                        }
                }

                // Enrich responses with fullName (movieTitle đã có snapshot trong DB)
                final java.util.Map<UUID, String> finalUserNames = userNames;

                bookingResponses.forEach(response -> {
                        if (response.getUserId() != null) {
                                response.setFullName(finalUserNames.getOrDefault(response.getUserId(), null));
                        }
                });

                return PagedResponse.<BookingResponse>builder()
                                .data(bookingResponses)
                                .page(bookingsPage.getNumber())
                                .size(bookingsPage.getSize())
                                .totalElements(bookingsPage.getTotalElements())
                                .totalPages(bookingsPage.getTotalPages())
                                .build();
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

                // Update promotion usage status in promotion-service
                try {
                        promotionClient.updatePromotionUsageStatus(booking.getId(), newStatus.name());
                } catch (Exception e) {
                        log.error("Failed to update promotion usage status: {}", e.getMessage());
                        // Don't fail the booking status update if promotion update fails
                }

                List<UUID> seatIds = booking.getSeats().stream()
                                .map(BookingSeat::getSeatId)
                                .toList();

                if (newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.EXPIRED
                                || newStatus == BookingStatus.REFUNDED) {
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
                                booking.getBookingCode(),
                                booking.getUserId(),
                                booking.getGuestName(),
                                booking.getGuestEmail(),
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
                                .orElseThrow(() -> new BookingException("Booking not found: " + id));
                BookingResponse response = bookingMapper.toBookingResponse(booking);

                // Enrich with fullName (movieTitle, theaterName, roomName đã có snapshot trong DB)
                if (response.getUserId() != null) {
                        try {
                                java.util.Map<UUID, String> userNames = userProfileClient.getBatchUserNames(
                                                java.util.List.of(response.getUserId()));
                                response.setFullName(userNames.get(response.getUserId()));
                        } catch (Exception e) {
                                log.error("Failed to fetch user name for booking {}: {}", id, e.getMessage());
                        }
                }

                return response;
        }

        public List<BookingResponse> getBookingsByUser(UUID userId) {
                List<BookingResponse> responses = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                                .map(r -> bookingMapper.toBookingResponse(r))
                                .toList();

                if (responses.isEmpty()) {
                        return responses;
                }

                // Enrich with fullName (movieTitle, theaterName, roomName đã có snapshot trong DB)
                if (userId != null) {
                        try {
                                java.util.Map<UUID, String> userNames = userProfileClient.getBatchUserNames(
                                                java.util.List.of(userId));
                                String fullName = userNames.get(userId);
                                responses.forEach(r -> r.setFullName(fullName));
                        } catch (Exception e) {
                                log.error("Failed to fetch user name for userId {}: {}", userId, e.getMessage());
                        }
                }

                return responses;
        }

        @Transactional
        public BookingResponse cancelBooking(UUID bookingId, UUID userId) {
                Booking booking = bookingRepository.findById(bookingId)
                                .orElseThrow(() -> new BookingNotFoundException(bookingId.toString()));

                // Validate ownership
                if (!booking.getUserId().equals(userId)) {
                        throw new BookingException("You don't own this booking");
                }

                // Only CONFIRMED bookings can be cancelled
                if (booking.getStatus() != BookingStatus.CONFIRMED) {
                        throw new BookingException("Only confirmed bookings can be cancelled. Current status: "
                                        + booking.getStatus());
                }

                // Check showtime timing (must cancel at least 60 minutes before)
                ShowtimeResponse showtime = showtimeClient.getShowtimeById(booking.getShowtimeId());
                if (showtime == null) {
                        throw new BookingException("Showtime not found");
                }

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startTime = showtime.getStartTime();

                if (now.isAfter(startTime.minusMinutes(60))) {
                        throw new BookingException("Cannot cancel booking less than 60 minutes before showtime");
                }

                // Check monthly cancellation limit (2 times per month)
                LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                long cancelCount = bookingRepository.countCancelledBookingsInMonth(userId, startOfMonth);

                if (cancelCount >= 2) {
                        throw new BookingException(
                                        "You have reached the monthly cancellation limit (2 times per month)");
                }

                // Create refund voucher
                BigDecimal refundValue = booking.getFinalPrice();
                try {
                        promotionClient.createRefundVoucher(userId, refundValue);
                        log.info("Refund voucher created for booking {} | user={} | value={}",
                                        bookingId, userId, refundValue);
                } catch (Exception e) {
                        log.error("Failed to create refund voucher for booking {}: {}", bookingId, e.getMessage());
                        throw new BookingException("Failed to create refund voucher");
                }

                // Update booking status to REFUNDED (not CANCELLED)
                updateBookingStatus(booking, BookingStatus.REFUNDED);

                log.info("Booking {} refunded by user {}. Voucher created with value {}",
                                bookingId, userId, refundValue);

                return bookingMapper.toBookingResponse(booking);
        }

        @Transactional
        public void handleShowtimeSuspended(ShowtimeSuspendedEvent event) {
                log.warn("Showtime {} suspended. Reason: {}. Finding affected bookings...",
                                event.showtimeId(), event.reason());

                List<Booking> affectedBookings;

                if (event.affectedBookingIds() != null && !event.affectedBookingIds().isEmpty()) {
                        affectedBookings = bookingRepository.findAllById(event.affectedBookingIds());
                } else {
                        // Fallback nếu event không gửi list ID
                        affectedBookings = bookingRepository.findByShowtimeIdAndStatus(event.showtimeId(),
                                        BookingStatus.CONFIRMED);
                }

                if (affectedBookings.isEmpty()) {
                        log.info("No confirmed user bookings found for suspended showtime {}", event.showtimeId());
                        return;
                }

                for (Booking booking : affectedBookings) {
                        if (booking.getStatus() != BookingStatus.CONFIRMED)
                                continue;

                        BigDecimal refundValue = booking.getFinalPrice();
                        String refundMethod = "UNKNOWN";

                        // CASE 1: REGISTERED USER -> Hoàn Voucher
                        if (booking.getUserId() != null) {
                                try {
                                        promotionClient.createRefundVoucher(booking.getUserId(), refundValue);
                                        refundMethod = "VOUCHER";
                                        log.info("SYSTEM REFUND: Created VOUCHER for User {} - Booking {}",
                                                        booking.getUserId(), booking.getId());
                                } catch (Exception e) {
                                        log.error("Error creating voucher for user {}: {}", booking.getUserId(),
                                                        e.getMessage());
                                        // Vẫn tiếp tục update status để không bị treo đơn, sau đó Admin xử lý tay
                                        refundMethod = "ERROR_VOUCHER";
                                }
                        } else {
                                refundMethod = "COUNTER";
                                log.info("SYSTEM REFUND: Marked Booking {} (Guest) for COUNTER refund.",
                                                booking.getId());
                        }

                        updateBookingStatus(booking, BookingStatus.REFUNDED);
                        bookingProducer.sendBookingRefundedEvent(
                                        new BookingRefundedEvent(
                                                        booking.getId(),
                                                        booking.getUserId(),
                                                        booking.getGuestName(),
                                                        booking.getGuestEmail(),
                                                        booking.getShowtimeId(),
                                                        refundValue,
                                                        refundMethod,
                                                        event.reason()));
                }
        }

        @Transactional
        public void deleteBooking(UUID id) {
                bookingRepository.deleteById(id);
        }

        @Override
        @Transactional
        public int backfillMovieIds() {
                log.info("Starting backfill of movieIds for bookings with null movieId");

                List<Booking> bookingsWithoutMovieId = bookingRepository.findAllWithNullMovieId();

                if (bookingsWithoutMovieId.isEmpty()) {
                        log.info("No bookings found with null movieId");
                        return 0;
                }

                int successCount = 0;
                int failCount = 0;

                for (Booking booking : bookingsWithoutMovieId) {
                        try {
                                ShowtimeResponse showtime = showtimeClient.getShowtimeById(booking.getShowtimeId());
                                if (showtime != null && showtime.getMovieId() != null) {
                                        booking.setMovieId(showtime.getMovieId());
                                        bookingRepository.save(booking);
                                        successCount++;
                                        log.debug("Updated booking {} with movieId {}", booking.getId(),
                                                        showtime.getMovieId());
                                } else {
                                        failCount++;
                                        log.warn("Could not get movieId for booking {} (showtime: {})",
                                                        booking.getId(), booking.getShowtimeId());
                                }
                        } catch (Exception e) {
                                failCount++;
                                log.error("Error updating booking {}: {}", booking.getId(), e.getMessage());
                        }
                }

                log.info("Backfill completed: {} updated, {} failed out of {} total",
                                successCount, failCount, bookingsWithoutMovieId.size());

                return successCount;
        }

        @Override
        public boolean hasUserBookedMovie(UUID userId, UUID movieId) {

                List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);

                return bookings.stream().anyMatch(b -> b.getMovieId().equals(movieId) &&
                                (b.getStatus() == BookingStatus.CONFIRMED));
        }

        @Override
        public long getBookingCountByUserId(UUID userId) {
                return bookingRepository.countByUserId(userId);
        }
}