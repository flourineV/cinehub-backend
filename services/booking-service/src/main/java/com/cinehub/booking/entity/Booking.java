package com.cinehub.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "booking")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "booking_code", nullable = false, unique = true)
    private String BookingCode;

    @Column(nullable = true)
    private UUID userId;

    @Column(nullable = false)
    private UUID showtimeId;

    @Column(name = "movie_id")
    private UUID movieId;

    // Snapshot fields - lưu tại thời điểm đặt vé để tránh N+1 query
    @Column(name = "movie_title")
    private String movieTitle;

    @Column(name = "movie_title_en")
    private String movieTitleEn;

    @Column(name = "theater_name")
    private String theaterName;

    @Column(name = "theater_name_en")
    private String theaterNameEn;

    @Column(name = "room_name")
    private String roomName;

    @Column(name = "room_name_en")
    private String roomNameEn;

    @Column(name = "show_date_time")
    private LocalDateTime showDateTime;

    @Column(name = "payment_id", length = 100)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private BigDecimal discountAmount;

    @Column(nullable = false)
    private BigDecimal finalPrice;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "guest_name", length = 100)
    private String guestName;

    @Column(name = "guest_email", length = 100)
    private String guestEmail;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "language", length = 5)
    private String language; // 'vi' or 'en' - for email/notification language

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BookingFnb> fnbItems = new ArrayList<>();

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BookingSeat> seats = new ArrayList<>();

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private BookingPromotion promotion;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = BookingStatus.PENDING;
        }
        if (this.BookingCode == null || this.BookingCode.isEmpty()) {
            this.BookingCode = generateCode();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static String generateCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
