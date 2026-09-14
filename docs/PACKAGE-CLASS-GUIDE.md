# Package and Class Guide

## Backend

| Package | Current classes | Responsibility |
| --- | --- | --- |
| root | `AiCodeReviewAgentApplication` | Spring Boot entry point. |
| `api` | `CodeReviewController`, `CodeReviewRequest`, `StartReviewResponse`, `CodeReviewResultResponse` | HTTP boundary. The controller starts reviews, returns executions, reads stored activities, and opens SSE emitters. `CodeReviewResultResponse` remains a DTO type but the current result endpoint returns `ReviewExecution`. |
| `review` | `CodeReviewService`, `AsyncReviewExecutor`, `ReviewContext`, `ReviewContextManager`, `ReviewExecution`, `ReviewExecutionStore`, `InMemoryReviewExecutionStore`, `ReviewStatus`, `CodeReviewResponse`, `CodeReviewFinding`, `CodeReviewResult` | Core orchestration. Context ties an ID to a normalized root; execution store tracks lifecycle; executor runs the AI call asynchronously and persists terminal state; records define the structured result. |
| `activity` | `ReviewActivityType`, `ReviewActivityEvent`, `ReviewActivityPublisher`, `DefaultReviewActivityPublisher`, `ReviewActivityStore`, `InMemoryReviewActivityStore` | Activity vocabulary, immutable events, storage contract, in-memory history, and publisher fan-out. |
| `streaming` | `ReviewActivitySubscriber`, `SseReviewActivitySubscriber` | Subscriber contract and Spring `SseEmitter` transport. The concrete subscriber replays stored events, supports multiple listeners, and cleans up emitters. |
| `tool` | `RepositoryTools` | Spring AI `@Tool` methods: `getRepositoryTree`, `listFiles`, `readFile`, and `searchCode`. It resolves review-relative paths safely and publishes tool activity. |
| `observability` | `ReviewTracingService`, `ChatModelCompletionContentObservationFilter` | Review-level Micrometer span creation and Spring AI prompt/completion content mapping for Langfuse observations. |
| `config` | `AsyncConfig`, `OpenApiConfig` | Enables `@Async`; supplies OpenAPI metadata for Swagger. |

Important relationships: `CodeReviewService` owns the fast start path and calls `AsyncReviewExecutor`. The executor uses `RepositoryTools`, `ReviewExecutionStore`, `ReviewActivityPublisher`, and `ReviewTracingService`. Activity producers depend on the publisher interface; `DefaultReviewActivityPublisher` depends on the neutral store and subscriber list, avoiding a publisher/subscriber circular dependency.

## Frontend

| File | Responsibility |
| --- | --- |
| `src/App.jsx` | Owns review state, starts reviews, manages EventSource and polling cleanup, suppresses duplicate activities, tracks elapsed time, retrieves terminal results, and resets the UI. |
| `src/components/RepositoryForm.jsx` | Accessible repository-path form with starting/running button states. |
| `src/components/ReviewStatus.jsx` | Status badge, elapsed time, connection feedback, failure message, and clipboard copy feedback for the review ID. |
| `src/components/ActivityTimeline.jsx` | Maps backend activity types to readable labels, timestamps events, and scrolls its internal timeline to the newest event. |
| `src/components/ReviewSummary.jsx` | Completed-review summary and finding count. |
| `src/components/FindingCard.jsx` | Renders severity, category, file/line, issue, evidence, recommendation, and confidence. |
| `src/services/reviewApi.js` | Native `fetch` wrappers for REST endpoints and the native `EventSource` factory. |

The frontend uses no state-management or UI framework. Vite proxies `/api` to the Spring Boot server in local development.
