package com.cinehub.showtime.repository;

import com.cinehub.showtime.entity.Showtime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShowtimeRepositoryImpl implements ShowtimeRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Showtime> findAvailableShowtimesWithFiltersDynamic(
            UUID provinceId,
            UUID theaterId,
            UUID roomId,
            UUID movieId,
            UUID showtimeId,
            LocalDate selectedDate,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            LocalTime fromTime,
            LocalTime toTime,
            LocalDateTime now,
            Pageable pageable) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        // --- 1. Build predicates ---
        List<Predicate> predicates = new ArrayList<>();

        CriteriaQuery<Showtime> cq = cb.createQuery(Showtime.class);
        Root<Showtime> s = cq.from(Showtime.class);

        predicates.add(cb.greaterThan(s.get("startTime"), now));

        if (provinceId != null) {
            predicates.add(cb.equal(s.get("theater").get("province").get("id"), provinceId));
        }
        if (theaterId != null) {
            predicates.add(cb.equal(s.get("theater").get("id"), theaterId));
        }
        if (roomId != null) {
            predicates.add(cb.equal(s.get("room").get("id"), roomId));
        }
        if (movieId != null) {
            predicates.add(cb.equal(s.get("movieId"), movieId));
        }
        if (showtimeId != null) {
            predicates.add(cb.equal(s.get("id"), showtimeId));
        }
        if (selectedDate != null && startOfDay != null && endOfDay != null) {
            predicates.add(cb.between(s.get("startTime"), startOfDay, endOfDay));
        }
        if (fromTime != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                    cb.function("time", LocalTime.class, s.get("startTime")), fromTime));
        }
        if (toTime != null) {
            predicates.add(cb.lessThanOrEqualTo(
                    cb.function("time", LocalTime.class, s.get("startTime")), toTime));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        // --- 2. Sort ---
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                if (order.isAscending()) {
                    orders.add(cb.asc(s.get(order.getProperty())));
                } else {
                    orders.add(cb.desc(s.get(order.getProperty())));
                }
            });
            cq.orderBy(orders);
        }

        // --- 3. Fetch content ---
        List<Showtime> content = em.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // --- 4. Fetch total count separately ---
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Showtime> countRoot = countQuery.from(Showtime.class);
        countQuery.select(cb.count(countRoot));

        // Reuse same predicates for count query
        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.greaterThan(countRoot.get("startTime"), now));
        if (provinceId != null) {
            countPredicates.add(cb.equal(countRoot.get("theater").get("province").get("id"), provinceId));
        }
        if (theaterId != null) {
            countPredicates.add(cb.equal(countRoot.get("theater").get("id"), theaterId));
        }
        if (roomId != null) {
            countPredicates.add(cb.equal(countRoot.get("room").get("id"), roomId));
        }
        if (movieId != null) {
            countPredicates.add(cb.equal(countRoot.get("movieId"), movieId));
        }
        if (showtimeId != null) {
            countPredicates.add(cb.equal(countRoot.get("id"), showtimeId));
        }
        if (selectedDate != null && startOfDay != null && endOfDay != null) {
            countPredicates.add(cb.between(countRoot.get("startTime"), startOfDay, endOfDay));
        }
        if (fromTime != null) {
            countPredicates.add(cb.greaterThanOrEqualTo(
                    cb.function("time", LocalTime.class, countRoot.get("startTime")), fromTime));
        }
        if (toTime != null) {
            countPredicates.add(cb.lessThanOrEqualTo(
                    cb.function("time", LocalTime.class, countRoot.get("startTime")), toTime));
        }

        countQuery.where(countPredicates.toArray(new Predicate[0]));
        long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}
