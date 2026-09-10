package com.aicodereview.agent.api;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aicodereview.agent.review.CodeReviewResponse;
import com.aicodereview.agent.review.ReviewContext;
import com.aicodereview.agent.review.ReviewContextManager;
import com.aicodereview.agent.tool.RepositoryTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/ai")
public class AiConnectionController {


        private static final Logger log =
        LoggerFactory.getLogger(AiConnectionController.class);

        private final ChatClient chatClient;
        private final RepositoryTools repositoryTools;
        private final ReviewContextManager reviewContextManager;
        BeanOutputConverter<CodeReviewResponse> converter =
        new BeanOutputConverter<>(CodeReviewResponse.class);

        public AiConnectionController(
                        ChatClient.Builder chatClientBuilder,
                        RepositoryTools repositoryTools, ReviewContextManager reviewContextManager) {

                ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

                ToolCallAdvisor toolCallAdvisor = ToolCallAdvisor.builder()
                                .toolCallingManager(toolCallingManager)
                                .build();

                this.chatClient = chatClientBuilder
                                .defaultAdvisors(toolCallAdvisor)
                                .build();

                this.repositoryTools = repositoryTools;
                this.reviewContextManager = reviewContextManager;
        }

        @GetMapping("/inspect")
public String inspectRepository(@RequestParam String repositoryPath) {

    ReviewContext reviewContext =
            reviewContextManager.create(repositoryPath);

    return chatClient
            .prompt()
            .user("""
                    Review ID: %s

                    Inspect this repository and determine how ChatClient is configured and used.

                    You have access to repository tools:
                    - listFiles
                    - searchCode
                    - readFile

                    Rules:
                    - Use only relative paths inside the repository.
                    - Use searchCode when it helps locate relevant code.
                    - Read only the files needed to support your conclusions.
                    - Base conclusions only on inspected evidence.
                    - Do not guess.
                    - Stop when you have sufficient evidence.
                    - Keep the final answer concise.

                    """.formatted(reviewContext.reviewId()))
            .tools(repositoryTools)
            .call()
            .content();
}
        @GetMapping("/test")
        public String testConnection() {
                return chatClient

                                .prompt()
                                .user("Explain what a Spring Boot REST controller is in one sentence.")
                                .call()
                                .content();
        }

        @GetMapping("/review")
public CodeReviewResponse reviewRepository(
        @RequestParam String repositoryPath) {

    ReviewContext reviewContext =
            reviewContextManager.create(repositoryPath);

    BeanOutputConverter<CodeReviewResponse> converter =
            new BeanOutputConverter<>(CodeReviewResponse.class);

    String response = chatClient
            .prompt()
            .user("""
                    Review ID: %s

                    Perform a focused code review of this Java/Spring repository.

                    You have access to:
                    - listFiles
                    - searchCode
                    - readFile

                    Review for:
                    - likely bugs
                    - unsafe error handling
                    - security risks
                    - poor Spring practices
                    - maintainability concerns

                    Investigation rules:
                    - Use repository tools to gather evidence.
                    - Use relative paths only.
                    - Search before reading large numbers of files.
                    - Read only files relevant to potential findings.
                    - Do not report an issue unless inspected code proves it.
                    - Do not invent line numbers.
                    - Do not infer behavior from filenames alone.
                    - If evidence is weak, do not report the finding.
                    - Stop when sufficient evidence has been collected.

                    Severity rules:
                    - CRITICAL: severe security breach, data loss,
                      or system compromise.
                    - HIGH: likely production failure, major vulnerability,
                      or serious correctness issue.
                    - MEDIUM: meaningful defect, reliability problem,
                      or maintainability risk.
                    - LOW: minor issue or improvement.
                    - INFO: observation only.

                    Confidence rules:
                    - HIGH: directly proven by inspected code.
                    - MEDIUM: strongly supported but some context is missing.
                    - LOW: do not include the finding.

                    Important:
                    - No evidence means no finding.
                    - Severity must match demonstrated impact.
                    - Do not exaggerate severity.
                    - Do not report stylistic preferences as defects.
                    - If the exact line number is unknown, return null.
                    - Keep the review concise.
                    - Return findings only when there is meaningful evidence.

                    %s

                    """.formatted(
                            reviewContext.reviewId(),
                            converter.getFormat()))
            .tools(repositoryTools)
            .call()
            .content();

    log.info("Raw review response: {}", response);

    String jsonResponse = extractJson(response);

    return converter.convert(jsonResponse);
}

private String extractJson(String response) {

        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException(
                    "AI returned an empty response");
        }
    
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
    
        if (start == -1 || end == -1 || end <= start) {
            throw new IllegalArgumentException(
                    "No valid JSON object found in AI response");
        }
    
        return response.substring(start, end + 1);
    }

}