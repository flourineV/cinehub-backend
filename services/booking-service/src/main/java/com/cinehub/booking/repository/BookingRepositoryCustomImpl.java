package com.cinehub.booking.repository;

import com.cinehub.booking.dto.request.BookingCriteria;
import com.cinehub.booking.entity.Booking;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class BookingRepositoryCustomImpl implements BookingRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Booking> searchWithCriteria(BookingCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Booking> query = cb.createQuery(Booking.class);
        Root<Booking> booking = query.from(Booking.class);

        List<Predicate> predicates = new ArrayList<>();

        // keyword - partial match on userId, showtimeId, bookingCode, guestName
        if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
            String keywordLower = "%" + criteria.getKeyword().toLowerCase() + "%";
            List<Predicate> keywordPredicates = new ArrayList<>();

            // Match userId as string
            keywordPredicates.add(cb.like(cb.lower(booking.get("userId").as(String.class)), keywordLower));

            // Match showtimeId as string
            keywordPredicates.add(cb.like(cb.lower(booking.get("showtimeId").as(String.class)), keywordLower));

            // Match bookingCode
            keywordPredicates.add(cb.like(cb.lower(booking.get("BookingCode")), keywordLower));

            // Match guestName
            keywordPredicates.add(cb.like(cb.lower(booking.get("guestName")), keywordLower));

            predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
        }

        // userId
        if (criteria.getUserId() != null) {
            predicates.add(cb.equal(booking.get("userId"), criteria.getUserId()));
        }

        // userIds (for username search - multiple matching users)
        if (criteria.getUserIds() != null && !criteria.getUserIds().isEmpty()) {
            predicates.add(booking.get("userId").in(criteria.getUserIds()));
        }

        // showtimeId
        if (criteria.getShowtimeId() != null) {
            predicates.add(cb.equal(booking.get("showtimeId"), criteria.getShowtimeId()));
        }

        // movieId
        if (criteria.getMovieId() != null) {
            predicates.add(cb.equal(booking.get("movieId"), criteria.getMovieId()));
        }

        // bookingCode (LIKE search)
        if (criteria.getBookingCode() != null && !criteria.getBookingCode().isBlank()) {
            predicates.add(cb.like(
                    cb.lower(booking.get("BookingCode")),
                    "%" + criteria.getBookingCode().toLowerCase() + "%"));
        }

        // status
        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(booking.get("status"), criteria.getStatus()));
        }

        // paymentMethod
        if (criteria.getPaymentMethod() != null && !criteria.getPaymentMethod().isBlank()) {
            predicates.add(cb.equal(booking.get("paymentMethod"), criteria.getPaymentMethod()));
        }

        // guestName (LIKE search)
        if (criteria.getGuestName() != null && !criteria.getGuestName().isBlank()) {
            predicates.add(cb.like(
                    cb.lower(booking.get("guestName")),
                    "%" + criteria.getGuestName().toLowerCase() + "%"));
        }

        // guestEmail (LIKE search)
        if (criteria.getGuestEmail() != null && !criteria.getGuestEmail().isBlank()) {
            predicates.add(cb.like(
                    cb.lower(booking.get("guestEmail")),
                    "%" + criteria.getGuestEmail().toLowerCase() + "%"));
        }

        // fromDate
        if (criteria.getFromDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(booking.get("createdAt"), criteria.getFromDate()));
        }

        // toDate
        if (criteria.getToDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(booking.get("createdAt"), criteria.getToDate()));
        }

        // minPrice
        if (criteria.getMinPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(booking.get("finalPrice"), criteria.getMinPrice()));
        }

        // maxPrice
        if (criteria.getMaxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(booking.get("finalPrice"), criteria.getMaxPrice()));
        }

        query.where(predicates.toArray(new Predicate[0]));

        // Apply sorting from Pageable
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                if (order.isAscending()) {
                    orders.add(cb.asc(booking.get(order.getProperty())));
                } else {
                    orders.add(cb.desc(booking.get(order.getProperty())));
                }
            });
            query.orderBy(orders);
        }

        // Execute query with pagination
        TypedQuery<Booking> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Booking> results = typedQuery.getResultList();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Booking> countRoot = countQuery.from(Booking.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }
}
