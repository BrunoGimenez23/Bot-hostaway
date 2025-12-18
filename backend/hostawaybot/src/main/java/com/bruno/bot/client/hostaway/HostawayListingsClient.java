package com.bruno.bot.client.hostaway;

import com.bruno.bot.client.hostaway.dto.HostawayListing;
import com.bruno.bot.client.hostaway.dto.HostawayStandardResponse;
import com.bruno.bot.config.HostawayProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Optional;

@Component
public class HostawayListingsClient {

    private static final ParameterizedTypeReference<HostawayStandardResponse<HostawayListing>> LISTING_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final HostawayTokenManager tokenManager;
    private final HostawayProperties props;

    public HostawayListingsClient(RestClient.Builder builder,
                                  HostawayTokenManager tokenManager,
                                  HostawayProperties props) {
        this.restClient = builder.baseUrl(props.baseUrl()).build();
        this.tokenManager = tokenManager;
        this.props = props;
    }

    public HostawayListing getListing(long listingId) {
        // Intento 1: token actual
        try {
            return doGetListing(listingId);
        } catch (RestClientResponseException ex) {
            // Si es auth, invalidamos y reintentamos una sola vez
            int code = ex.getStatusCode().value();
            if (code == 401 || code == 403) {
                tokenManager.invalidate();
                return doGetListing(listingId); // intento 2
            }
            throw ex;
        }
    }

    private HostawayListing doGetListing(long listingId) {
        String token = tokenManager.getToken();

        HostawayStandardResponse<HostawayListing> res = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/listings/{id}")
                        // includeResources puede ser null; si es Integer, queda perfecto
                        .queryParamIfPresent("includeResources", Optional.ofNullable(props.includeResources()))
                        .build(listingId)
                )
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                // 4xx: tiramos excepción (y si es 401/403, arriba reintentamos)
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    throw new RestClientResponseException(
                            "Hostaway 4xx al consultar listingId=" + listingId,
                            resp.getStatusCode().value(),
                            resp.getStatusText(),
                            resp.getHeaders(),
                            resp.getBody() != null ? resp.getBody().readAllBytes() : null,
                            null
                    );
                })
                // 5xx: también tiramos excepción
                .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                    throw new RestClientResponseException(
                            "Hostaway 5xx al consultar listingId=" + listingId,
                            resp.getStatusCode().value(),
                            resp.getStatusText(),
                            resp.getHeaders(),
                            resp.getBody() != null ? resp.getBody().readAllBytes() : null,
                            null
                    );
                })
                .body(LISTING_TYPE);

        if (res == null || res.result() == null) {
            throw new IllegalStateException("Hostaway no devolvió result para listingId=" + listingId);
        }
        return res.result();
    }
}
