package com.aicodereview.agent.review;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultReviewActivityPublisher
        implements ReviewActivityPublisher, ReviewActivityStore {

    private static final Logger log =
            LoggerFactory.getLogger(
                    DefaultReviewActivityPublisher.class);

    private final Map<String, List<ReviewActivityEvent>> activities =
            new ConcurrentHashMap<>();

    @Override
    public void publish(
            String reviewId,
            ReviewActivityType type,
            String message) {

        ReviewActivityEvent event =
                new ReviewActivityEvent(
                        reviewId,
                        type,
                        message,
                        Instant.now());

        activities
                .computeIfAbsent(
                        reviewId,
                        key -> new CopyOnWriteArrayList<>())
                .add(event);

        log.info(
                "REVIEW ACTIVITY | reviewId={} | type={} | message={}",
                event.reviewId(),
                event.type(),
                event.message());
    }

    public List<ReviewActivityEvent> getActivities(
            String reviewId) {

        return List.copyOf(
                activities.getOrDefault(
                        reviewId,
                        List.of()));
    }

    public void clear(
            String reviewId) {

        activities.remove(reviewId);
    }
}