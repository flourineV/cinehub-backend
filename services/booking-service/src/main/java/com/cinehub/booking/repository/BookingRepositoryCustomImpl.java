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

        List<Predicate> predicates = buildPredicates(cb, booking, criteria);
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

        // Count query - rebuild predicates with new root
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Booking> countRoot = countQuery.from(Booking.class);
        countQuery.select(cb.count(countRoot));
        
        // Rebuild predicates for count query
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, criteria);
        countQuery.where(countPredicates.toArray(new Predicate[0]));
        
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Booking> booking, BookingCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();

        if (criteria.getUserId() != null) {
            predicates.add(cb.equal(booking.get("userId"), criteria.getUserId()));
        }

        if (criteria.getShowtimeId() != null) {
            predicates.add(cb.equal(booking.get("showtimeId"), criteria.getShowtimeId()));
        }

        if (criteria.getBookingCode() != null && !criteria.getBookingCode().isBlank()) {
            predicates.add(cb.like(
                cb.lower(booking.get("BookingCode")),
                "%" + criteria.getBookingCode().toLowerCase() + "%"
            ));
        }

        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(booking.get("status"), criteria.getStatus()));
        }

        if (criteria.getPaymentMethod() != null && !criteria.getPaymentMethod().isBlank()) {
            predicates.add(cb.equal(booking.get("paymentMethod"), criteria.getPaymentMethod()));
        }

        if (criteria.getGuestName() != null && !criteria.getGuestName().isBlank()) {
            predicates.add(cb.like(
                cb.lower(booking.get("guestName")),
                "%" + criteria.getGuestName().toLowerCase() + "%"
            ));
        }

        if (criteria.getGuestEmail() != null && !criteria.getGuestEmail().isBlank()) {
            predicates.add(cb.like(
                cb.lower(booking.get("guestEmail")),
                "%" + criteria.getGuestEmail().toLowerCase() + "%"
            ));
        }

        if (criteria.getFromDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(booking.get("createdAt"), criteria.getFromDate()));
        }

        if (criteria.getToDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(booking.get("createdAt"), criteria.getToDate()));
        }

        if (criteria.getMinPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(booking.get("finalPrice"), criteria.getMinPrice()));
        }

        if (criteria.getMaxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(booking.get("finalPrice"), criteria.getMaxPrice()));
        }

        return predicates;
    }
}
