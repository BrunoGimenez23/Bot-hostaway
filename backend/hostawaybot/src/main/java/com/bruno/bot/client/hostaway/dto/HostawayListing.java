package com.bruno.bot.client.hostaway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HostawayListing(
        Long id,
        String name,
        String city,
        String country,
        String checkInTimeStart,
        String checkInTimeEnd,
        String checkOutTime,
        String houseRules
) {}
