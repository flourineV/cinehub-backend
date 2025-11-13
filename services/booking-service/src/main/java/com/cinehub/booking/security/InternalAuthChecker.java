package com.cinehub.booking.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class InternalAuthChecker {

    @Value("${app.internal.secret-key}")
    private String internalSecretKey;

    public void requireInternal(String headerKey) {
        if (headerKey == null || !headerKey.equals(internalSecretKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal service key");
        }
    }
}
