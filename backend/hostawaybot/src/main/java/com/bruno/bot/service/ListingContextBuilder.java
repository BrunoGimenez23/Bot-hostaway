package com.bruno.bot.service;

import com.bruno.bot.client.hostaway.dto.HostawayListing;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ListingContextBuilder {

    public String build(HostawayListing l) {

        String address = bestAddress(l);

        // Horas (15 -> "15:00")
        String checkInStart = hour(l.checkInTimeStart());
        String checkInEnd = hour(l.checkInTimeEnd());
        String checkOut = hour(l.checkOutTime());

        // Wifi: si hay user/pass, lo marcamos como "si"
        String wifi = (notBlank(l.wifiUsername()) || notBlank(l.wifiPassword())) ? "si" : "no disponible";

        // Equipamiento/servicios: por ahora lo inferimos desde description (rápido y útil)
        // En tu JSON ya aparece texto sobre "sábanas y toallas", etc.
        String inferredEquipment = inferEquipmentFromDescription(l.description());
        String inferredServices = inferServicesFromDescription(l.description());

        return """
                Propiedad: %s
                Direccion: %s
                Check-in: %s - %s
                Check-out: %s
                Acceso: %s
                Reglas: %s
                wifi: %s
                wifi usuario: %s
                wifi password: %s
                servicios incluidos: %s
                equipamiento: %s
                """.formatted(
                safe(l.internalListingName(), l.name()),
                safe(address),
                safe(checkInStart),
                safe(checkInEnd),
                safe(checkOut),
                safe(firstNonBlank(l.specialInstruction(), l.keyPickup())),
                safe(l.houseRules()),
                safe(wifi),
                safe(l.wifiUsername()),
                safe(l.wifiPassword()),
                safe(inferredServices),
                safe(inferredEquipment)
        ).trim();
    }

    private String bestAddress(HostawayListing l) {
        String a = firstNonBlank(l.publicAddress(), l.address(), l.street());
        if (notBlank(a)) return a.trim();

        // fallback
        String city = safe(l.city());
        String country = safe(l.country());
        if (!"No disponible".equals(city) && !"No disponible".equals(country)) return city + ", " + country;
        return "No disponible";
    }

    private String hour(Integer h) {
        if (h == null) return null;
        // Hostaway manda 15, 23, 11 → lo normalizamos a HH:00
        if (h < 0 || h > 23) return String.valueOf(h);
        return String.format("%02d:00", h);
    }

    private String inferEquipmentFromDescription(String description) {
        String d = norm(description);
        if (d.isBlank()) return null;

        // ejemplos simples
        boolean hasTowels = d.contains("toalla");
        boolean hasSheets = d.contains("sabana") || d.contains("sabanas");
        boolean hasHairDryer = d.contains("secador");

        StringBuilder sb = new StringBuilder();
        if (hasTowels) sb.append("toallas, ");
        if (hasSheets) sb.append("sabanas, ");
        if (hasHairDryer) sb.append("secador de pelo, ");

        String out = sb.toString().trim();
        if (out.endsWith(",")) out = out.substring(0, out.length() - 1).trim();

        return out.isBlank() ? null : out;
    }

    private String inferServicesFromDescription(String description) {
        String d = norm(description);
        if (d.isBlank()) return null;

        // ejemplo: en tu JSON aparece electricidad hasta USD 50 en estadías largas
        if (d.contains("consumo de electricidad")) return "parcial (ver condiciones en la descripcion)";
        return null;
    }

    private String safe(String v) {
        return (v == null || v.isBlank()) ? "No disponible" : v.trim();
    }

    private String safe(String v1, String v2) {
        String x = firstNonBlank(v1, v2);
        return safe(x);
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String norm(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT)
                .replace("á","a").replace("é","e").replace("í","i").replace("ó","o").replace("ú","u")
                .replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
