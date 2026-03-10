package com.checkout.gateway.dto;

import com.checkout.gateway.validation.FutureExpiry;
import com.checkout.gateway.validation.NumericOnly;
import com.checkout.gateway.validation.SupportedCurrency;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@FutureExpiry
public class PaymentRequest {

    @NotBlank(message = "Card number is required")
    @Size(min = 14, max = 19, message = "Card number must be between 14 and 19 characters")
    @NumericOnly(message = "Card number must contain only numeric characters")
    private String cardNumber;

    @NotNull(message = "Expiry month is required")
    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    private Integer expiryMonth;

    @NotNull(message = "Expiry year is required")
    private Integer expiryYear;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @SupportedCurrency
    private String currency;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be a positive integer")
    private Integer amount;

    @NotBlank(message = "CVV is required")
    @Size(min = 3, max = 4, message = "CVV must be 3 or 4 characters")
    @NumericOnly(message = "CVV must contain only numeric characters")
    private String cvv;

    public PaymentRequest() {
    }

    public PaymentRequest(String cardNumber, int expiryMonth, int expiryYear,
                          String currency, int amount, String cvv) {
        this.cardNumber = cardNumber;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.currency = currency;
        this.amount = amount;
        this.cvv = cvv;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(Integer expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(Integer expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }
}
