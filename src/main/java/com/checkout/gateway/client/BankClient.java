package com.checkout.gateway.client;

import com.checkout.gateway.dto.BankRequest;
import com.checkout.gateway.dto.BankResponse;
import com.checkout.gateway.exception.BankUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BankClient {

    private final RestClient restClient;

    public BankClient(@Value("${bank.simulator.url}") String bankUrl, RestClient.Builder builder) {
        this.restClient = builder.baseUrl(bankUrl).build();
    }

    public BankResponse sendPayment(BankRequest request) {
        try {
            return restClient.post()
                    .uri("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(BankResponse.class);
        } catch (HttpServerErrorException e) {
            throw new BankUnavailableException("Bank simulator returned error: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new BankUnavailableException("Failed to communicate with bank simulator", e);
        }
    }
}
