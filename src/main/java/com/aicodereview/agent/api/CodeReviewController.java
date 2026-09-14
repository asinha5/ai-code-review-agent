package com.aicodereview.agent.api;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aicodereview.agent.activity.ReviewActivityEvent;
import com.aicodereview.agent.activity.ReviewActivityStore;
import com.aicodereview.agent.review.CodeReviewService;
import com.aicodereview.agent.review.ReviewExecution;
import com.aicodereview.agent.streaming.SseReviewActivitySubscriber;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/reviews")
@Tag(
        name = "Code Reviews",
        description = "APIs for starting code reviews and monitoring review execution")
public class CodeReviewController {

    private final CodeReviewService codeReviewService;
    private final ReviewActivityStore reviewActivityStore;
    private final SseReviewActivitySubscriber sseReviewActivitySubscriber;

    public CodeReviewController(
            CodeReviewService codeReviewService,
            ReviewActivityStore reviewActivityStore,
            SseReviewActivitySubscriber sseReviewActivitySubscriber) {

        this.codeReviewService = codeReviewService;
        this.reviewActivityStore = reviewActivityStore;
        this.sseReviewActivitySubscriber = sseReviewActivitySubscriber;
    }

    @Operation(
            summary = "Start a code review",
            description = """
                    Starts an asynchronous AI-assisted code review.

                    A reviewId is created immediately and returned with
                    HTTP 202 Accepted.

                    The review continues in the background.

                    Use the reviewId to:
                    - stream live activity
                    - retrieve stored activities
                    - retrieve the final review result
                    """)
    @PostMapping
    public ResponseEntity<StartReviewResponse> startReview(
            @RequestBody CodeReviewRequest request) {

        String reviewId =
                codeReviewService.startReview(
                        request.repositoryPath());

        StartReviewResponse response =
                new StartReviewResponse(
                        reviewId,
                        "PENDING");

        return ResponseEntity
                .accepted()
                .body(response);
    }

    @Operation(
            summary = "Get review result",
            description = """
                    Returns the current execution status and final result
                    for the supplied reviewId.

                    Possible statuses:

                    PENDING
                    RUNNING
                    COMPLETED
                    FAILED

                    The result is populated only when the review reaches
                    COMPLETED status.
                    """)
    @GetMapping("/{reviewId}/result")
    public ResponseEntity<ReviewExecution> getReviewResult(
            @PathVariable String reviewId) {

        ReviewExecution execution =
                codeReviewService.getReview(reviewId);

        return ResponseEntity.ok(execution);
    }

    @Operation(
            summary = "Get review activities",
            description = """
                    Returns all review activity events currently stored
                    for the supplied reviewId.

                    Activities may include repository inspection,
                    file reads, code searches, analysis, completion,
                    or failure events.
                    """)
    @GetMapping("/{reviewId}/activities")
    public ResponseEntity<List<ReviewActivityEvent>> getActivities(
            @PathVariable String reviewId) {

        return ResponseEntity.ok(
                reviewActivityStore.getActivities(reviewId));
    }

    @Operation(
            summary = "Stream review activity",
            description = """
                    Opens a Server-Sent Events connection for the supplied
                    reviewId.

                    Previously stored review activities are replayed first.

                    New review activities are then streamed live.

                    Multiple subscribers may connect to the same reviewId.

                    The SSE connection completes automatically when the
                    review reaches REVIEW_COMPLETED or REVIEW_FAILED.
                    """)
    @GetMapping(
            value = "/{reviewId}/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamReviewActivity(
            @PathVariable String reviewId) {

        return sseReviewActivitySubscriber
                .subscribe(reviewId);
    }
}