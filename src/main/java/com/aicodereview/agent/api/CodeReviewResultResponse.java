package com.aicodereview.agent.api;

import com.aicodereview.agent.review.CodeReviewFinding;

import java.util.List;

public record CodeReviewResultResponse(
        String reviewId,
        String summary,
        List<CodeReviewFinding> findings) {
}