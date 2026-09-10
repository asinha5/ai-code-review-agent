package com.aicodereview.agent.review;

public record CodeReviewFinding(
        String severity,
        String category,
        String file,
        Integer line,
        String issue,
        String evidence,
        String recommendation,
        String confidence) {
}