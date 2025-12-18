package com.bruno.bot.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(prefix = "llm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DemoLlmClient implements LlmClient {

    // Extraer tiempos desde el CONTEXTO (pueden estar en cualquier lugar del prompt)
    private static final Pattern CHECKIN_TIME =
            Pattern.compile("check-?in\\s*(?:[:\\-]|desde|a partir de)?\\s*(\\d{1,2}:\\d{2})", Pattern.CASE_INSENSITIVE);

    private static final Pattern CHECKOUT_TIME =
            Pattern.compile("check-?out\\s*(?:[:\\-]|hasta)?\\s*(\\d{1,2}:\\d{2})", Pattern.CASE_INSENSITIVE);

    // Extraer bloques del contexto por etiqueta "Dirección:" / "Acceso:" (1 línea)
    private static final Pattern ADDRESS_LINE =
            Pattern.compile("(?:direcci[oó]n)\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern ACCESS_LINE =
            Pattern.compile("acceso\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    // Extraer "Pregunta: ...." (solo lo que el usuario preguntó)
    private static final Pattern QUESTION_BLOCK =
            Pattern.compile("pregunta\\s*:\\s*(.*)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override
    public String getAnswer(String prompt) {
        // 1) Sacar la pregunta real del prompt
        String question = extractQuestion(prompt);
        String q = question.toLowerCase(Locale.ROOT);

        // 2) Extraer datos del contexto (del prompt completo)
        String checkIn = extract(prompt, CHECKIN_TIME);
        String checkOut = extract(prompt, CHECKOUT_TIME);
        String address = extract(prompt, ADDRESS_LINE);
        String access = extract(prompt, ACCESS_LINE);

        String promptLower = prompt.toLowerCase(Locale.ROOT);

        // 3) Detectar intención SOLO mirando la pregunta
        boolean asksCheckIn = containsAny(q, "entrada", "check-in", "checkin", "llegada");
        boolean asksCheckOut = containsAny(q, "salida", "check-out", "checkout");

        boolean asksEarly = containsAny(q,
                "early", "entrar antes", "llegar antes", "llego antes", "llegamos antes",
                "check in antes", "check-in antes", "antes de las");

        boolean asksLate = containsAny(q,
                "late", "salir mas tarde", "salir más tarde", "late checkout", "late check-out",
                "salir despues", "salir después", "despues de las", "después de las");

        boolean asksAccess = containsAny(q,
                "como entro", "cómo entro", "entrar", "acceso", "llaves", "llave",
                "codigo", "código", "smart lock", "cerradura", "caja de llaves", "key", "lock");

        boolean asksAddress = containsAny(q,
                "direccion", "dirección", "donde queda", "dónde queda", "ubicacion", "ubicación",
                "como llego", "cómo llego", "address", "mapa");

        boolean asksWifi = containsAny(q, "wifi", "wi-fi", "internet");
        boolean asksParty = containsAny(q, "fiestas", "fiesta", "party", "ruido");
        boolean asksSmoking = containsAny(q, "fumar", "cigarro", "smoke");
        boolean asksAC = containsAny(q, "aire", "ac", "aire acondicionado");

        // =========================
        // RESPUESTAS POR INTENCIÓN
        // =========================

        // Dirección / ubicación
        if (asksAddress) {
            if (address != null && !address.isBlank()) {
                return "La propiedad queda en " + address.trim() + ".";
            }
            return "La dirección se envía antes del check-in. Si querés, te la confirmo.";
        }

        // Acceso / llaves / ingreso
        if (asksAccess) {
            if (access != null && !access.isBlank()) {
                // Ej: "Acceso: las instrucciones se envían antes del check-in"
                return access.trim() + ".";
            }
            return "Las instrucciones de acceso se envían antes del check-in. Si querés, te confirmo el detalle.";
        }

        // Early check-in
        if (asksEarly) {
            return "El early check-in depende de la disponibilidad del día y se confirma más cerca de la fecha.";
        }

        // Late check-out
        if (asksLate) {
            return "El late check-out depende de la disponibilidad del día y se confirma el mismo día.";
        }

        // WiFi
        if (asksWifi) {
            // Lee el contexto (prompt completo) para decidir si hay WiFi
            if (promptLower.contains("amenities") && promptLower.contains("wifi")) {
                return "Sí, la propiedad cuenta con WiFi.";
            }
            // Si además está explicitado “wifi:” o similar en contexto
            if (promptLower.contains("wifi:") || promptLower.contains("wi-fi:")) {
                return "Sí, la propiedad cuenta con WiFi.";
            }
            return "En la información disponible no figura WiFi. Si querés, lo confirmo.";
        }

        // Fiestas / ruido
        if (asksParty) {
            if (promptLower.contains("no fiestas") || promptLower.contains("no se permiten fiestas")) {
                return "No se permiten fiestas en la propiedad.";
            }
            return "Sobre fiestas/ruido: lo confirmo según las reglas de esta propiedad.";
        }

        // Fumar
        if (asksSmoking) {
            if (promptLower.contains("no fumar") || promptLower.contains("no se permite fumar")) {
                return "No está permitido fumar dentro de la propiedad.";
            }
            return "Sobre fumar: lo confirmo según las reglas de esta propiedad.";
        }

        // Aire acondicionado
        if (asksAC) {
            if (promptLower.contains("aire acondicionado")) {
                return "Sí, la propiedad cuenta con aire acondicionado.";
            }
            return "En la información disponible no figura aire acondicionado. Si querés, lo confirmo.";
        }

        // Check-in / check-out (horarios)
        if (asksCheckIn) {
            if (checkIn != null) return "La entrada (check-in) es a partir de las " + checkIn + ".";
            return "La entrada (check-in) no figura en la información disponible. Si querés, lo confirmo.";
        }

        if (asksCheckOut) {
            if (checkOut != null) return "La salida (check-out) es hasta las " + checkOut + ".";
            return "La salida (check-out) no figura en la información disponible. Si querés, lo confirmo.";
        }

        // Fallback
        return "Gracias por tu consulta. En base a la información disponible, te confirmo y te respondo enseguida.";
    }

    private String extractQuestion(String prompt) {
        Matcher m = QUESTION_BLOCK.matcher(prompt);
        if (m.find()) {
            return m.group(1).trim();
        }
        // fallback: si por alguna razón no está "Pregunta:"
        return prompt;
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
