//package com.bruno.bot.client;
//
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.stereotype.Component;
//
//@Component
//@ConditionalOnProperty(prefix = "llm", name = "enabled", havingValue = "true")
//public class OpenAiLlmClient implements LlmClient {
//
//    private final OpenAiService service;
//
//    public OpenAiLlmClient(OpenAiService service) {
//        this.service = service;
//    }
//
//    @Override
//    public String getAnswer(String prompt) {
//        return service.ask(prompt);
//    }
//}
