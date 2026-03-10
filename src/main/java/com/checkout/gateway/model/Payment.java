package com.checkout.gateway.model;

import java.util.UUID;

public class Payment {

    private UUID id;
    private PaymentStatus status;
    private String cardNumberLastFour;
    private int expiryMonth;
    private int expiryYear;
    private String currency;
    private int amount;
    private String authorizationCode;

    public Payment() {
    }

    public Payment(UUID id, PaymentStatus status, String cardNumberLastFour,
                   int expiryMonth, int expiryYear, String currency, int amount,
                   String authorizationCode) {
        this.id = id;
        this.status = status;
        this.cardNumberLastFour = cardNumberLastFour;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.currency = currency;
        this.amount = amount;
        this.authorizationCode = authorizationCode;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getCardNumberLastFour() {
        return cardNumberLastFour;
    }

    public void setCardNumberLastFour(String cardNumberLastFour) {
        this.cardNumberLastFour = cardNumberLastFour;
    }

    public int getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(int expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public int getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(int expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }
}
