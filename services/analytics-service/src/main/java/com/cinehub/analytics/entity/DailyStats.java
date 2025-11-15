package com.cinehub.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "date", nullable = false, unique = true)
    private LocalDate date;

    @Column(name = "total_page_views")
    private Long totalPageViews;

    @Column(name = "unique_visitors")
    private Long uniqueVisitors;

    @Column(name = "total_bookings")
    private Long totalBookings;

    @Column(name = "total_revenue")
    private Double totalRevenue;

    @Column(name = "new_users")
    private Long newUsers;

    @Column(name = "guest_users")
    private Long guestUsers;
}
