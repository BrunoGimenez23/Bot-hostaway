package com.bruno.bot.service;

import com.bruno.bot.client.LlmClient;
import com.bruno.bot.config.HostawayProperties;
import com.bruno.bot.dto.BotQuestionRequest;
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
            org.springframework.beans.factory.ObjectProvider<HostawayListingContextProvider> hostawayProvider
    ) {
        this.llmClient = llmClient;
        this.hostawayProperties = hostawayProperties;
        this.demoProvider = demoProvider;
        this.hostawayProvider = hostawayProvider.getIfAvailable();
    }

    public String answerQuestion(BotQuestionRequest request) {

        String context;
        String mode;

        if (hostawayProperties.enabled() && hostawayProvider != null) {
            context = hostawayProvider.getContext(request.listingId());
            mode = "HOSTAWAY";
        } else {
            context = demoProvider.getContext(request.listingId());
            mode = "DEMO";
        }

        String prompt = """
                Eres un asistente para huéspedes.
                Responde corto, claro y en el idioma indicado.
                Si la información no está en el contexto, decí que lo vas a confirmar.

                Modo: %s
                Idioma: %s

                CONTEXTO:
                %s

                Pregunta:
                %s
                """.formatted(
                mode,
                request.language() == null ? "es" : request.language(),
                context,
                request.message()
        );

        return llmClient.getAnswer(prompt);
    }
}
