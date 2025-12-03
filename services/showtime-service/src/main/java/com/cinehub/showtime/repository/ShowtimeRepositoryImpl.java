package com.cinehub.showtime.repository;

import com.cinehub.showtime.entity.Showtime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.query.QueryUtils; // Import quan trọng để sort

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
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            LocalTime fromTime,
            LocalTime toTime,
            LocalDateTime now,
            Pageable pageable) {

        CriteriaBuilder cb = em.getCriteriaBuilder();

        // --- 1. Fetch Content Query ---
        CriteriaQuery<Showtime> cq = cb.createQuery(Showtime.class);
        Root<Showtime> root = cq.from(Showtime.class);

        // Build predicates reuse logic
        List<Predicate> predicates = buildPredicates(cb, root, provinceId, theaterId, roomId, movieId, showtimeId,
                startOfDay, endOfDay, fromTime, toTime, now);

        cq.where(predicates.toArray(new Predicate[0]));

        // Sort
        if (pageable.getSort().isSorted()) {
            cq.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));
        }

        List<Showtime> content = em.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // --- 2. Count Query ---
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Showtime> countRoot = countQuery.from(Showtime.class);

        // Tái sử dụng hàm buildPredicates cho count query
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, provinceId, theaterId, roomId, movieId,
                showtimeId,
                startOfDay, endOfDay, fromTime, toTime, now);

        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Hàm helper để tạo điều kiện lọc chung cho cả select và count
     */
    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Showtime> root,
            UUID provinceId, UUID theaterId, UUID roomId, UUID movieId, UUID showtimeId,
            LocalDateTime startOfDay, LocalDateTime endOfDay,
            LocalTime fromTime, LocalTime toTime, LocalDateTime now) {
        List<Predicate> predicates = new ArrayList<>();

        if (provinceId != null) {
            predicates.add(cb.equal(root.get("theater").get("province").get("id"), provinceId));
        }
        if (theaterId != null) {
            predicates.add(cb.equal(root.get("theater").get("id"), theaterId));
        }
        if (roomId != null) {
            predicates.add(cb.equal(root.get("room").get("id"), roomId));
        }
        if (movieId != null) {
            predicates.add(cb.equal(root.get("movieId"), movieId));
        }
        if (showtimeId != null) {
            predicates.add(cb.equal(root.get("id"), showtimeId));
        }
        
        // Nếu có date range filter, dùng nó thay vì filter theo now
        if (startOfDay != null && endOfDay != null) {
            predicates.add(cb.between(root.get("startTime"), startOfDay, endOfDay));
        } else {
            // Chỉ filter theo now khi không có date range
            predicates.add(cb.greaterThan(root.get("startTime"), now));
        }

        // Lọc theo thời gian trong ngày (fromTime, toTime)
        // Dùng PostgreSQL EXTRACT với format string
        if (fromTime != null) {
            int fromMinutes = fromTime.getHour() * 60 + fromTime.getMinute();

            // EXTRACT(HOUR FROM startTime) * 60 + EXTRACT(MINUTE FROM startTime)
            Expression<Integer> hourExpr = cb.function("date_part", Integer.class,
                    cb.literal("hour"), root.get("startTime"));
            Expression<Integer> minuteExpr = cb.function("date_part", Integer.class,
                    cb.literal("minute"), root.get("startTime"));
            Expression<Integer> totalMinutes = cb.sum(
                    cb.prod(hourExpr, 60),
                    minuteExpr);

            predicates.add(cb.greaterThanOrEqualTo(totalMinutes, fromMinutes));
        }

        if (toTime != null) {
            int toMinutes = toTime.getHour() * 60 + toTime.getMinute();

            Expression<Integer> hourExpr = cb.function("date_part", Integer.class,
                    cb.literal("hour"), root.get("startTime"));
            Expression<Integer> minuteExpr = cb.function("date_part", Integer.class,
                    cb.literal("minute"), root.get("startTime"));
            Expression<Integer> totalMinutes = cb.sum(
                    cb.prod(hourExpr, 60),
                    minuteExpr);

            predicates.add(cb.lessThanOrEqualTo(totalMinutes, toMinutes));
        }

        return predicates;
    }
}