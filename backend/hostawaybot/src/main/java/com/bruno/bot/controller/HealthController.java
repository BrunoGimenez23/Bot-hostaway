package com.bruno.bot.controller;

import com.bruno.bot.config.HostawayProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final HostawayProperties props;

    public HealthController(HostawayProperties props) {
        this.props = props;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        boolean hasCreds = props.accountId() != null && !props.accountId().isBlank()
                && props.clientSecret() != null && !props.clientSecret().isBlank();

        return Map.of(
                "status", "OK",
                "hostawayEnabled", props.enabled(),
                "hostawayCredentialsPresent", hasCreds
        );
    }
}
