package com.checkout.gateway.service;

import com.checkout.gateway.client.BankClient;
import com.checkout.gateway.dto.BankRequest;
import com.checkout.gateway.dto.BankResponse;
import com.checkout.gateway.dto.PaymentRequest;
import com.checkout.gateway.dto.PaymentResponse;
import com.checkout.gateway.exception.PaymentNotFoundException;
import com.checkout.gateway.model.Payment;
import com.checkout.gateway.model.PaymentStatus;
import com.checkout.gateway.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentService {

    private final BankClient bankClient;
    private final PaymentRepository paymentRepository;

    public PaymentService(BankClient bankClient, PaymentRepository paymentRepository) {
        this.bankClient = bankClient;
        this.paymentRepository = paymentRepository;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        BankRequest bankRequest = toBankRequest(request);
        BankResponse bankResponse = bankClient.sendPayment(bankRequest);

        PaymentStatus status = bankResponse.isAuthorized()
                ? PaymentStatus.AUTHORIZED
                : PaymentStatus.DECLINED;

        String lastFour = request.getCardNumber()
                .substring(request.getCardNumber().length() - 4);

        Payment payment = new Payment(
                UUID.randomUUID(),
                status,
                lastFour,
                request.getExpiryMonth(),
                request.getExpiryYear(),
                request.getCurrency(),
                request.getAmount(),
                bankResponse.getAuthorizationCode()
        );

        paymentRepository.save(payment);

        return toPaymentResponse(payment);
    }

    public PaymentResponse getPayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return toPaymentResponse(payment);
    }

    private BankRequest toBankRequest(PaymentRequest request) {
        String expiryDate = String.format("%02d/%d", request.getExpiryMonth(), request.getExpiryYear());
        return new BankRequest(
                request.getCardNumber(),
                expiryDate,
                request.getCurrency(),
                request.getAmount(),
                request.getCvv()
        );
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getStatus(),
                payment.getCardNumberLastFour(),
                payment.getExpiryMonth(),
                payment.getExpiryYear(),
                payment.getCurrency(),
                payment.getAmount()
        );
    }
}
