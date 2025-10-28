package com.cinehub.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payment_transaction", indexes = {
                @Index(name = "idx_payment_booking_id", columnList = "bookingId"),
                @Index(name = "idx_payment_user_id", columnList = "userId"),
                @Index(name = "idx_payment_status", columnList = "status"),
                @Index(name = "idx_payment_created_at", columnList = "createdAt")
}, uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_transaction_ref", columnNames = "transactionRef")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

        @Id
        @GeneratedValue
        private UUID id;

        @Column(nullable = false)
        private UUID bookingId;

        @Column(nullable = false)
        private UUID userId;

        @Column(nullable = false)
        private UUID showtimeId;

        // 💺 Lưu danh sách ghế theo BookingCreatedEvent
        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "payment_seat_ids", joinColumns = @JoinColumn(name = "payment_id"))
        @Column(name = "seat_id", nullable = false)
        private List<UUID> seatIds;

        @Column(nullable = false, precision = 10, scale = 2)
        private BigDecimal amount;

        @Column(nullable = false, length = 50)
        private String method;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private PaymentStatus status;

        @Column(nullable = false, unique = true, length = 255)
        private String transactionRef;

        @CreationTimestamp
        @Column(nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(nullable = false)
        private LocalDateTime updatedAt;
}
