package com.aicodereview.agent.review;

import java.util.List;

public record CodeReviewResponse(
        String summary,
        List<CodeReviewFinding> findings) {
}