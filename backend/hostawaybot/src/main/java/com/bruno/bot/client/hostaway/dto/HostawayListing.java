package com.bruno.bot.client.hostaway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HostawayListing(
        Long id,
        String name,
        String internalListingName,

        // Dirección
        String street,
        String address,
        String publicAddress,
        String zipcode,
        String city,
        String country,

        // Horarios (Hostaway los manda como números)
        Integer checkInTimeStart,
        Integer checkInTimeEnd,
        Integer checkOutTime,

        // Reglas / instrucciones
        String houseRules,
        String specialInstruction,
        String keyPickup,

        // WiFi (Hostaway lo manda directo)
        String wifiUsername,
        String wifiPassword,

        // Texto útil para equipamiento/servicios (podés parsear de acá)
        String description
) {}
