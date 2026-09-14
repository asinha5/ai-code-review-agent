# Roadmap

## Done

### Milestone 1 — Agentic Repository Review: COMPLETE

- Spring AI tools for repository tree, directory listing, file reading, and code search
- Server-owned `reviewId` context and repository-boundary checks
- Structured, evidence-oriented review response and findings
- Anthropic Claude configuration and Swagger/OpenAPI

### Milestone 2 — Async Execution and Live Activity: COMPLETE

- `PENDING`, `RUNNING`, `COMPLETED`, and `FAILED` execution lifecycle
- Asynchronous executor and in-memory execution store
- Stored activity history, activity publisher, SSE replay, multiple subscribers, and cleanup
- Result and activity REST endpoints

### Milestone 3 — React + Vite UI: COMPLETE

- Repository-path submission and asynchronous start handling
- SSE activity timeline with duplicate suppression and polling fallback
- Status, elapsed time, summary, findings, failed state, reset, and responsive developer-tool UI
- Vite `/api` proxy for local Spring Boot development

### Milestone 4 — Langfuse Observability: COMPLETE

- Actuator, Micrometer tracing bridge for OpenTelemetry, and OTLP exporter dependencies
- Review-level `ai-code-review` span correlated by `review.id`
- Terminal review status, finding-count, and error metadata
- Spring AI prompt/completion observation mapping for Langfuse input/output fields

## Next

1. Make Git diff-aware review the next major product and agent milestone.
2. Add hardening and centralized error handling for invalid paths, unknown IDs, provider failures, and malformed responses.
3. Add explicit provider profiles and configuration plus prompt/token optimization.
4. Add richer deterministic tests for review state, activity replay, SSE lifecycle, tools, and frontend behavior.

## Future

- GitHub pull-request integration
- Persistent review and activity history
- MCP implementation and comparison with native Spring AI tools
- Optional multi-agent review experiments
- Production authentication and deployment design

These items are not currently implemented.
