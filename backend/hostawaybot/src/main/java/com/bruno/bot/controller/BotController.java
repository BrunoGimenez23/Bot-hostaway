package com.bruno.bot.controller;

import com.bruno.bot.dto.BotAnswerResponse;
import com.bruno.bot.dto.BotQuestionRequest;
import com.bruno.bot.service.BotService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bot")
public class BotController {

    private final BotService botService;

    public BotController(BotService botService) {
        this.botService = botService;
    }

    @PostMapping("/answer")
    public ResponseEntity<?> answer(@RequestBody BotQuestionRequest req) {
        var result = botService.answerQuestionWithMeta(req); // te lo paso abajo
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Bot-Mode", result.mode());
        return ResponseEntity.ok().headers(headers).body(Map.of("answer", result.answer()));
    }

}
