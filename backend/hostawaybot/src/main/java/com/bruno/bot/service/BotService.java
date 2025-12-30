package com.bruno.bot.service;

import com.bruno.bot.client.LlmClient;
import com.bruno.bot.config.HostawayProperties;
import com.bruno.bot.dto.BotQuestionRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class BotService {

    private final LlmClient llmClient;
    private final HostawayProperties hostawayProperties;
    private final DemoListingContextProvider demoProvider;
    private final HostawayListingContextProvider hostawayProvider; // puede ser null si enabled=false

    public BotService(
            LlmClient llmClient,
            HostawayProperties hostawayProperties,
            DemoListingContextProvider demoProvider,
            ObjectProvider<HostawayListingContextProvider> hostawayProvider
    ) {
        this.llmClient = llmClient;
        this.hostawayProperties = hostawayProperties;
        this.demoProvider = demoProvider;
        this.hostawayProvider = hostawayProvider.getIfAvailable();
    }

    /**
     * Mantiene compatibilidad con tu controller actual (String).
     * Internamente calcula meta para que puedas debuggear fácil.
     */
    public String answerQuestion(BotQuestionRequest request) {
        return answerQuestionWithMeta(request).answer();
    }

    /**
     * Útil para devolver headers tipo X-Bot-Mode desde el controller.
     */
    public AnswerResult answerQuestionWithMeta(BotQuestionRequest request) {
        String lang = (request.language() == null || request.language().isBlank())
                ? "es"
                : request.language().trim();

        Long listingId = request.listingId();
        String question = (request.message() == null) ? "" : request.message().trim();

        ContextResult ctx = resolveContextWithFallback(listingId);

        String prompt = """
                Eres un asistente para huéspedes.
                Responde corto y claro en el idioma indicado.
                Si la información no está en el contexto, decí que lo vas a confirmar.
                No inventes datos.

                Modo: %s
                Idioma: %s

                CONTEXTO:
                %s

                Pregunta:
                %s
                """.formatted(
                ctx.mode(),
                lang,
                ctx.context(),
                question
        );

        String ans = llmClient.getAnswer(prompt);

        // Log simple para ver qué está pasando en local
        System.out.println("BOT ANSWER | mode=" + ctx.mode() + " | listingId=" + listingId);

        return new AnswerResult(ctx.mode(), ans);
    }

    /**
     * Resuelve el contexto con fallback seguro a DEMO.
     * Incluye logs explícitos para que no quede “silencioso”.
     */
    private ContextResult resolveContextWithFallback(Long listingId) {

        boolean hasCreds = hostawayProperties.accountId() != null && !hostawayProperties.accountId().isBlank()
                && hostawayProperties.clientSecret() != null && !hostawayProperties.clientSecret().isBlank();

        // Si Hostaway no está activo o no hay provider o faltan credenciales → DEMO directo
        if (!hostawayProperties.enabled() || hostawayProvider == null || !hasCreds) {
            String reason =
                    !hostawayProperties.enabled() ? "hostaway.enabled=false" :
                            (hostawayProvider == null ? "hostawayProvider=null (bean no creado)" :
                                    "faltan credenciales (accountId/clientSecret)");

            System.out.println("CTX MODE = DEMO | listingId=" + listingId + " | reason=" + reason);
            return new ContextResult(demoProvider.getMode(), demoProvider.getContext(listingId));
        }

        // Hostaway activo: intento real con fallback a demo si falla
        try {
            String ctx = hostawayProvider.getContext(listingId);

            if (ctx == null || ctx.isBlank()) {
                System.err.println("CTX EMPTY -> fallback DEMO | listingId=" + listingId);
                return new ContextResult(demoProvider.getMode(), demoProvider.getContext(listingId));
            }

            System.out.println("CTX MODE = HOSTAWAY | listingId=" + listingId);
            return new ContextResult(hostawayProvider.getMode(), ctx);

        } catch (Exception ex) {
            // Importantísimo: no romper por API, pero sí dejar rastro claro en logs
            String msg = ex.getMessage();
            if (msg == null) msg = ex.getClass().getSimpleName();

            System.err.println("CTX MODE = DEMO (fallback HOSTAWAY ERROR) | listingId=" + listingId + " | err=" + msg);

            return new ContextResult("DEMO (fallback HOSTAWAY ERROR)", demoProvider.getMode().equals("DEMO")
                    ? demoProvider.getContext(listingId)
                    : demoProvider.getContext(listingId));
        }
    }

    private record ContextResult(String mode, String context) {}

    public record AnswerResult(String mode, String answer) {}
}
