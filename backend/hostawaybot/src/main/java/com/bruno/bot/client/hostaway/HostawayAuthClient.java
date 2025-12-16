package com.bruno.bot.client.hostaway;

import com.bruno.bot.client.hostaway.dto.HostawayTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class HostawayAuthClient {

    private final RestClient restClient;

    public HostawayAuthClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://api.hostaway.com")
                .build();
    }

    public HostawayTokenResponse getAccessToken(String accountId, String clientSecret) {

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", accountId);
        form.add("client_secret", clientSecret);
        form.add("scope", "general");

        return restClient.post()
                .uri("/v1/accessTokens")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(HostawayTokenResponse.class);
    }
}
