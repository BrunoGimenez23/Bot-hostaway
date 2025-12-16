package com.bruno.bot.client.hostaway.dto;

public record HostawayTokenResponse(
        String token_type,
        long expires_in,
        String access_token
) {}
