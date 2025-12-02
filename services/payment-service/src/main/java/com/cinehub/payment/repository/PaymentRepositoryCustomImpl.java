package com.cinehub.payment.repository;

import com.cinehub.payment.dto.request.PaymentCriteria;
import com.cinehub.payment.entity.PaymentTransaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PaymentRepositoryCustomImpl implements PaymentRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<PaymentTransaction> findByCriteria(PaymentCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<PaymentTransaction> query = cb.createQuery(PaymentTransaction.class);
        Root<PaymentTransaction> root = query.from(PaymentTransaction.class);

        List<Predicate> predicates = buildPredicates(criteria, cb, root);

        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        // Apply sorting from Pageable
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(order -> {
                if (order.isAscending()) {
                    orders.add(cb.asc(root.get(order.getProperty())));
                } else {
                    orders.add(cb.desc(root.get(order.getProperty())));
                }
            });
            query.orderBy(orders);
        }

        // Get results
        List<PaymentTransaction> results = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // Count total
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<PaymentTransaction> countRoot = countQuery.from(PaymentTransaction.class);
        countQuery.select(cb.count(countRoot));

        List<Predicate> countPredicates = buildPredicates(criteria, cb, countRoot);
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    private List<Predicate> buildPredicates(PaymentCriteria criteria, CriteriaBuilder cb,
            Root<PaymentTransaction> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (criteria.getUserId() != null) {
            predicates.add(cb.equal(root.get("userId"), criteria.getUserId()));
        }

        if (criteria.getBookingId() != null) {
            predicates.add(cb.equal(root.get("bookingId"), criteria.getBookingId()));
        }

        if (criteria.getShowtimeId() != null) {
            predicates.add(cb.equal(root.get("showtimeId"), criteria.getShowtimeId()));
        }

        if (criteria.getTransactionRef() != null && !criteria.getTransactionRef().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("transactionRef")),
                    "%" + criteria.getTransactionRef().toLowerCase() + "%"));
        }

        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
        }

        if (criteria.getMethod() != null && !criteria.getMethod().isEmpty()) {
            predicates.add(cb.equal(root.get("method"), criteria.getMethod()));
        }

        if (criteria.getFromDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getFromDate()));
        }

        if (criteria.getToDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getToDate()));
        }

        if (criteria.getMinAmount() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), criteria.getMinAmount()));
        }

        if (criteria.getMaxAmount() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("amount"), criteria.getMaxAmount()));
        }

        return predicates;
    }
}
