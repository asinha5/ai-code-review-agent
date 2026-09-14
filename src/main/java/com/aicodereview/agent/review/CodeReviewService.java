package com.aicodereview.agent.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.aicodereview.agent.activity.ReviewActivityPublisher;
import com.aicodereview.agent.activity.ReviewActivityType;

@Service
public class CodeReviewService {

    private static final Logger log =
            LoggerFactory.getLogger(CodeReviewService.class);

    private final ReviewContextManager reviewContextManager;
    private final ReviewExecutionStore reviewExecutionStore;
    private final AsyncReviewExecutor asyncReviewExecutor;
    private final ReviewActivityPublisher reviewActivityPublisher;

    public CodeReviewService(
            ReviewContextManager reviewContextManager,
            ReviewExecutionStore reviewExecutionStore,
            AsyncReviewExecutor asyncReviewExecutor,
            ReviewActivityPublisher reviewActivityPublisher) {

        this.reviewContextManager = reviewContextManager;
        this.reviewExecutionStore = reviewExecutionStore;
        this.asyncReviewExecutor = asyncReviewExecutor;
        this.reviewActivityPublisher = reviewActivityPublisher;
    }

    /**
     * Starts a new asynchronous code review.
     *
     * This method:
     * 1. Validates the repository path.
     * 2. Creates a ReviewContext.
     * 3. Stores the review as PENDING.
     * 4. Publishes REVIEW_STARTED.
     * 5. Delegates the actual review execution to AsyncReviewExecutor.
     *
     * The method returns immediately with the reviewId.
     */
    public String startReview(
            String repositoryPath) {

        validateRepositoryPath(repositoryPath);

        ReviewContext reviewContext =
                reviewContextManager.create(repositoryPath);

        String reviewId =
                reviewContext.reviewId();

        ReviewExecution execution =
                new ReviewExecution(
                        reviewId,
                        ReviewStatus.PENDING,
                        null,
                        null);

        reviewExecutionStore.save(execution);

        log.info(
                "Code review created | reviewId={} | repository={}",
                reviewId,
                reviewContext.repositoryRoot());

        reviewActivityPublisher.publish(
                reviewId,
                ReviewActivityType.REVIEW_STARTED,
                "Code review started");

        asyncReviewExecutor.execute(
                reviewContext);

        return reviewId;
    }

    /**
     * Returns the current execution state for a review.
     */
    public ReviewExecution getReview(
            String reviewId) {

        return reviewExecutionStore
                .get(reviewId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Review not found: "
                                        + reviewId));
    }

    private void validateRepositoryPath(
            String repositoryPath) {

        if (repositoryPath == null
                || repositoryPath.isBlank()) {

            throw new IllegalArgumentException(
                    "Repository path must not be empty");
        }
    }
}