package com.bruno.bot.client.hostaway;

import com.bruno.bot.client.hostaway.dto.HostawayListing;
import com.bruno.bot.client.hostaway.dto.HostawayStandardResponse;
import com.bruno.bot.config.HostawayProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HostawayListingsClient {

    private static final ParameterizedTypeReference<HostawayStandardResponse<HostawayListing>> LISTING_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final HostawayTokenManager tokenManager;
    private final HostawayProperties props;

    public HostawayListingsClient(RestClient.Builder builder, HostawayTokenManager tokenManager, HostawayProperties props) {
        this.restClient = builder.baseUrl(props.baseUrl()).build();
        this.tokenManager = tokenManager;
        this.props = props;
    }

    public HostawayListing getListing(long listingId) {
        String token = tokenManager.getToken();

        var res = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/listings/{id}")
                        .queryParamIfPresent("includeResources", java.util.Optional.ofNullable(props.includeResources()))
                        .build(listingId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    // si token inválido, invalidamos para que el próximo intento regenere
                    if (resp.getStatusCode().value() == 401 || resp.getStatusCode().value() == 403) {
                        tokenManager.invalidate();
                    }
                })
                .body(LISTING_TYPE);

        if (res == null || res.result() == null) {
            throw new IllegalStateException("Hostaway no devolvió result para listingId=" + listingId);
        }
        return res.result();
    }
}
