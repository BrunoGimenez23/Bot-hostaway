package com.bruno.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hostaway")
public record HostawayProperties(
        boolean enabled,
        String baseUrl,
        String accountId,
        String clientSecret,
        Integer includeResources
) {}
