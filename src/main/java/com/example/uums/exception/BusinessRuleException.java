package com.example.uums.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a business/domain rule is violated (HTTP 422).
 * Used for validation failures that are not simple field errors — e.g. attempting
 * to capture a reading for an inactive meter or pay more than the outstanding balance.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
