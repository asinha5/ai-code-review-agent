package com.aicodereview.agent.observability;

import org.springframework.stereotype.Component;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@Component
public class ReviewTracingService {

    private final Tracer tracer;

    public ReviewTracingService(
            Tracer tracer) {

        this.tracer = tracer;
    }

    public Span startReviewSpan(
            String reviewId,
            String repositoryName) {

        Span span =
                tracer
                        .nextSpan()
                        .name("ai-code-review")
                        .tag(
                                "review.id",
                                reviewId)
                        .tag(
                                "review.repository",
                                repositoryName);

        span.start();

        return span;
    }
}