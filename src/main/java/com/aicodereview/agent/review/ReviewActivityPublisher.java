package com.aicodereview.agent.review;

public interface ReviewActivityPublisher {

    void publish(
            String reviewId,
            ReviewActivityType type,
            String message);
}