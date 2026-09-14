package com.aicodereview.agent.activity;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.aicodereview.agent.streaming.ReviewActivitySubscriber;

@Component
public class DefaultReviewActivityPublisher
        implements ReviewActivityPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    DefaultReviewActivityPublisher.class);

    private final ReviewActivityStore reviewActivityStore;
    private final List<ReviewActivitySubscriber> subscribers;

    public DefaultReviewActivityPublisher(
            ReviewActivityStore reviewActivityStore,
            List<ReviewActivitySubscriber> subscribers) {

        this.reviewActivityStore = reviewActivityStore;
        this.subscribers = subscribers;
    }

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

        reviewActivityStore.add(event);

        log.info(
                "REVIEW ACTIVITY | reviewId={} | type={} | message={}",
                event.reviewId(),
                event.type(),
                event.message());

        notifySubscribers(event);
    }

    private void notifySubscribers(
            ReviewActivityEvent event) {

        for (ReviewActivitySubscriber subscriber : subscribers) {

            try {

                subscriber.onActivity(event);

            } catch (Exception e) {

                log.warn(
                        "Failed to notify activity subscriber | reviewId={} | subscriber={}",
                        event.reviewId(),
                        subscriber.getClass().getSimpleName(),
                        e);
            }
        }
    }
}