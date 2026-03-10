package com.checkout.gateway.dto;

import com.checkout.gateway.model.PaymentStatus;

import java.util.UUID;

public class PaymentResponse {

    private UUID id;
    private PaymentStatus status;
    private String lastFourCardDigits;
    private int expiryMonth;
    private int expiryYear;
    private String currency;
    private int amount;

    public PaymentResponse() {
    }

    public PaymentResponse(UUID id, PaymentStatus status, String lastFourCardDigits,
                           int expiryMonth, int expiryYear, String currency, int amount) {
        this.id = id;
        this.status = status;
        this.lastFourCardDigits = lastFourCardDigits;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.currency = currency;
        this.amount = amount;
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

    public String getLastFourCardDigits() {
        return lastFourCardDigits;
    }

    public void setLastFourCardDigits(String lastFourCardDigits) {
        this.lastFourCardDigits = lastFourCardDigits;
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
}
