package com.bruno.bot.controller;

import com.bruno.bot.client.hostaway.HostawayTokenManager;
import com.bruno.bot.config.HostawayProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final HostawayProperties props;
    private final HostawayTokenManager tokenManager;

    public HealthController(HostawayProperties props, HostawayTokenManager tokenManager) {
        this.props = props;
        this.tokenManager = tokenManager;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {

        boolean hasCreds = props.accountId() != null && !props.accountId().isBlank()
                && props.clientSecret() != null && !props.clientSecret().isBlank();

        boolean ready = tokenManager.isReady();

        return Map.of(
                "status", "OK",
                "hostawayEnabled", props.enabled(),
                "hostawayCredentialsPresent", hasCreds,
                "hostawayReady", ready,
                "mode", (props.enabled() && hasCreds && ready) ? "HOSTAWAY" : "DEMO"
        );
    }
}
