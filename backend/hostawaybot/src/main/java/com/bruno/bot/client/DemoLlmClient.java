package com.bruno.bot.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(prefix = "llm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DemoLlmClient implements LlmClient {

    // Tiempos desde el CONTEXTO (pueden estar en cualquier lugar del prompt)
    private static final Pattern CHECKIN_TIME =
            Pattern.compile("check-?in\\s*(?:[:\\-]|desde|a partir de)?\\s*(\\d{1,2}:\\d{2})", Pattern.CASE_INSENSITIVE);

    private static final Pattern CHECKOUT_TIME =
            Pattern.compile("check-?out\\s*(?:[:\\-]|hasta)?\\s*(\\d{1,2}:\\d{2})", Pattern.CASE_INSENSITIVE);

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

        // 3) Detectar intención SOLO mirando la pregunta
        boolean asksCheckIn = containsAny(q, "entrada", "check-in", "checkin", "llegada");
        boolean asksCheckOut = containsAny(q, "salida", "check-out", "checkout");
        boolean asksWifi = containsAny(q, "wifi", "wi-fi", "internet");
        boolean asksParty = containsAny(q, "fiestas", "fiesta", "party", "ruido");
        boolean asksSmoking = containsAny(q, "fumar", "cigarro", "smoke");
        boolean asksAC = containsAny(q, "aire", "ac", "aire acondicionado");

        if (asksWifi) {
            // Lee el contexto (prompt completo) para decidir si hay WiFi
            if (prompt.toLowerCase(Locale.ROOT).contains("amenities") && prompt.toLowerCase(Locale.ROOT).contains("wifi")) {
                return "Sí, la propiedad cuenta con WiFi.";
            }
            return "En la información disponible no figura WiFi. Si querés, lo confirmo.";
        }

        if (asksParty) {
            if (prompt.toLowerCase(Locale.ROOT).contains("no fiestas")) return "No se permiten fiestas en la propiedad.";
            return "Sobre fiestas/ruido: lo confirmo según las reglas de esta propiedad.";
        }

        if (asksSmoking) {
            if (prompt.toLowerCase(Locale.ROOT).contains("no fumar")) return "No está permitido fumar dentro de la propiedad.";
            return "Sobre fumar: lo confirmo según las reglas de esta propiedad.";
        }

        if (asksAC) {
            if (prompt.toLowerCase(Locale.ROOT).contains("aire acondicionado")) return "Sí, la propiedad cuenta con aire acondicionado.";
            return "En la información disponible no figura aire acondicionado. Si querés, lo confirmo.";
        }

        if (asksCheckIn) {
            if (checkIn != null) return "La entrada (check-in) es a partir de las " + checkIn + ".";
            return "La entrada (check-in) no figura en la información disponible. Si querés, lo confirmo.";
        }

        if (asksCheckOut) {
            if (checkOut != null) return "La salida (check-out) es hasta las " + checkOut + ".";
            return "La salida (check-out) no figura en la información disponible. Si querés, lo confirmo.";
        }

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
