package com.checkout.gateway.controller;

import com.checkout.gateway.client.BankClient;
import com.checkout.gateway.dto.BankResponse;
import com.checkout.gateway.dto.PaymentRequest;
import com.checkout.gateway.exception.BankUnavailableException;
import com.checkout.gateway.model.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @MockitoBean
    private BankClient bankClient;

    @Test
    void processPayment_authorized_returns200() throws Exception {
        when(bankClient.sendPayment(any())).thenReturn(
                new BankResponse(true, "auth-code-123"));

        PaymentRequest request = new PaymentRequest(
                "2222405343248877", 4, 2030, "GBP", 100, "123");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", is("Authorized")))
                .andExpect(jsonPath("$.lastFourCardDigits", is("8877")))
                .andExpect(jsonPath("$.expiryMonth", is(4)))
                .andExpect(jsonPath("$.expiryYear", is(2030)))
                .andExpect(jsonPath("$.currency", is("GBP")))
                .andExpect(jsonPath("$.amount", is(100)));
    }

    @Test
    void processPayment_declined_returns200WithDeclined() throws Exception {
        when(bankClient.sendPayment(any())).thenReturn(
                new BankResponse(false, null));

        PaymentRequest request = new PaymentRequest(
                "2222405343248878", 4, 2030, "USD", 50, "456");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("Declined")))
                .andExpect(jsonPath("$.lastFourCardDigits", is("8878")));
    }

    @Test
    void processPayment_invalidRequest_returns400Rejected() throws Exception {
        PaymentRequest request = new PaymentRequest(
                "123", 4, 2030, "GBP", 100, "123");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("Rejected")));

        verify(bankClient, never()).sendPayment(any());
    }

    @Test
    void processPayment_bankUnavailable_returns502() throws Exception {
        when(bankClient.sendPayment(any())).thenThrow(
                new BankUnavailableException("Bank simulator returned error: 503"));

        PaymentRequest request = new PaymentRequest(
                "2222405343248877", 4, 2030, "GBP", 100, "123");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    void getPayment_afterProcessing_returnsPaymentDetails() throws Exception {
        when(bankClient.sendPayment(any())).thenReturn(
                new BankResponse(true, "auth-code-789"));

        PaymentRequest request = new PaymentRequest(
                "2222405343248877", 6, 2029, "EUR", 999, "321");

        String createResult = mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String paymentId = jsonMapper.readTree(createResult).get("id").asText();

        mockMvc.perform(get("/api/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(paymentId)))
                .andExpect(jsonPath("$.status", is("Authorized")))
                .andExpect(jsonPath("$.lastFourCardDigits", is("8877")))
                .andExpect(jsonPath("$.expiryMonth", is(6)))
                .andExpect(jsonPath("$.expiryYear", is(2029)))
                .andExpect(jsonPath("$.currency", is("EUR")))
                .andExpect(jsonPath("$.amount", is(999)));
    }

    @Test
    void getPayment_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/payments/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", notNullValue()));
    }
}
