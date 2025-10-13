package com.cinehub.pricing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "seat_price")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "seat_type", nullable = false, length = 50)
    private String seatType; // Ví dụ: NORMAL, VIP, COUPLE

    @Column(name = "ticket_type", nullable = false, length = 50)
    private String ticketType; // Ví dụ: ADULT, CHILD, STUDENT

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "description", length = 255)
    private String description;
}