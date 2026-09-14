# Roadmap

Implemented and planned work are deliberately separated. Planned items are experiments or future capabilities, not current behavior.

## Completed

### Foundation and AI integration

- Java 21, Spring Boot 3.5.x, Spring AI 1.1.x, and Maven project setup
- Spring AI `ChatClient` integration
- Provider experiments with Anthropic Claude, Groq/OpenAI-compatible access, and earlier Ollama/Qwen local models
- SLF4J application logging

### Native repository tools

- Native Spring AI `@Tool` methods and descriptive `@ToolParam` schemas
- Model-driven autonomous tool selection
- `getRepositoryTree`, `listFiles`, `readFile`, and `searchCode`
- Ignored-directory and searchable-file filtering
- Coarse-grained repository tree inspection to reduce repeated tool calls

### Secure review context

- `ReviewContext` and `ReviewContextManager`
- Generated UUID review IDs associated with normalized repository roots
- Repository-relative tool paths
- Normalization and repository-boundary checks for path traversal protection

### Structured review

- `CodeReviewFinding`, `CodeReviewResponse`, and `CodeReviewResult`
- Severity, category, evidence, recommendation, and confidence fields
- Prompt rules for a small number of meaningful, evidence-backed findings
- `BeanOutputConverter` and defensive JSON extraction
- Review orchestration service
- `POST /api/reviews` response containing review ID, summary, and findings

### Review activity

- Dedicated `activity` package
- `ReviewActivityType` and `ReviewActivityEvent`
- `ReviewActivityPublisher` and `DefaultReviewActivityPublisher`
- `ReviewActivityStore` and thread-safe `InMemoryReviewActivityStore`
- Service lifecycle and repository-tool activity publication
- `GET /api/reviews/{reviewId}/activities`

### SSE live streaming

- Dedicated `streaming` package
- `ReviewActivitySubscriber` and `SseReviewActivitySubscriber`
- `GET /api/reviews/{reviewId}/stream` with `text/event-stream`
- Replay of stored events followed by live event delivery
- Multiple concurrent SSE subscribers per review ID
- Timeout, error, completion, and terminal-event cleanup
- Concurrent `curl` verification of multiple subscribers

### API documentation

- springdoc OpenAPI integration and configuration
- Swagger UI
- Controller `@Tag` and endpoint `@Operation` annotations
- SSE media type represented by its endpoint declaration

## Next

1. **React + Vite UI** — add the browser application shell and repository-path review form.
2. **Connect the UI to the SSE activity stream** — subscribe by `reviewId` and manage connection states.
3. **Visual review timeline** — present replayed and live `ReviewActivityEvent` entries clearly.
4. **Review findings UI** — render the summary and evidence-based findings with severity and confidence.
5. **Langfuse observability** — correlate AI traces, tool calls, latency, token usage, provider metadata, and errors with a review ID.
6. **Centralized exception handling** — provide consistent responses for invalid paths, unknown reviews, provider errors, and malformed output.
7. **Finding validation and guardrails** — verify required fields, supported classifications, evidence quality, and useful file/line references.
8. **Token and prompt optimization** — tune prompts and tool-result limits while measuring quality, latency, and call counts.
9. **Provider profiles and configuration** — make model/provider selection explicit without fixing the application to one provider.
10. **Prompt caching experiments** — evaluate provider-supported caching for stable prompt and tool context.
11. **MCP comparison** — implement an MCP-based variant and compare it with native Spring AI tools.
12. **Git diff-aware review** — inspect changed code rather than always reviewing an entire repository.
13. **GitHub pull request integration** — obtain PR metadata/diffs and prepare review output for that workflow.
14. **Optional persistence and history** — retain reviews and activity only when durable history becomes a requirement.
15. **Optional multi-agent experiments** — compare specialist agents and synthesis against the simpler single-agent design after the core workflow is stable.

## Development principle

Keep deterministic plumbing tests separate from paid or provider-dependent checks. Use live models to validate agent behavior and tool compatibility; use local tests for security, storage, REST, SSE, DTO, and repository-tool behavior.
