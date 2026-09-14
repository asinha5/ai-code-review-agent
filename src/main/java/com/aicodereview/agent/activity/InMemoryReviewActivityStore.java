package com.aicodereview.agent.activity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

@Component
public class InMemoryReviewActivityStore
        implements ReviewActivityStore {

    private final Map<String, List<ReviewActivityEvent>> activities =
            new ConcurrentHashMap<>();

    @Override
    public void add(ReviewActivityEvent event) {

        activities
                .computeIfAbsent(
                        event.reviewId(),
                        key -> new CopyOnWriteArrayList<>())
                .add(event);
    }

    @Override
    public List<ReviewActivityEvent> getActivities(
            String reviewId) {

        return List.copyOf(
                activities.getOrDefault(
                        reviewId,
                        List.of()));
    }

    @Override
    public void clear(
            String reviewId) {

        activities.remove(reviewId);
    }
}