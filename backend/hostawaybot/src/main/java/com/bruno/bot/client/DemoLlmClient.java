package com.bruno.bot.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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

    @Override
    public String getAnswer(String prompt) {

        String question = extractQuestion(prompt);
        String q = question.toLowerCase(Locale.ROOT);
        String promptLower = prompt.toLowerCase(Locale.ROOT);

        String checkIn = extract(prompt, CHECKIN_TIME);
        String checkOut = extract(prompt, CHECKOUT_TIME);
        String address = extract(prompt, ADDRESS_LINE);
        String access = extract(prompt, ACCESS_LINE);

        // ===== Intenciones =====
        boolean asksCheckIn = containsAny(q, "check-in", "checkin", "entrada", "llegada");
        boolean asksCheckOut = containsAny(q, "check-out", "checkout", "salida");

        boolean asksEarly = containsAny(q, "early", "entrar antes", "llegar antes", "llego antes", "check in antes", "check-in antes");
        boolean asksLate = containsAny(q, "late", "salir más tarde", "salir mas tarde", "llego tarde", "llegar tarde", "late checkout", "late check-out");

        boolean asksAccess = containsAny(q, "llave", "llaves", "acceso", "cómo entro", "como entro", "codigo", "código", "smart lock", "caja de llaves");
        boolean asksAddress = containsAny(q, "dirección", "direccion", "dónde queda", "donde queda", "ubicación", "ubicacion", "cómo llego", "como llego");

        boolean asksWifi = containsAny(q, "wifi", "wi-fi", "internet");
        boolean asksAC = containsAny(q, "aire", "aire acondicionado", "ac");

        boolean asksParking = containsAny(q, "auto", "estacionar", "parking", "estacionamiento");
        boolean asksServices = containsAny(q, "servicios", "luz", "gas", "electricidad", "incluye", "incluido", "incluye todos los servicios");
        boolean asksEquipment = containsAny(q, "toall", "toallon", "toallón", "sábana", "sabana", "sábanas", "sabana", "secador", "sombrilla");

        boolean asksAvailability = containsAny(q, "disponible", "para hoy", "esta disponible", "está disponible", "disponibilidad");
        boolean asksReservationConfirm = containsAny(q, "reservé", "reserve", "reservé", "acabo de reservar", "reserva confirmada", "te acabo de reservar");

        boolean asksBarbecue = containsAny(q, "barbacoa", "asado", "cumple", "cumpleaños", "parrilla");

        // NUEVO: Fumar y fiestas (lo que te faltaba)
        boolean asksSmoking = containsAny(q, "fumar", "cigarro", "cigarrillo", "smoke");
        boolean asksParty = containsAny(q, "fiesta", "fiestas", "party", "ruido", "cumple", "cumpleaños");

        // ===== Respuestas =====

        // Check-in / check-out
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

        // Early / late
        if (asksEarly) {
            return "El early check-in depende de la disponibilidad y se confirma más cerca de la fecha.";
        }

        if (asksLate) {
            return "No hay problema con llegadas tarde. El acceso está contemplado y se envía antes del check-in.";
        }

        // Acceso / dirección
        if (asksAccess) {
            return access != null
                    ? access + "."
                    : "Las instrucciones de acceso se envían antes del check-in.";
        }

        if (asksAddress) {
            return address != null
                    ? "La propiedad queda en " + address + "."
                    : "La dirección se envía antes del check-in.";
        }

        // Reglas: fumar
        if (asksSmoking) {
            if (promptLower.contains("no fumar") || promptLower.contains("no se permite fumar")) {
                return "No está permitido fumar dentro de la propiedad.";
            }
            return "Sobre fumar, te confirmo según las reglas de la propiedad.";
        }

        // Reglas: fiestas / ruido
        // Arreglado: detecta "no fiestas" / "no se permiten fiestas" en el contexto
        // y además evita confundir "cumpleaños tranquilo" con "fiesta permitida".
        if (asksParty) {
            if (promptLower.contains("no fiestas") || promptLower.contains("no se permiten fiestas")) {
                return "No se permiten fiestas en la propiedad.";
            }
            // Si el usuario aclara que es algo tranquilo, respondé neutral sin habilitar “fiesta”
            if (containsAny(q, "tranquilo", "nada grande", "sin alcohol", "asado íntimo", "intimo", "íntimo")) {
                return "Gracias por la aclaración. Igual, el uso para reuniones depende de las reglas del alojamiento y disponibilidad. Te lo confirmo.";
            }
            return "Sobre fiestas/ruido, lo confirmo según las reglas del alojamiento.";
        }

        // Amenities
        if (asksWifi) {
            return promptLower.contains("wifi")
                    ? "Sí, la propiedad cuenta con WiFi."
                    : "El servicio de internet se confirma según la propiedad.";
        }

        if (asksAC) {
            return promptLower.contains("aire acondicionado")
                    ? "Sí, la propiedad cuenta con aire acondicionado."
                    : "El aire acondicionado se confirma según la propiedad.";
        }

        // Consultas frecuentes reales
        if (asksParking) {
            // Si en el contexto aparece algo de estacionamiento, lo usamos; sino, neutral
            if (promptLower.contains("estacionamiento:")) {
                return "Sobre estacionamiento: reviso la info y te confirmo las opciones disponibles cerca del alojamiento.";
            }
            return "Sobre estacionamiento, te confirmo las opciones disponibles cerca del alojamiento.";
        }

        if (asksServices) {
            if (promptLower.contains("servicios incluidos")) {
                return "Sí, el precio incluye los servicios habituales (electricidad, internet, gas, etc.). Si querés, te confirmo algún detalle puntual.";
            }
            return "Los servicios habituales del alojamiento están incluidos. Si necesitás confirmar alguno en particular, lo reviso.";
        }

        if (asksEquipment) {
            if (promptLower.contains("equipamiento:")) {
                return "Sí, el alojamiento cuenta con equipamiento básico (toallas/sábanas). Si querés, te confirmo si incluye secador o sombrilla según esta propiedad.";
            }
            return "El alojamiento cuenta con equipamiento básico. Si necesitás confirmar algún elemento puntual, lo verifico.";
        }

        if (asksAvailability) {
            return "La disponibilidad se confirma al momento de la reserva según el calendario.";
        }

        if (asksReservationConfirm) {
            return "Perfecto, la reserva quedó registrada. Coordinamos cualquier detalle antes de la llegada.";
        }

        if (asksBarbecue) {
            return "El uso de la barbacoa depende de disponibilidad y reglas del alojamiento. Te confirmo si está habilitada para esa fecha.";
        }

        // Fallback
        return "Gracias por tu consulta. Te respondo en base a la información disponible y confirmo cualquier detalle adicional.";
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
        for (String k : keys) {
            if (text.contains(k)) return true;
        }
        return false;
    }
}
