package com.aicodereview.agent.review;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class InMemoryReviewExecutionStore
        implements ReviewExecutionStore {

    private final Map<String, ReviewExecution> executions =
            new ConcurrentHashMap<>();

    @Override
    public void save(
            ReviewExecution execution) {

        executions.put(
                execution.reviewId(),
                execution);
    }

    @Override
    public Optional<ReviewExecution> get(
            String reviewId) {

        return Optional.ofNullable(
                executions.get(reviewId));
    }
}