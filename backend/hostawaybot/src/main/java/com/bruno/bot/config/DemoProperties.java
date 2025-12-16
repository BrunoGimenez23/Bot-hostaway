package com.bruno.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo")
public record DemoProperties(
        String fallbackContext
) {}
