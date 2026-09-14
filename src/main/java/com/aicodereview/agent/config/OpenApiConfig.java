package com.aicodereview.agent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI codeReviewOpenApi() {

        return new OpenAPI()
                .info(new Info()
                        .title("AI Code Review Agent API")
                        .version("v1")
                        .description("""
                                REST API for the AI Code Review Agent.

                                Supports:
                                - Starting repository reviews
                                - Retrieving review activities
                                - Streaming review progress using SSE
                                """));
    }
}