package com.checkout.gateway.client;

import com.checkout.gateway.dto.BankRequest;
import com.checkout.gateway.dto.BankResponse;
import com.checkout.gateway.exception.BankUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class BankClient {

    private final RestTemplate restTemplate;
    private final String bankUrl;

    public BankClient(@Value("${bank.simulator.url}") String bankUrl) {
        this.restTemplate = new RestTemplate();
        this.bankUrl = bankUrl;
    }

    public BankResponse sendPayment(BankRequest request) {
        try {
            return restTemplate.postForObject(
                    bankUrl + "/payments", request, BankResponse.class);
        } catch (HttpServerErrorException e) {
            throw new BankUnavailableException("Bank simulator returned error: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new BankUnavailableException("Failed to communicate with bank simulator", e);
        }
    }
}
