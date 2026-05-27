package com.example.parking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.example.parking.dto.response.AccountSubscriptionResponse;

@Service
public class AccountClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String accountServiceUrl;

    public AccountClient(@Value("${account.service-url:http://localhost:8081/accounts}") String accountServiceUrl) {
        this.accountServiceUrl = accountServiceUrl;
    }

    public String getSubscription(Long accountId) {
        try {
            AccountSubscriptionResponse response = restTemplate.getForObject(
                    accountServiceUrl + "/internal/" + accountId + "/subscription",
                    AccountSubscriptionResponse.class);

            if (response == null || response.getSubscription() == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account subscription could not be checked");
            }

            return response.getSubscription();
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account subscription could not be checked");
        }
    }
}
