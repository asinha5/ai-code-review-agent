package com.aicodereview.agent.review;

import java.util.Optional;

public interface ReviewExecutionStore {

    void save(ReviewExecution execution);

    Optional<ReviewExecution> get(String reviewId);
}