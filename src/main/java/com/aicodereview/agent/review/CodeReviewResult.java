package com.aicodereview.agent.review;

public record CodeReviewResult(
        String reviewId,
        CodeReviewResponse review) {
}