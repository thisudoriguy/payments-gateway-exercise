package com.checkout.gateway.validation;

import com.checkout.gateway.dto.PaymentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private PaymentRequest validRequest() {
        return new PaymentRequest(
                "2222405343248877", 4, 2030, "GBP", 100, "123");
    }

    @Test
    void validRequest_noViolations() {
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(validRequest());
        assertThat(violations).isEmpty();
    }

    @Test
    void cardNumber_tooShort_violation() {
        PaymentRequest request = validRequest();
        request.setCardNumber("1234567890123");
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("between 14 and 19"));
    }

    @Test
    void cardNumber_nonNumeric_violation() {
        PaymentRequest request = validRequest();
        request.setCardNumber("2222ABCD43248877");
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("numeric"));
    }

    @Test
    void expiryDate_inThePast_violation() {
        PaymentRequest request = validRequest();
        request.setExpiryMonth(1);
        request.setExpiryYear(2020);
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("future"));
    }

    @Test
    void currency_unsupported_violation() {
        PaymentRequest request = validRequest();
        request.setCurrency("XYZ");
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("USD, GBP, EUR"));
    }

    @Test
    void amount_zero_violation() {
        PaymentRequest request = validRequest();
        request.setAmount(0);
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertThat(violations).anyMatch(v -> v.getMessage().contains("positive"));
    }

    @Test
    void cvv_tooShort_violation() {
        PaymentRequest request = validRequest();
        request.setCvv("12");
        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }
}
