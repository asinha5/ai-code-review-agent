package com.aicodereview.agent.review;

import com.aicodereview.agent.tool.RepositoryTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

@Service
public class CodeReviewService {

    private static final Logger log =
            LoggerFactory.getLogger(CodeReviewService.class);

    private final ChatClient chatClient;
    private final RepositoryTools repositoryTools;
    private final ReviewContextManager reviewContextManager;
    private final ReviewActivityPublisher reviewActivityPublisher;


    public CodeReviewService(
        ChatClient.Builder chatClientBuilder,
        RepositoryTools repositoryTools,
        ReviewContextManager reviewContextManager,
        ReviewActivityPublisher reviewActivityPublisher) {

    this.chatClient = chatClientBuilder.build();
    this.repositoryTools = repositoryTools;
    this.reviewContextManager = reviewContextManager;
    this.reviewActivityPublisher = reviewActivityPublisher;
}

public CodeReviewResult  review(String repositoryPath) {

        validateRepositoryPath(repositoryPath);
    
        ReviewContext reviewContext =
                reviewContextManager.create(repositoryPath);
    
        String reviewId =
                reviewContext.reviewId();
    
        try {
    
            log.info(
                    "Starting code review | reviewId={} | repository={}",
                    reviewId,
                    reviewContext.repositoryRoot());
    
            reviewActivityPublisher.publish(
                    reviewId,
                    ReviewActivityType.REVIEW_STARTED,
                    "Code review started");
    
            BeanOutputConverter<CodeReviewResponse> converter =
                    new BeanOutputConverter<>(
                            CodeReviewResponse.class);
    
            String prompt =
                    buildReviewPrompt(
                            reviewId,
                            converter.getFormat());
    
            reviewActivityPublisher.publish(
                    reviewId,
                    ReviewActivityType.ANALYZING,
                    "Analyzing repository");
    
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
                    && chatResponse.getResult()
                            .getOutput() != null) {
    
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
    
            reviewActivityPublisher.publish(
                    reviewId,
                    ReviewActivityType.REVIEW_COMPLETED,
                    "Code review completed");
    
            log.info(
                    "Structured review generated | reviewId={} | findings={}",
                    reviewId,
                    response.findings() != null
                            ? response.findings().size()
                            : 0);
    
                            return new CodeReviewResult(
                                reviewId,
                                response);
    
        } catch (Exception e) {
    
            reviewActivityPublisher.publish(
                    reviewId,
                    ReviewActivityType.REVIEW_FAILED,
                    "Code review failed");
    
            log.error(
                    "Code review failed | reviewId={}",
                    reviewId,
                    e);
    
            throw e;
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

            Prefer reading important Java and configuration files such as:

            - Controllers
            - Services
            - Components
            - Repository or persistence classes
            - Configuration classes
            - Security-related classes
            - Tool implementations
            - Exception handling
            - application.yml or application.properties
            - pom.xml when relevant

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

            Focus on meaningful engineering problems.

            Do not report trivial formatting or stylistic preferences unless
            they create a genuine maintainability or correctness problem.

            ============================================================
            EVIDENCE RULES
            ============================================================

            Do not invent issues.

            Every finding must be supported by evidence discovered using
            repository tools.

            Do not assume code exists if you have not inspected it.

            If there is insufficient evidence for a suspected issue,
            do not include it as a finding.

            Prefer fewer high-confidence findings over many speculative
            findings.

            Only include findings with HIGH or MEDIUM confidence.

            ============================================================
            SEVERITY
            ============================================================

            Severity must be exactly one of:

            CRITICAL
            HIGH
            MEDIUM
            LOW
            INFO

            ============================================================
            CONFIDENCE
            ============================================================

            Confidence must be exactly one of:

            HIGH
            MEDIUM
            LOW

            ============================================================
            FINDING FORMAT
            ============================================================

            For every finding provide:

            - severity
            - category
            - file
            - line, when known
            - issue
            - evidence
            - recommendation
            - confidence

            Return at most 3 findings.

            Keep each finding concise.

            Keep the overall summary concise.

            ============================================================
            STOP CONDITION
            ============================================================

            Once you have inspected enough relevant code to produce a
            meaningful review, stop using tools.

            Do not continue exploring the repository unnecessarily.

            Return the final review.

            ============================================================
            OUTPUT
            ============================================================

            Return only JSON matching the required output structure.

            Do not include markdown.

            Do not wrap the JSON in a code block.

            Do not include commentary before or after the JSON.

            Required output format:

            %s
            """.formatted(
            reviewId,
            reviewId,
            outputFormat);
}

    private void validateRepositoryPath(
            String repositoryPath) {

        if (repositoryPath == null
                || repositoryPath.isBlank()) {

            throw new IllegalArgumentException(
                    "Repository path must not be empty");
        }
    }

    private String extractJson(
            String response) {

        if (response == null
                || response.isBlank()) {

            throw new IllegalArgumentException(
                    "AI returned an empty response");
        }

        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');

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