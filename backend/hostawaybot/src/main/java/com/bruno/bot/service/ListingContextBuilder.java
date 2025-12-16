package com.bruno.bot.service;

import com.bruno.bot.client.hostaway.dto.HostawayListing;
import org.springframework.stereotype.Component;

@Component
public class ListingContextBuilder {

    public String build(HostawayListing l) {
        return """
                Propiedad: %s
                Ubicación: %s, %s
                Check-in: %s - %s
                Check-out: %s
                Reglas: %s
                """.formatted(
                safe(l.name()),
                safe(l.city()),
                safe(l.country()),
                safe(l.checkInTimeStart()),
                safe(l.checkInTimeEnd()),
                safe(l.checkOutTime()),
                safe(l.houseRules())
        ).trim();
    }

    private String safe(String v) {
        return (v == null || v.isBlank()) ? "No disponible" : v.trim();
    }
}
