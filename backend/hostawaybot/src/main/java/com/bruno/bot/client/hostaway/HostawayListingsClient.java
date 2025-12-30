package com.bruno.bot.client.hostaway;

import com.bruno.bot.client.hostaway.dto.HostawayListing;
import com.bruno.bot.client.hostaway.dto.HostawayStandardResponse;
import com.bruno.bot.config.HostawayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class HostawayListingsClient {

    private static final ParameterizedTypeReference<HostawayStandardResponse<HostawayListing>> LISTING_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final HostawayTokenManager tokenManager;
    private final HostawayProperties props;
    private final ObjectMapper objectMapper;

    public HostawayListingsClient(RestClient.Builder builder,
                                  HostawayTokenManager tokenManager,
                                  HostawayProperties props,
                                  ObjectMapper objectMapper) {
        this.restClient = builder.baseUrl(props.baseUrl()).build();
        this.tokenManager = tokenManager;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public HostawayListing getListing(long listingId) {
        // Intento 1: token actual
        try {
            return doGetListing(listingId);
        } catch (RestClientResponseException ex) {
            int code = ex.getStatusCode().value();

            // Si es auth, invalidamos y reintentamos una sola vez
            if (code == 401 || code == 403) {
                tokenManager.invalidate();
                return doGetListing(listingId); // intento 2
            }

            // Otros errores: rethrow
            throw ex;
        }
    }

    private HostawayListing doGetListing(long listingId) {
        String token = tokenManager.getToken();

        // 1) Traemos RAW primero para diagnosticar problemas de mapeo / status=fail
        String raw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/listings/{id}")
                        .queryParamIfPresent("includeResources", Optional.ofNullable(props.includeResources()))
                        .build(listingId)
                )
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                // 4xx -> excepción con body
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    throw new RestClientResponseException(
                            "Hostaway " + resp.getStatusCode().value() + " al consultar listingId=" + listingId,
                            resp.getStatusCode().value(),
                            resp.getStatusText(),
                            resp.getHeaders(),
                            resp.getBody() != null ? resp.getBody().readAllBytes() : null,
                            StandardCharsets.UTF_8
                    );
                })
                // 5xx -> excepción con body
                .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                    throw new RestClientResponseException(
                            "Hostaway " + resp.getStatusCode().value() + " al consultar listingId=" + listingId,
                            resp.getStatusCode().value(),
                            resp.getStatusText(),
                            resp.getHeaders(),
                            resp.getBody() != null ? resp.getBody().readAllBytes() : null,
                            StandardCharsets.UTF_8
                    );
                })
                .body(String.class);

        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Hostaway devolvió body vacío para listingId=" + listingId);
        }

        // 2) Si vino "status":"fail", lo dejamos bien explícito en el error
        // (Hostaway a veces devuelve fail+message en vez de result)
        String rawLower = raw.toLowerCase();
        if (rawLower.contains("\"status\":\"fail\"")) {
            System.err.println("HOSTAWAY FAIL RAW listingId=" + listingId + " | body=" + raw);
            throw new IllegalStateException("Hostaway devolvió status=fail para listingId=" + listingId);
        }

        // 3) Parseamos al wrapper esperado
        HostawayStandardResponse<HostawayListing> res;
        try {
            res = objectMapper.readValue(raw,
                    objectMapper.getTypeFactory().constructParametricType(
                            HostawayStandardResponse.class,
                            HostawayListing.class
                    )
            );
        } catch (Exception e) {
            System.err.println("HOSTAWAY PARSE ERROR listingId=" + listingId + " | raw=" + raw);
            throw new IllegalStateException("Error parseando respuesta de Hostaway para listingId=" + listingId, e);
        }

        if (res == null || res.result() == null) {
            System.err.println("HOSTAWAY MISSING RESULT listingId=" + listingId + " | raw=" + raw);
            throw new IllegalStateException("Hostaway no devolvió result para listingId=" + listingId);
        }

        return res.result();
    }
}
