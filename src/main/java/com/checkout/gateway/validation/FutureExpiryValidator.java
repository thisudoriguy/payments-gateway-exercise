package com.checkout.gateway.validation;

import com.checkout.gateway.dto.PaymentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.YearMonth;

public class FutureExpiryValidator implements ConstraintValidator<FutureExpiry, PaymentRequest> {

    @Override
    public boolean isValid(PaymentRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getExpiryMonth() == null || request.getExpiryYear() == null) {
            return true;
        }

        int month = request.getExpiryMonth();
        int year = request.getExpiryYear();

        if (month < 1 || month > 12 || year < 1) {
            return false;
        }

        YearMonth expiry = YearMonth.of(year, month);
        return expiry.isAfter(YearMonth.now());
    }
}
