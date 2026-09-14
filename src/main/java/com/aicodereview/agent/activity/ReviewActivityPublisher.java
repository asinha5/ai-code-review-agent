package com.aicodereview.agent.activity;

public interface ReviewActivityPublisher {

    void publish(
            String reviewId,
            ReviewActivityType type,
            String message);
}