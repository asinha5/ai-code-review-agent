package com.aicodereview.agent.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aicodereview.agent.review.CodeReviewResult;
import com.aicodereview.agent.review.CodeReviewService;
import com.aicodereview.agent.review.ReviewActivityEvent;
import com.aicodereview.agent.review.ReviewActivityPublisher;
import com.aicodereview.agent.review.ReviewActivityStore;
import com.aicodereview.agent.review.ReviewActivityType;

@RestController
@RequestMapping("/api/reviews")
public class CodeReviewController {

    private final CodeReviewService codeReviewService;
    private final ReviewActivityStore reviewActivityStore;
    private final ReviewActivityPublisher reviewActivityPublisher;

    public CodeReviewController(
            CodeReviewService codeReviewService,
            ReviewActivityStore reviewActivityStore,
            ReviewActivityPublisher reviewActivityPublisher) {

        this.codeReviewService = codeReviewService;
        this.reviewActivityStore = reviewActivityStore;
        this.reviewActivityPublisher = reviewActivityPublisher;
    }

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

    @GetMapping("/{reviewId}/activities")
    public ResponseEntity<List<ReviewActivityEvent>> getActivities(
            @PathVariable String reviewId) {

        return ResponseEntity.ok(
                reviewActivityStore.getActivities(reviewId));
    }

}