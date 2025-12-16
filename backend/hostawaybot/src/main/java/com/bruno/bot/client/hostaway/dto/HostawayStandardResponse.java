package com.bruno.bot.client.hostaway.dto;

public record HostawayStandardResponse<T>(
        String status,
        T result
) {}
