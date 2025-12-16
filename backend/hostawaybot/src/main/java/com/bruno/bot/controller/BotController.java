package com.bruno.bot.controller;

import com.bruno.bot.dto.BotAnswerResponse;
import com.bruno.bot.dto.BotQuestionRequest;
import com.bruno.bot.service.BotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bot")
public class BotController {

    private final BotService botService;

    public BotController(BotService botService) {
        this.botService = botService;
    }

    @PostMapping("/answer")
    public ResponseEntity<BotAnswerResponse> answer(@jakarta.validation.Valid @RequestBody BotQuestionRequest request) {
        String answer = botService.answerQuestion(request);
        return ResponseEntity.ok(new BotAnswerResponse(answer));
    }

}
