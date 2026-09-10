package com.aicodereview.agent.review;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class ReviewContextManager {

    private final Map<String, ReviewContext> reviews =
            new ConcurrentHashMap<>();

    public ReviewContext create(String repositoryPath) {

        Path repositoryRoot = Path.of(repositoryPath)
                .toAbsolutePath()
                .normalize();

        if (!Files.exists(repositoryRoot)) {
            throw new IllegalArgumentException(
                    "Repository does not exist: " + repositoryPath);
        }

        if (!Files.isDirectory(repositoryRoot)) {
            throw new IllegalArgumentException(
                    "Repository path is not a directory: " + repositoryPath);
        }

        String reviewId = UUID.randomUUID().toString();

        ReviewContext context =
                new ReviewContext(reviewId, repositoryRoot);

        reviews.put(reviewId, context);

        return context;
    }

    public ReviewContext get(String reviewId) {

        ReviewContext context = reviews.get(reviewId);

        if (context == null) {
            throw new IllegalArgumentException(
                    "Unknown review ID: " + reviewId);
        }

        return context;
    }
}