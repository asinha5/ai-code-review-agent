package com.aicodereview.agent.activity;

import java.time.Instant;

public record ReviewActivityEvent(
        String reviewId,
        ReviewActivityType type,
        String message,
        Instant timestamp) {
}