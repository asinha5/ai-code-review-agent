package com.aicodereview.agent.review;

import java.time.Instant;

public record ReviewActivityEvent(
        String reviewId,
        ReviewActivityType type,
        String message,
        Instant timestamp) {
}