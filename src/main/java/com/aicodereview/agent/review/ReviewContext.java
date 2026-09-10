package com.aicodereview.agent.review;

import java.nio.file.Path;

public record ReviewContext(
        String reviewId,
        Path repositoryRoot) {
}