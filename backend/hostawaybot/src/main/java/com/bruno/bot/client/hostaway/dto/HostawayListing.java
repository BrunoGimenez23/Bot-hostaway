package com.bruno.bot.client.hostaway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HostawayListing(
        Long id,
        String name,

        // ubicación
        String city,
        String country,
        String address,        // a veces existe como address / street / fullAddress
        String street,
        String zipcode,
        String state,
        Double latitude,
        Double longitude,

        // horarios
        String checkInTimeStart,
        String checkInTimeEnd,
        String checkOutTime,

        // reglas / instrucciones
        String houseRules,
        String accessInstructions, // si existiera algo así

        // amenities (según API pueden venir como lista o como map)
        List<String> amenities,
        Map<String, Object> listingAmenities,  // fallback si viene anidado

        // otros recursos (si includeResources devuelve algo anidado)
        Map<String, Object> resources
) {}
