package com.checkout.gateway.service;

import com.checkout.gateway.client.BankClient;
import com.checkout.gateway.dto.BankResponse;
import com.checkout.gateway.dto.PaymentRequest;
import com.checkout.gateway.dto.PaymentResponse;
import com.checkout.gateway.exception.PaymentNotFoundException;
import com.checkout.gateway.model.Payment;
import com.checkout.gateway.model.PaymentStatus;
import com.checkout.gateway.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private BankClient bankClient;

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(bankClient, paymentRepository);
    }

    @Test
    void processPayment_authorized_returnsAuthorizedResponse() {
        PaymentRequest request = new PaymentRequest(
                "2222405343248877", 4, 2030, "GBP", 100, "123");

        when(bankClient.sendPayment(any())).thenReturn(
                new BankResponse(true, "auth-code-123"));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(response.getLastFourCardDigits()).isEqualTo("8877");
        assertThat(response.getExpiryMonth()).isEqualTo(4);
        assertThat(response.getExpiryYear()).isEqualTo(2030);
        assertThat(response.getCurrency()).isEqualTo("GBP");
        assertThat(response.getAmount()).isEqualTo(100);
        assertThat(response.getId()).isNotNull();
    }

    @Test
    void processPayment_declined_returnsDeclinedResponse() {
        PaymentRequest request = new PaymentRequest(
                "2222405343248878", 4, 2030, "GBP", 100, "123");

        when(bankClient.sendPayment(any())).thenReturn(
                new BankResponse(false, null));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(response.getLastFourCardDigits()).isEqualTo("8878");
    }

    @Test
    void getPayment_existingId_returnsPayment() {
        UUID id = UUID.randomUUID();
        Payment payment = new Payment(id, PaymentStatus.AUTHORIZED, "8877",
                4, 2030, "GBP", 100, "auth-code");

        when(paymentRepository.findById(id)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPayment(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(response.getLastFourCardDigits()).isEqualTo("8877");
    }

    @Test
    void getPayment_nonExistentId_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(paymentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(id))
                .isInstanceOf(PaymentNotFoundException.class);
    }
}
