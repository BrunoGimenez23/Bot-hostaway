package com.bruno.bot.service;

import com.bruno.bot.client.hostaway.dto.HostawayListing;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ListingContextBuilder {

    public String build(HostawayListing l) {
        // Hoy tu DTO no trae amenities/access/equipment, así que quedan "No disponible".
        // Cuando agregues campos (o resources), estos métodos pueden empezar a devolver valores reales.

        String access = extractAccess(l);
        String wifi = extractWifi(l);
        String ac = extractAC(l);
        String servicesIncluded = extractServicesIncluded(l);
        String equipment = extractEquipment(l);
        String parking = extractParking(l);

        return """
                Propiedad: %s
                Direccion: %s
                Check-in: %s - %s
                Check-out: %s
                Acceso: %s
                Reglas: %s
                wifi: %s
                aire acondicionado: %s
                servicios incluidos: %s
                equipamiento: %s
                estacionamiento: %s
                """.formatted(
                safe(l.name()),
                safeAddress(l),
                safe(l.checkInTimeStart()),
                safe(l.checkInTimeEnd()),
                safe(l.checkOutTime()),
                safe(access),
                safe(l.houseRules()),
                safe(wifi),
                safe(ac),
                safe(servicesIncluded),
                safe(equipment),
                safe(parking)
        ).trim();
    }

    // ===== Helpers =====
    private String safe(String v) {
        return (v == null || v.isBlank()) ? "No disponible" : v.trim();
    }

    private String safeAddress(HostawayListing l) {
        // Con tu DTO actual solo tenemos city/country
        String city = safe(l.city());
        String country = safe(l.country());
        if (!"No disponible".equals(city) && !"No disponible".equals(country)) {
            return city + ", " + country;
        }
        return "No disponible".equals(city) ? country : city;
    }

    /**
     * Normaliza texto (por si después parseás amenities desde strings).
     */
    private String norm(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // ===== Extractors (hoy retornan null porque el DTO no tiene esos campos) =====

    private String extractAccess(HostawayListing l) {
        // Cuando tengas el campo real: return l.accessInstructions();
        return null;
    }

    private String extractWifi(HostawayListing l) {
        // Cuando tengas amenities reales:
        // - si trae lista: buscar "wifi" y devolver "si"/"no"
        // - si no trae: null
        return null;
    }

    private String extractAC(HostawayListing l) {
        return null;
    }

    private String extractServicesIncluded(HostawayListing l) {
        // En general Hostaway no lo da como boolean; si lo tenés en descripción/reglas lo podés parsear.
        return null;
    }

    private String extractEquipment(HostawayListing l) {
        return null;
    }

    private String extractParking(HostawayListing l) {
        return null;
    }
}
