package com.bruno.bot.client.hostaway;

import com.bruno.bot.client.hostaway.dto.HostawayTokenResponse;
import com.bruno.bot.config.HostawayProperties;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HostawayAuthClient {

    private final RestClient restClient;

    public HostawayAuthClient(RestClient.Builder builder, HostawayProperties props) {
        this.restClient = builder
                .baseUrl(props.baseUrl())
                .build();
    }

    public HostawayTokenResponse getAccessToken(String accountId, String clientSecret) {

        if (accountId == null || accountId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("accountId/clientSecret are required to request Hostaway token");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", accountId);
        form.add("client_secret", clientSecret);
        form.add("scope", "general");

        HostawayTokenResponse res = restClient.post()
                .uri("/v1/accessTokens")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    throw new RestClientResponseException(
                            "Hostaway auth 4xx (invalid credentials / forbidden / rate-limit)",
                            resp.getStatusCode().value(),
                            resp.getStatusText(),
                            resp.getHeaders(),
                            resp.getBody() != null ? resp.getBody().readAllBytes() : null,
                            null
                    );
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                    throw new RestClientResponseException(
                            "Hostaway auth 5xx (Hostaway error)",
                            resp.getStatusCode().value(),
                            resp.getStatusText(),
                            resp.getHeaders(),
                            resp.getBody() != null ? resp.getBody().readAllBytes() : null,
                            null
                    );
                })
                .body(HostawayTokenResponse.class);

        if (res == null || res.access_token() == null || res.access_token().isBlank()) {
            throw new IllegalStateException("Hostaway auth returned empty access_token");
        }

        // expires_in debería venir, pero por seguridad no fallamos acá; lo maneja TokenManager.
        return res;
    }
}
