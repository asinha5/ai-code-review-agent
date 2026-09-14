# Learning Notes

These are practical lessons from implementing and testing the current agent.

## Agent and tool loop

1. **A normal LLM call and an agentic call have different control flow.** A normal call is prompt to response. Here, the model can decide to call a tool, Spring AI executes it, the observation returns to the model, and the cycle repeats before the final response.
2. **`@Tool` and `@ToolParam` describe different things.** `@Tool` exposes and describes a callable capability. `@ToolParam` explains each argument in the generated schema, such as the exact `reviewId` and repository-relative path expected.
3. **Native tool calling depends on provider and model support.** A valid Java method is insufficient if the selected model cannot emit compatible tool calls reliably.
4. **Schema quality affects reliability.** Specific names, descriptions, examples, constraints, and parameter guidance reduce invented IDs, wrong paths, and unnecessary calls.
5. **Agent loops amplify token use.** Every cycle carries prompt/tool context, observations, and conversation state. One extra traversal can cost much more than one ordinary API call.
6. **A coarse tool can be more efficient than many small tools.** `getRepositoryTree` gives initial repository awareness in one call and avoids repeated `listFiles` directory walking. The model can still follow up selectively.

## Repository safety and context

7. **Relative paths are safer than model-generated absolute paths.** Tools accept an opaque review ID plus a path relative to a known root.
8. **`reviewId` preserves server-controlled context.** `ReviewContextManager` associates a generated UUID with the normalized repository root, so the model never becomes the source of authority for that root.
9. **Normalization plus a root-prefix check enforces the boundary.** Resolve the relative path, normalize it, and require `resolved.startsWith(repositoryRoot)` before access. This rejects straightforward `..` traversal outside the review repository.
10. **Traversal needs explicit exclusions and output limits.** Ignoring generated, dependency, IDE, and VCS directories keeps observations relevant and bounded.

## Providers, limits, and output

11. **Provider experiments expose meaningful differences.** Anthropic Claude, Groq through an OpenAI-compatible integration, and earlier Ollama/Qwen local models showed different tool-calling behavior and free-tier/token constraints. The design should not assume one provider forever.
12. **Output-token limits can truncate structured JSON.** A response that begins correctly may end before its closing braces, making conversion fail.
13. **Input tokens per minute disappear quickly in tool loops.** Repository trees, source files, search results, schemas, and repeated context can hit rate limits even when the final answer is short.
14. **Free tiers are valuable but shape testing.** Rate and token limits favor small prompts, capped results, deterministic local tests, and fewer intentional live-model checkpoints.
15. **`BeanOutputConverter` is best-effort.** Its format instructions and conversion improve structure, but do not guarantee provider-native constrained output.
16. **JSON extraction has a narrow job.** Taking text from the first `{` through the last `}` removes surrounding prose. It cannot invent missing braces, recover truncation, or validate the semantics of a finding.
17. **No evidence means no finding.** The prompt should prefer a few high-confidence issues tied to inspected files and code over plausible but speculative advice.

## Activity, streaming, and observability

18. **Activity should expose actions, not private reasoning.** “Reading `SomeService.java`” is useful and verifiable; hidden model deliberation is neither necessary nor appropriate for the UI.
19. **SLF4J, `ReviewActivityEvent`, and Langfuse serve distinct audiences.** SLF4J is for technical application diagnostics. Activity events are user-facing progress. Planned Langfuse traces would cover model/tool execution, latency, tokens, providers, and cost where available.
20. **Publisher/store separation avoids circular dependencies.** If the publisher both stored events and depended on an SSE subscriber that read the store, Spring could face a publisher -> subscriber -> store/publisher cycle. `InMemoryReviewActivityStore` is a neutral component shared by both.
21. **SSE fits one-way progress.** The server needs to push activity while the browser mainly listens, so `text/event-stream` and `SseEmitter` are simpler than a bidirectional protocol.
22. **Replay protects late subscribers.** A client may obtain or learn a `reviewId` after activity has begun. Loading stored events first preserves earlier progress, then the same connection receives live events.
23. **One review can have multiple listeners.** A single-emitter map would replace an earlier tab. A `CopyOnWriteArrayList` per review lets all current tabs receive events, while a `ConcurrentHashMap` coordinates review IDs.
24. **Emitter lifecycle is part of correctness.** Timeout, client error, and completion must remove emitters. `REVIEW_COMPLETED` and `REVIEW_FAILED` should complete every emitter for the affected review.
25. **In-memory activity is intentionally temporary.** Thread-safe structures handle concurrent requests, but history resets on restart and is unsuitable for durable audit requirements.

## Testing strategy

26. **Separate deterministic tests from paid or model-dependent tests.** Context creation, path security, repository traversal, DTO conversion boundaries, activity storage, REST mappings, replay, multiple subscribers, and cleanup can be tested without an LLM. Use live providers specifically for autonomous tool selection, schema compatibility, prompt behavior, and finding quality.
27. **Test streaming concurrently.** Multiple `curl -N` clients attached to one review reveal replacement and cleanup mistakes that a single subscriber cannot. The current SSE replay/live flow has been exercised this way.

## What comes next

The implemented backend now has review orchestration, tool access, structured results, activity history, and live SSE. React/Vite, Langfuse, stronger validation, provider profiles, prompt optimization/caching experiments, MCP comparison, Git-aware review, PR integration, and optional persistence remain planned.
