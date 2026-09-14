# Architecture

## Purpose

The application is a tool-driven code reviewer. Instead of placing a whole repository in one prompt, it gives an LLM constrained repository capabilities and lets the model decide what evidence it needs. The final response is converted into an application-level structured result.

## Main review flow

```text
Client
  |
  v
CodeReviewController
  |
  v
CodeReviewService
  |
  +--> ReviewContextManager
  |
  +--> ReviewActivityPublisher
  |
  +--> ChatClient
           |
           v
          LLM
           |
           v
     RepositoryTools
           |
           v
     local repository
           |
           v
     observations back to model
           |
           v
     structured review result
```

`ReviewContextManager` creates a UUID `reviewId`, normalizes the caller-supplied repository root, and retains the mapping in memory. Tool calls carry the opaque ID and repository-relative paths, keeping the authoritative root on the server.

`ChatClient` uses native Spring AI tool calling. The model can autonomously call `getRepositoryTree`, `listFiles`, `readFile`, and `searchCode`, observe their results, and repeat until it can return a review. `@ToolParam` descriptions make the generated tool schema explicit. Anthropic Claude, Groq/OpenAI-compatible access, and earlier Ollama/Qwen models have been tried; provider/model support and limits influence how reliably this loop works.

The review prompt requires inspected evidence, allows only a small number of meaningful findings, and assigns severity and confidence. A `BeanOutputConverter<CodeReviewResponse>` supplies the requested format and performs DTO conversion. Before conversion, the service extracts the outer JSON object defensively to remove surrounding prose; it does not attempt to repair truncated JSON.

## Repository boundary

```text
reviewId -> ReviewContext -> normalized repositoryRoot
                                  |
                                  v
              root.resolve(relativePath).normalize()
                                  |
                                  v
                     resolved.startsWith(root)
```

Any path outside the repository root is rejected. Tree and search traversal exclude `.git`, `.idea`, `.vscode`, `.mvn`, `target`, `node_modules`, `build`, and `dist`.

`getRepositoryTree` provides a coarse-grained overview first. It was introduced to avoid repeated directory-by-directory `listFiles` calls, reducing round trips and token consumption during an agent loop. Targeted listing, reading, and searching remain available afterward.

## Structured result

The model returns `CodeReviewResponse` (`summary` and `findings`). Each `CodeReviewFinding` has severity, category, file, optional line, issue, evidence, recommendation, and confidence. `CodeReviewService` combines this with the generated ID in `CodeReviewResult`; the controller maps it to `CodeReviewResultResponse` for `POST /api/reviews`.

## Activity flow

```text
CodeReviewService / RepositoryTools
              |
              v
     ReviewActivityPublisher
              |
              v
     ReviewActivityEvent
              |
              +--> InMemoryReviewActivityStore
              |
              +--> ReviewActivitySubscriber
                         |
                         v
              SseReviewActivitySubscriber
                         |
                         v
                    SseEmitter
                         |
                         v
                    client/browser
```

The current types are `REVIEW_STARTED`, `REPOSITORY_INSPECTION`, `FILE_READING`, `CODE_SEARCH`, `ANALYZING`, `GENERATING_FINDINGS`, `REVIEW_COMPLETED`, and `REVIEW_FAILED`.

`DefaultReviewActivityPublisher` creates and logs each event, writes it through `ReviewActivityStore`, and notifies all `ReviewActivitySubscriber` implementations. `InMemoryReviewActivityStore` uses thread-safe data structures keyed by `reviewId`; it supports reading and clearing but is non-persistent and resets on application restart.

The responsibilities are deliberately separate:

- Publisher: accepts activity from review code and fans it out.
- Store: owns historical activity independently of delivery.
- Subscriber: consumes newly published events without coupling producers to a transport.
- SSE delivery: manages HTTP emitters, replay, sending, and cleanup.

This keeps `CodeReviewService` and `RepositoryTools` unaware of HTTP or `SseEmitter`, makes storage replaceable, and permits other subscribers later.

## SSE replay and live delivery

```text
subscriber connects
      |
      v
load stored activities
      |
      v
replay historical events
      |
      v
keep connection open
      |
      v
push new events live
```

`GET /api/reviews/{reviewId}/stream` explicitly produces `text/event-stream`. `SseReviewActivitySubscriber` keeps a `ConcurrentHashMap` from review ID to a `CopyOnWriteArrayList<SseEmitter>`, allowing multiple clients or tabs to follow one review. Timeout, error, and normal-completion callbacks remove emitters. New activities are pushed to every active emitter, and `REVIEW_COMPLETED` or `REVIEW_FAILED` closes all emitters for that review. Replay and multi-subscriber behavior have been tested with concurrent `curl` connections.

## Avoiding a circular dependency

A design in which the publisher also implemented the readable store could form this Spring bean cycle:

```text
DefaultReviewActivityPublisher
  -> subscribers
  -> SseReviewActivitySubscriber
  -> ReviewActivityStore
  -> DefaultReviewActivityPublisher
```

Storage is instead an independent component:

```text
InMemoryReviewActivityStore
   ^                  ^
   |                  |
Publisher          SSE Subscriber
```

The publisher writes history; the SSE subscriber reads it for replay. The broader lesson is to separate shared state from components that notify and consume one another.

## Observability boundaries

- SLF4J logs are technical application diagnostics for operators and developers.
- `ReviewActivityEvent` is a stable, user-visible description of observable actions such as reading a file; it is not private model reasoning.
- Planned Langfuse integration would capture AI traces, tool calls, latency, token usage, provider metadata, and related diagnostics.

These concerns complement one another and should not be collapsed into one event stream.

## HTTP and OpenAPI

The implemented public API is:

```text
POST /api/reviews
GET  /api/reviews/{reviewId}/activities
GET  /api/reviews/{reviewId}/stream
```

springdoc provides OpenAPI generation and Swagger UI. `CodeReviewController` uses `@Tag` and `@Operation`, `OpenApiConfig` supplies API metadata, and the stream mapping declares `MediaType.TEXT_EVENT_STREAM_VALUE`.

## Implemented versus planned

The review loop, secure tools, structured findings, activity store, REST activity access, SSE replay/live streaming, multiple subscribers, and OpenAPI documentation are implemented. The React UI, Langfuse, MCP comparison, Git-aware review, PR integration, and persistence remain planned; see [Roadmap](ROADMAP.md).
