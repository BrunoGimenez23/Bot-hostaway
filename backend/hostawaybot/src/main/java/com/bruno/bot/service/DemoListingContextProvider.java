package com.bruno.bot.service;

import com.bruno.bot.config.DemoProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class DemoListingContextProvider implements ListingContextProvider {

    private final DemoProperties demoProperties;

    public DemoListingContextProvider(DemoProperties demoProperties) {
        this.demoProperties = demoProperties;
    }

    @Override
    public String getContext(Long listingId) {
        return """
               (Contexto simulado)
               listingId: %s
               %s
               """.formatted(listingId, demoProperties.fallbackContext()).trim();
    }

    @Override
    public String getMode() {
        return "DEMO";
    }
}
