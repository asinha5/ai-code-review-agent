package com.aicodereview.agent.streaming;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aicodereview.agent.activity.ReviewActivityEvent;
import com.aicodereview.agent.activity.ReviewActivityStore;
import com.aicodereview.agent.activity.ReviewActivityType;

@Component
public class SseReviewActivitySubscriber
        implements ReviewActivitySubscriber {

    private static final Logger log =
            LoggerFactory.getLogger(
                    SseReviewActivitySubscriber.class);

    private static final long SSE_TIMEOUT =
            30 * 60 * 1000L;

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    private final ReviewActivityStore reviewActivityStore;

    public SseReviewActivitySubscriber(
            ReviewActivityStore reviewActivityStore) {

        this.reviewActivityStore = reviewActivityStore;
    }

    public SseEmitter subscribe(
            String reviewId) {

        SseEmitter emitter =
                new SseEmitter(SSE_TIMEOUT);

        emitters
                .computeIfAbsent(
                        reviewId,
                        key -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() ->
                removeEmitter(
                        reviewId,
                        emitter));

        emitter.onTimeout(() -> {

            log.debug(
                    "SSE connection timed out | reviewId={}",
                    reviewId);

            removeEmitter(
                    reviewId,
                    emitter);
        });

        emitter.onError(error -> {

            log.debug(
                    "SSE connection error | reviewId={}",
                    reviewId,
                    error);

            removeEmitter(
                    reviewId,
                    emitter);
        });

        log.info(
                "SSE subscriber connected | reviewId={} | subscribers={}",
                reviewId,
                getSubscriberCount(reviewId));

        replayExistingActivities(
                reviewId,
                emitter);

        return emitter;
    }

    @Override
    public void onActivity(
            ReviewActivityEvent event) {

        List<SseEmitter> reviewEmitters =
                emitters.get(event.reviewId());

        if (reviewEmitters == null
                || reviewEmitters.isEmpty()) {

            return;
        }

        for (SseEmitter emitter : reviewEmitters) {

            sendEvent(
                    event,
                    emitter);
        }

        if (event.type()
                == ReviewActivityType.REVIEW_COMPLETED
                || event.type()
                == ReviewActivityType.REVIEW_FAILED) {

            completeReviewEmitters(
                    event.reviewId());
        }
    }

    private void sendEvent(
            ReviewActivityEvent event,
            SseEmitter emitter) {

        try {

            emitter.send(
                    SseEmitter
                            .event()
                            .name("review-activity")
                            .id(event.timestamp().toString())
                            .data(event));

        } catch (IOException e) {

            log.debug(
                    "Failed to send SSE event | reviewId={}",
                    event.reviewId(),
                    e);

            emitter.completeWithError(e);

            removeEmitter(
                    event.reviewId(),
                    emitter);
        }
    }

    private void replayExistingActivities(
            String reviewId,
            SseEmitter emitter) {

        for (ReviewActivityEvent event :
                reviewActivityStore.getActivities(reviewId)) {

            try {

                emitter.send(
                        SseEmitter
                                .event()
                                .name("review-activity")
                                .id(event.timestamp().toString())
                                .data(event));

            } catch (IOException e) {

                log.debug(
                        "Failed to replay SSE activity | reviewId={}",
                        reviewId,
                        e);

                emitter.completeWithError(e);

                removeEmitter(
                        reviewId,
                        emitter);

                return;
            }
        }
    }

    private void completeReviewEmitters(
            String reviewId) {

        List<SseEmitter> reviewEmitters =
                emitters.remove(reviewId);

        if (reviewEmitters == null) {
            return;
        }

        for (SseEmitter emitter : reviewEmitters) {

            try {
                emitter.complete();
            } catch (Exception e) {

                log.debug(
                        "Failed to complete SSE emitter | reviewId={}",
                        reviewId,
                        e);
            }
        }

        log.info(
                "All SSE subscribers completed | reviewId={}",
                reviewId);
    }

    private void removeEmitter(
            String reviewId,
            SseEmitter emitter) {

        CopyOnWriteArrayList<SseEmitter> reviewEmitters =
                emitters.get(reviewId);

        if (reviewEmitters == null) {
            return;
        }

        reviewEmitters.remove(emitter);

        if (reviewEmitters.isEmpty()) {
            emitters.remove(
                    reviewId,
                    reviewEmitters);
        }

        log.debug(
                "SSE subscriber removed | reviewId={} | remainingSubscribers={}",
                reviewId,
                getSubscriberCount(reviewId));
    }

    private int getSubscriberCount(
            String reviewId) {

        List<SseEmitter> reviewEmitters =
                emitters.get(reviewId);

        return reviewEmitters == null
                ? 0
                : reviewEmitters.size();
    }
}