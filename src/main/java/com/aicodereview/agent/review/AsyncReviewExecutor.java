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
import com.aicodereview.agent.observability.ReviewTracingService;
import com.aicodereview.agent.tool.RepositoryTools;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

@Service
public class AsyncReviewExecutor {

    private static final Logger log =
            LoggerFactory.getLogger(AsyncReviewExecutor.class);

    private final ChatClient chatClient;
    private final RepositoryTools repositoryTools;
    private final ReviewExecutionStore reviewExecutionStore;
    private final ReviewActivityPublisher reviewActivityPublisher;
    private final ReviewTracingService reviewTracingService;
    private final Tracer tracer;

    public AsyncReviewExecutor(
            ChatClient.Builder chatClientBuilder,
            RepositoryTools repositoryTools,
            ReviewExecutionStore reviewExecutionStore,
            ReviewActivityPublisher reviewActivityPublisher,
            ReviewTracingService reviewTracingService,
            Tracer tracer) {

        this.chatClient = chatClientBuilder.build();
        this.repositoryTools = repositoryTools;
        this.reviewExecutionStore = reviewExecutionStore;
        this.reviewActivityPublisher = reviewActivityPublisher;
        this.reviewTracingService = reviewTracingService;
        this.tracer = tracer;
    }

    @Async
    public void execute(
            ReviewContext reviewContext) {

        String reviewId =
                reviewContext.reviewId();

        String repositoryName =
                reviewContext.repositoryRoot()
                        .getFileName()
                        .toString();

        Span span =
                reviewTracingService.startReviewSpan(
                        reviewId,
                        repositoryName);

        try (Tracer.SpanInScope ignored =
                     tracer.withSpan(span)) {

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

            log.info(
                    "AI response metadata | reviewId={} | metadata={}",
                    reviewId,
                    chatResponse.getMetadata());

            String rawResponse = null;

            if (chatResponse.getResult() != null
                    && chatResponse.getResult()
                            .getOutput() != null) {

                rawResponse =
                        chatResponse
                                .getResult()
                                .getOutput()
                                .getText();
            }

            log.info(
                    "AI response received | reviewId={} | length={}",
                    reviewId,
                    rawResponse != null
                            ? rawResponse.length()
                            : 0);

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

            int findingCount =
                    response.findings() == null
                            ? 0
                            : response.findings().size();

            reviewExecutionStore.save(
                    new ReviewExecution(
                            reviewId,
                            ReviewStatus.COMPLETED,
                            response,
                            null));

            span.tag(
                    "review.status",
                    "COMPLETED");

            span.tag(
                    "review.findings.count",
                    String.valueOf(findingCount));

            reviewActivityPublisher.publish(
                    reviewId,
                    ReviewActivityType.REVIEW_COMPLETED,
                    "Code review completed");

            log.info(
                    "Async code review completed | reviewId={} | findings={}",
                    reviewId,
                    findingCount);

        } catch (Exception e) {

            span.tag(
                    "review.status",
                    "FAILED");

            span.error(e);

            String userFriendlyMessage =
                    getUserFriendlyErrorMessage(e);

            reviewExecutionStore.save(
                    new ReviewExecution(
                            reviewId,
                            ReviewStatus.FAILED,
                            null,
                            userFriendlyMessage));

            reviewActivityPublisher.publish(
                    reviewId,
                    ReviewActivityType.REVIEW_FAILED,
                    "Code review failed");

            log.error(
                    "Async code review failed | reviewId={}",
                    reviewId,
                    e);

        } finally {

            span.end();
        }
    }

    private String buildReviewPrompt(
            String reviewId,
            String outputFormat) {

        return """
                You are an autonomous senior Java and Spring Boot code reviewer.

                Your goal is to inspect the repository using the available tools,
                identify important engineering risks, and return a concise,
                evidence-based code review.

                REVIEW ID:
                %s

                IMPORTANT:
                Every repository tool call must use exactly this reviewId:
                %s

                Do not invent, modify, shorten, or regenerate the reviewId.

                ============================================================
                REPOSITORY TOOLS
                ============================================================

                Available tools:

                - getRepositoryTree
                - listFiles
                - readFile
                - searchCode

                Start with getRepositoryTree to understand the repository.

                Avoid walking the repository directory-by-directory unless
                additional inspection is necessary.

                Select only relevant files.

                Use readFile for important implementation/configuration files.

                Use searchCode when you need to locate:
                - classes
                - methods
                - annotations
                - configuration values
                - exception handling
                - specific APIs

                Avoid reading every file.

                Use the minimum number of tool calls necessary.

                ============================================================
                REVIEW AREAS
                ============================================================

                Review for:

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

                Every finding must be supported by repository evidence.

                If evidence is insufficient, do not report the issue.

                Only include HIGH or MEDIUM confidence findings.

                Prefer fewer strong findings over speculative findings.

                ============================================================
                OUTPUT LIMITS
                ============================================================

                Return at most 2 findings.

                Keep the summary under 60 words.

                For each finding:

                - issue: maximum 30 words
                - evidence: maximum 50 words
                - recommendation: maximum 40 words

                Keep output concise.

                ============================================================
                REQUIRED FIELDS
                ============================================================

                Each finding must contain:

                - severity
                - category
                - file
                - line
                - issue
                - evidence
                - recommendation
                - confidence

                Severity must be one of:

                CRITICAL
                HIGH
                MEDIUM
                LOW
                INFO

                Confidence must be one of:

                HIGH
                MEDIUM
                LOW

                ============================================================
                OUTPUT FORMAT
                ============================================================

                Return only JSON.

                Do not include markdown.

                Do not wrap JSON in code fences.

                Do not include commentary before or after JSON.

                You MUST return a complete JSON object.

                Do not stop after generating a finding.

                Ensure the final response contains all required closing
                brackets and braces.

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

    private String getUserFriendlyErrorMessage(
            Exception e) {

        String message =
                e.getMessage();

        if (message == null
                || message.isBlank()) {

            return "Code review failed due to an unexpected error.";
        }

        String normalized =
                message.toLowerCase();

        if (message.contains("429")
                || normalized.contains("rate limit")) {

            return "AI provider rate limit exceeded. Please wait a moment and try again.";
        }

        if (normalized.contains("timeout")
                || normalized.contains("timed out")) {

            return "AI provider request timed out. Please try again.";
        }

        if (normalized.contains("api key")
                || normalized.contains("authentication")
                || normalized.contains("unauthorized")) {

            return "AI provider authentication failed. Please check the configured API credentials.";
        }

        if (normalized.contains("json")
                || normalized.contains("unexpected end-of-input")
                || normalized.contains("parse")) {

            return "AI returned an incomplete or invalid structured response. Please try again.";
        }

        return "Code review failed. Please check the application logs for details.";
    }
}