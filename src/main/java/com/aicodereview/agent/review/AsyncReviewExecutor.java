package com.aicodereview.agent.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.aicodereview.agent.activity.ReviewActivityPublisher;
import com.aicodereview.agent.activity.ReviewActivityType;
import com.aicodereview.agent.tool.RepositoryTools;

@Service
public class AsyncReviewExecutor {

    private static final Logger log =
            LoggerFactory.getLogger(AsyncReviewExecutor.class);

    private final ChatClient chatClient;
    private final RepositoryTools repositoryTools;
    private final ReviewExecutionStore reviewExecutionStore;
    private final ReviewActivityPublisher reviewActivityPublisher;

    public AsyncReviewExecutor(
            ChatClient.Builder chatClientBuilder,
            RepositoryTools repositoryTools,
            ReviewExecutionStore reviewExecutionStore,
            ReviewActivityPublisher reviewActivityPublisher) {

        this.chatClient = chatClientBuilder.build();
        this.repositoryTools = repositoryTools;
        this.reviewExecutionStore = reviewExecutionStore;
        this.reviewActivityPublisher = reviewActivityPublisher;
    }

    @Async
    public void execute(
            ReviewContext reviewContext) {

        String reviewId =
                reviewContext.reviewId();

        try {

            reviewExecutionStore.save(
                    new ReviewExecution(
                            reviewId,
                            ReviewStatus.RUNNING,
                            null,
                            null));

            reviewActivityPublisher.publish(
                    reviewId,
                    ReviewActivityType.ANALYZING,
                    "Analyzing repository");

            BeanOutputConverter<CodeReviewResponse> converter =
                    new BeanOutputConverter<>(
                            CodeReviewResponse.class);

            String prompt =
                    buildReviewPrompt(
                            reviewId,
                            converter.getFormat());

            ChatResponse chatResponse =
                    chatClient
                            .prompt()
                            .user(prompt)
                            .tools(repositoryTools)
                            .call()
                            .chatResponse();

            if (chatResponse == null) {
                throw new IllegalStateException(
                        "AI returned no ChatResponse");
            }

            String rawResponse = null;

            if (chatResponse.getResult() != null
                    && chatResponse.getResult().getOutput() != null) {

                rawResponse =
                        chatResponse
                                .getResult()
                                .getOutput()
                                .getText();
            }

            reviewActivityPublisher.publish(
                    reviewId,
                    ReviewActivityType.GENERATING_FINDINGS,
                    "Processing review findings");

            String jsonResponse =
                    extractJson(rawResponse);

            CodeReviewResponse response =
                    converter.convert(jsonResponse);

            if (response == null) {
                throw new IllegalStateException(
                        "Unable to convert AI response into CodeReviewResponse");
            }

            reviewExecutionStore.save(
                    new ReviewExecution(
                            reviewId,
                            ReviewStatus.COMPLETED,
                            response,
                            null));

            reviewActivityPublisher.publish(
                    reviewId,
                    ReviewActivityType.REVIEW_COMPLETED,
                    "Code review completed");

            log.info(
                    "Async code review completed | reviewId={} | findings={}",
                    reviewId,
                    response.findings() != null
                            ? response.findings().size()
                            : 0);

        } catch (Exception e) {

            reviewExecutionStore.save(
                    new ReviewExecution(
                            reviewId,
                            ReviewStatus.FAILED,
                            null,
                            e.getMessage()));

            reviewActivityPublisher.publish(
                    reviewId,
                    ReviewActivityType.REVIEW_FAILED,
                    "Code review failed");

            log.error(
                    "Async code review failed | reviewId={}",
                    reviewId,
                    e);
        }
    }

    private String buildReviewPrompt(
            String reviewId,
            String outputFormat) {

        return """
                You are an autonomous senior Java and Spring Boot code reviewer.

                Your goal is to inspect the repository using the available tools,
                identify important code-quality or runtime risks, and return a
                concise, evidence-based code review.

                REVIEW ID:
                %s

                IMPORTANT:
                Every repository tool call must use exactly this reviewId:
                %s

                Do not invent, modify, shorten, or regenerate the reviewId.

                ============================================================
                REPOSITORY TOOLS
                ============================================================

                You have repository tools available for:

                - getting the repository tree
                - listing files within a specific directory
                - reading repository files
                - searching source code

                Start by using getRepositoryTree to understand the overall
                repository structure.

                Do not repeatedly call listFiles to walk the repository one
                directory at a time unless additional directory inspection is
                genuinely necessary.

                After viewing the repository tree, select only the files that are
                most relevant to the review.

                Use searchCode when you need to locate a specific class,
                annotation, API usage, configuration value, or code pattern.

                Avoid reading every file in the repository.

                Use the minimum number of tool calls necessary to gather
                sufficient evidence.

                ============================================================
                REVIEW AREAS
                ============================================================

                Review the repository for:

                1. Bugs and correctness issues
                2. Potential runtime failures
                3. Java design problems
                4. Spring Boot design problems
                5. Error-handling problems
                6. Security risks
                7. Resource-management issues
                8. Maintainability problems
                9. Important code-quality issues

                ============================================================
                EVIDENCE RULES
                ============================================================

                Do not invent issues.

                Every finding must be supported by evidence discovered using
                repository tools.

                Only include findings with HIGH or MEDIUM confidence.

                Prefer fewer high-confidence findings over speculative findings.

                ============================================================
                OUTPUT
                ============================================================

                Return at most 3 findings.

                Keep the summary concise.

                Return only JSON matching the required output structure.

                Do not include markdown or commentary outside the JSON.

                Required output format:

                %s
                """.formatted(
                reviewId,
                reviewId,
                outputFormat);
    }

    private String extractJson(
            String response) {

        if (response == null
                || response.isBlank()) {

            throw new IllegalArgumentException(
                    "AI returned an empty response");
        }

        int start =
                response.indexOf('{');

        int end =
                response.lastIndexOf('}');

        if (start == -1
                || end == -1
                || end <= start) {

            throw new IllegalArgumentException(
                    "No valid JSON object found in AI response");
        }

        return response.substring(
                start,
                end + 1);
    }
}