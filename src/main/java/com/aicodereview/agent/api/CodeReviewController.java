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
import com.aicodereview.agent.review.CodeReviewResult;
import com.aicodereview.agent.review.CodeReviewService;
import com.aicodereview.agent.streaming.SseReviewActivitySubscriber;

import com.aicodereview.agent.activity.ReviewActivityPublisher;
import com.aicodereview.agent.activity.ReviewActivityType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/reviews")
@Tag(
        name = "Code Reviews",
        description = "APIs for starting code reviews and monitoring review activity")
public class CodeReviewController {

    private final CodeReviewService codeReviewService;
    private final ReviewActivityStore reviewActivityStore;
    private final SseReviewActivitySubscriber sseReviewActivitySubscriber;
    private final ReviewActivityPublisher reviewActivityPublisher;

    public CodeReviewController(
            CodeReviewService codeReviewService,
            ReviewActivityStore reviewActivityStore,
            SseReviewActivitySubscriber sseReviewActivitySubscriber,
        ReviewActivityPublisher reviewActivityPublisher) {

        this.codeReviewService = codeReviewService;
        this.reviewActivityStore = reviewActivityStore;
        this.sseReviewActivitySubscriber = sseReviewActivitySubscriber;
        this.reviewActivityPublisher = reviewActivityPublisher;
    }

    @Operation(
            summary = "Start a code review",
            description = """
                    Starts an AI-assisted code review for the supplied
                    local repository path.

                    The response contains the generated reviewId,
                    review summary, and structured findings.
                    """)
    @PostMapping
    public ResponseEntity<CodeReviewResultResponse> review(
            @RequestBody CodeReviewRequest request) {

        CodeReviewResult result =
                codeReviewService.review(
                        request.repositoryPath());

        CodeReviewResultResponse response =
                new CodeReviewResultResponse(
                        result.reviewId(),
                        result.review().summary(),
                        result.review().findings());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get review activities",
            description = """
                    Returns the activity events currently stored
                    for the supplied reviewId.
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
                    Opens a Server-Sent Events connection and streams
                    live activity events for the supplied reviewId.

                    The connection completes automatically when the review
                    finishes or fails.
                    """)
    @GetMapping(
            value = "/{reviewId}/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamReviewActivity(
            @PathVariable String reviewId) {

        return sseReviewActivitySubscriber.subscribe(reviewId);
    }

    @PostMapping("/{reviewId}/activities/test")
    @Operation(
            summary = "Publish test activity",
            description = "Temporary endpoint used to test SSE without invoking the AI model.")
    public ResponseEntity<Void> publishTestActivity(
            @PathVariable String reviewId) {
    
        reviewActivityPublisher.publish(
                reviewId,
                ReviewActivityType.ANALYZING,
                "Testing live SSE activity");
    
        return ResponseEntity.ok().build();
    }

}