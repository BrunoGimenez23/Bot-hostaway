package com.bruno.bot.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(prefix = "llm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DemoLlmClient implements LlmClient {

    // ===== Patterns de contexto =====
    private static final Pattern CHECKIN_TIME =
            Pattern.compile("check-?in\\s*:\\s*(\\d{1,2}:\\d{2})", Pattern.CASE_INSENSITIVE);

    private static final Pattern CHECKOUT_TIME =
            Pattern.compile("check-?out\\s*:\\s*(\\d{1,2}:\\d{2})", Pattern.CASE_INSENSITIVE);

    private static final Pattern ADDRESS_LINE =
            Pattern.compile("(?:direcci[oó]n)\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern ACCESS_LINE =
            Pattern.compile("acceso\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern QUESTION_BLOCK =
            Pattern.compile("pregunta\\s*:\\s*(.*)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern TIME_HHMM =
            Pattern.compile("\\b([01]?\\d|2[0-3]):[0-5]\\d\\b");

    private static final Pattern WIFI_LINE =
            Pattern.compile("\\bwifi\\s*:\\s*(si|no|true|false)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern AC_LINE =
            Pattern.compile("\\baire\\s+acondicionado\\s*:\\s*(si|no|true|false)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern SERVICES_INCLUDED_LINE =
            Pattern.compile("\\bservicios\\s+incluidos\\s*:\\s*(si|no|true|false)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern EQUIPMENT_LINE =
            Pattern.compile("\\bequipamiento\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern PARKING_LINE =
            Pattern.compile("\\bestacionamiento\\s*:\\s*(si|no|true|false)\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public String getAnswer(String prompt) {

        String questionRaw = extractQuestion(prompt);
        String q = norm(questionRaw);
        String p = norm(prompt);

        String checkIn = extract(prompt, CHECKIN_TIME);
        String checkOut = extract(prompt, CHECKOUT_TIME);
        String address = extract(prompt, ADDRESS_LINE);
        String access = extract(prompt, ACCESS_LINE);

        Boolean hasWifi = parseYesNo(prompt, WIFI_LINE);
        Boolean hasAC = parseYesNo(prompt, AC_LINE);
        Boolean servicesIncluded = parseYesNo(prompt, SERVICES_INCLUDED_LINE);
        Boolean hasParking = parseYesNo(prompt, PARKING_LINE);
        String equipment = extract(prompt, EQUIPMENT_LINE);

        // ===== Intenciones =====
        boolean asksAccess = containsAny(q, "llave", "llaves", "acceso", "como entro", "codigo", "smart lock");
        boolean asksAddress = containsAny(q, "direccion", "donde queda", "ubicacion", "como llego");

        boolean asksWifi = containsAny(q, "wifi", "internet");

        // FIX 1: NO usar "ac" suelto (dispara por "acabo", "acerca", etc.)
        // Solo frases seguras y/o "a/c".
        boolean asksAC = containsAny(q, "aire acondicionado", "a/c", "aire frio", "aire frio");

        boolean asksParking = containsAny(q, "parking", "estacionar", "estacionamiento", "auto");

        boolean asksEquipment = containsAny(q,
                "toalla", "toallas", "toallon", "sabana", "sabanas", "secador", "sombrilla", "parasol", "ropa de cama"
        );

        boolean mentionsUtilities = containsAny(q, "servicios", "luz", "agua", "gas", "electricidad", "internet");
        boolean asksServices = mentionsUtilities && containsAny(q, "incluye", "incluido", "incluidos", "incluyen");

        boolean asksCheckIn = containsAny(q, "check in", "check-in", "checkin", "entrada");
        boolean asksCheckOut = containsAny(q, "check out", "check-out", "checkout", "salida");

        boolean asksEarly = containsAny(q, "early", "entrar antes", "check in antes", "check-in antes");
        boolean asksLateCheckout = containsAny(q, "late checkout", "late check-out", "salir mas tarde", "checkout mas tarde");
        boolean asksLateArrival = containsAny(q, "llego tarde", "llego a las", "llegamos a las", "llego tipo", "llegamos tipo");

        boolean mentionsTime = TIME_HHMM.matcher(q).find();
        boolean asksPermission = containsAny(q, "puedo", "se puede", "podemos");

        if (asksCheckIn && mentionsTime && asksPermission) {
            asksLateArrival = true;
        }

        boolean asksSmoking = containsAny(q, "fumar", "cigarro", "cigarrillo");
        boolean asksParty = containsAny(q, "fiesta", "ruido", "party");
        boolean asksAvailability = containsAny(q, "disponible", "para hoy");

        // FIX 2: intención reserva más robusta
        boolean asksReservationConfirm = containsAny(
                q,
                "acabo de reservar",
                "te acabo de reservar",
                "ya reserve",
                "ya reservamos",
                "reserva confirmada",
                "reserve para",
                "reservamos para"
        );

        boolean asksBarbecue = containsAny(q, "barbacoa", "parrilla", "asado");

        // ===== PRIORIDAD =====

        // FIX 3: reserva con prioridad máxima (antes que amenities/horarios)
        if (asksReservationConfirm) {
            return "¡Perfecto! La reserva quedó registrada. Cualquier detalle adicional lo coordinamos antes de la llegada.";
        }

        if (asksAccess) {
            return access != null ? ensureDot(access) : "Las instrucciones de acceso se envían antes del check-in.";
        }

        if (asksAddress) {
            return address != null
                    ? "La propiedad queda en " + address + "."
                    : "La dirección se envía antes del check-in.";
        }

        if (asksLateArrival) {
            return "No hay problema con llegadas tarde. El acceso está contemplado y se envía antes del check-in." +
                    (checkIn != null ? " El check-in es a partir de las " + checkIn + "." : "");
        }

        if (asksEarly) {
            return "El early check-in depende de la disponibilidad y se confirma más cerca de la fecha.";
        }

        if (asksLateCheckout) {
            return "El late check-out depende de la disponibilidad y se confirma cerca de la salida.";
        }

        if (asksCheckIn) {
            return checkIn != null
                    ? "El check-in es a partir de las " + checkIn + "."
                    : "El horario de check-in se confirma antes de la llegada.";
        }

        if (asksCheckOut) {
            return checkOut != null
                    ? "El check-out es hasta las " + checkOut + "."
                    : "El horario de check-out se confirma al momento de la salida.";
        }

        if (asksSmoking) {
            if (p.contains("no fumar") || p.contains("no se permite fumar")) return "No está permitido fumar dentro de la propiedad.";
            return "Sobre fumar, te confirmo según las reglas de la propiedad.";
        }

        if (asksParty) {
            if (p.contains("no se permiten fiestas") || p.contains("no fiestas")) return "No se permiten fiestas en la propiedad.";
            return "Sobre reuniones o ruido, depende de las reglas del alojamiento.";
        }

        // ===== EQUIPAMIENTO (ANTES QUE SERVICES) =====
        if (asksEquipment) {
            if (equipment != null && !equipment.trim().isEmpty()) {
                String eq = norm(equipment);

                if (containsAny(q, "sombrilla", "parasol")) {
                    boolean hasUmbrella = containsAny(eq, "sombrilla", "parasol");
                    return hasUmbrella
                            ? "Sí, el alojamiento incluye sombrilla."
                            : "No figura sombrilla en el equipamiento publicado. Si querés, lo confirmo.";
                }

                if (containsAny(q, "secador")) {
                    boolean hasDryer = containsAny(eq, "secador");
                    return hasDryer
                            ? "Sí, el alojamiento incluye secador de pelo."
                            : "No figura secador de pelo en el equipamiento publicado. Si querés, lo confirmo.";
                }

                if (containsAny(q, "toalla", "toallon")) {
                    boolean hasTowels = containsAny(eq, "toalla", "toallas", "toallon");
                    return hasTowels
                            ? "Sí, el alojamiento incluye toallas."
                            : "No figura que incluya toallas. Si querés, lo confirmo.";
                }

                if (containsAny(q, "sabana", "ropa de cama")) {
                    boolean hasSheets = containsAny(eq, "sabana", "sabanas", "ropa de cama");
                    return hasSheets
                            ? "Sí, el alojamiento incluye sábanas/ropa de cama."
                            : "No figura ropa de cama en el equipamiento publicado. Si querés, lo confirmo.";
                }

                return "El alojamiento incluye: " + equipment.trim() + ".";
            }

            return "El equipamiento depende de la propiedad. Si querés, confirmo toallas, sábanas, secador o sombrilla.";
        }

        // ===== SERVICES =====
        if (asksServices) {
            if (servicesIncluded == null) {
                return "Los servicios incluidos dependen del anuncio. Si querés, te confirmo qué incluye exactamente.";
            }
            return servicesIncluded
                    ? "Sí, el precio incluye los servicios habituales (electricidad, agua, gas e internet)."
                    : "No, los servicios no están todos incluidos.";
        }

        if (asksWifi) {
            if (hasWifi == null) return "Te confirmo si la propiedad cuenta con WiFi.";
            return hasWifi ? "Sí, la propiedad cuenta con WiFi." : "No, la propiedad no cuenta con WiFi.";
        }

        if (asksParking) {
            if (hasParking == null) return "Te confirmo las opciones de estacionamiento cercanas.";
            return hasParking
                    ? "Sí, la propiedad cuenta con estacionamiento."
                    : "No cuenta con estacionamiento privado, pero hay opciones cercanas.";
        }

        if (asksAC) {
            if (hasAC == null) return "Te confirmo si la propiedad tiene aire acondicionado.";
            return hasAC ? "Sí, cuenta con aire acondicionado." : "No cuenta con aire acondicionado.";
        }

        if (asksAvailability) {
            return "La disponibilidad se confirma al momento de la reserva.";
        }

        if (asksBarbecue) {
            return "El uso de la barbacoa depende de disponibilidad y reglas del alojamiento.";
        }

        return "Gracias por tu consulta. Te respondo según la información disponible.";
    }

    // ===== Helpers =====

    private String extractQuestion(String prompt) {
        Matcher m = QUESTION_BLOCK.matcher(prompt);
        return m.find() ? m.group(1).trim() : prompt;
    }

    private String extract(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private boolean containsAny(String text, String... keys) {
        for (String k : keys) if (text.contains(k)) return true;
        return false;
    }

    private String norm(String s) {
        if (s == null) return "";
        String x = s.toLowerCase(Locale.ROOT);
        x = Normalizer.normalize(x, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        x = x.replaceAll("[^a-z0-9:\\s]", " ");
        return x.replaceAll("\\s+", " ").trim();
    }

    private Boolean parseYesNo(String text, Pattern p) {
        Matcher m = p.matcher(text);
        if (!m.find()) return null;
        String v = m.group(1).toLowerCase(Locale.ROOT);
        return v.equals("si") || v.equals("true");
    }

    private String ensureDot(String s) {
        String t = s.trim();
        return t.endsWith(".") ? t : t + ".";
    }
}
