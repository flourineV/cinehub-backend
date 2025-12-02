package com.cinehub.payment.repository;

import com.cinehub.payment.dto.request.PaymentCriteria;
import com.cinehub.payment.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentRepositoryCustom {
    Page<PaymentTransaction> findByCriteria(PaymentCriteria criteria, Pageable pageable);
}
