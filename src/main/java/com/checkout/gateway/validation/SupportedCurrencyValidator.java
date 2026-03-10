package com.checkout.gateway.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class SupportedCurrencyValidator implements ConstraintValidator<SupportedCurrency, String> {

    private static final Set<String> SUPPORTED = Set.of("USD", "GBP", "EUR");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return SUPPORTED.contains(value.toUpperCase());
    }
}
