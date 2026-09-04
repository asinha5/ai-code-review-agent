package com.aicodereview.agent.api;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiConnectionController {

    private final ChatClient chatClient;

    public AiConnectionController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/test")
    public String testConnection() {
        return chatClient
                .prompt()
                .user("Explain what a Spring Boot REST controller is in one sentence.")
                .call()
                .content();
    }
}