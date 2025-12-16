package com.bruno.bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BotQuestionRequest(
        @NotNull Long listingId,
        @NotBlank String message,
        String language
) {}
