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

    public String answerQuestion(BotQuestionRequest request) {
        String lang = (request.language() == null || request.language().isBlank()) ? "es" : request.language().trim();

        ContextResult ctx = resolveContextWithFallback(request.listingId());

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
                request.message() == null ? "" : request.message().trim()
        );

        return llmClient.getAnswer(prompt);
    }

    private ContextResult resolveContextWithFallback(Long listingId) {
        // Si Hostaway no está activo o no hay provider, demo directo
        if (!hostawayProperties.enabled()
                || hostawayProvider == null
                || hostawayProperties.accountId() == null || hostawayProperties.accountId().isBlank()
                || hostawayProperties.clientSecret() == null || hostawayProperties.clientSecret().isBlank()) {
            return new ContextResult(demoProvider.getMode(), demoProvider.getContext(listingId));
        }


        // Hostaway activo: intento real con fallback a demo si falla
        try {
            String ctx = hostawayProvider.getContext(listingId);
            // Si vino vacío por algún motivo, también fallback
            if (ctx == null || ctx.isBlank()) {
                return new ContextResult(demoProvider.getMode(), demoProvider.getContext(listingId));
            }
            return new ContextResult(hostawayProvider.getMode(), ctx);
        } catch (Exception ex) {
            // Importante: no romper Fase 1/2 por un error de API
            return new ContextResult(demoProvider.getMode(), demoProvider.getContext(listingId));
        }
    }

    private record ContextResult(String mode, String context) {}
}
