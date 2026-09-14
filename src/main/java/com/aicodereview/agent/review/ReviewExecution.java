package com.aicodereview.agent.review;

public record ReviewExecution(
        String reviewId,
        ReviewStatus status,
        CodeReviewResponse result,
        String errorMessage) {
}