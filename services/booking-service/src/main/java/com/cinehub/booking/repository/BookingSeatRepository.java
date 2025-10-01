package com.cinehub.booking.repository;

import com.cinehub.booking.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, String> {
}
