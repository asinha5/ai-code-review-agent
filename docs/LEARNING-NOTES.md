# Learning Notes

1. An agentic call is a loop: the model requests a tool, Spring AI executes it, the observation returns to the model, and only then can it produce a final answer.
2. `@Tool` exposes a capability; `@ToolParam` makes its generated schema clear enough for the model to call it correctly.
3. A review ID is safer than trusting model-provided absolute paths. The server maps it to a normalized repository root and accepts only paths beneath it.
4. A structured `CodeReviewResponse` gives the UI stable fields. `BeanOutputConverter` helps conversion but cannot repair malformed JSON.
5. Low output-token limits can truncate JSON before closing braces. `max-tokens` is a ceiling, not a promise that a model will use every token or finish correctly.
6. Groq free-tier ITPM/OTPM limits can be consumed quickly by tool loops because trees, source excerpts, schemas, and tool observations all add input tokens.
7. Anthropic Claude is the currently configured working provider. Provider choice still depends on reliable tool calling and structured output, not only model quality.
8. `@Async` lets the HTTP start request return a review ID while the long model call continues in the background.
9. An explicit `PENDING`, `RUNNING`, `COMPLETED`, and `FAILED` lifecycle makes asynchronous state visible to both API clients and the UI.
10. SSE is a good fit for one-way live progress. Polling remains useful as a recovery path when a browser stream cannot stay connected.
11. Replaying stored events protects clients that connect after a review has already started.
12. A per-review list of emitters supports multiple browser tabs. Emitter timeout, error, and completion cleanup prevents stale subscribers from accumulating.
13. Publisher/store/subscriber separation avoids circular dependencies: producers publish to one interface; the publisher stores and fans out; subscribers consume events independently.
14. React can consume asynchronous AI workflows with ordinary `useState`, `useEffect`, and `useRef`: keep the EventSource and polling timer in refs, then clean both up on reset and unmount.
15. Vite’s `/api` proxy avoids browser CORS configuration during local development because the browser calls the Vite origin while Vite forwards requests to Spring Boot.
16. Langfuse is for AI/agent observability, while SLF4J is developer logging and `ReviewActivityEvent` is concise user-facing progress.
17. OpenTelemetry is the telemetry format and transport ecosystem; Micrometer’s tracing bridge lets Spring Boot code create spans through a familiar `Tracer` API.
18. `ReviewTracingService` correlates one review with an `ai-code-review` span via `review.id`, then records status, finding count, and errors at completion.
19. Spring AI model observations can include provider, model, token, and latency data when the provider and instrumentation expose it.
20. Prompt/completion observation is sensitive. It can export AI prompts, model responses, source excerpts, and findings/evidence. Enable it only with an approved backend and suitable retention/access controls; the current configuration also enables prompt/completion logging.
21. The completion observation filter adds Langfuse input/output fields from Spring AI request instructions and response text. This makes traces more useful but increases the amount of sensitive content exported.
22. Friendly failure messages should distinguish rate limits, timeouts, authentication errors, and invalid structured output without exposing stack traces to the UI.
23. Future interview discussion should distinguish operational progress, diagnostics, and observability instead of treating all emitted information as application logging.
