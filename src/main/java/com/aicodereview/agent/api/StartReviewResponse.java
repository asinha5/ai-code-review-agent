package com.aicodereview.agent.api;

public record StartReviewResponse(
        String reviewId,
        String status) {
}