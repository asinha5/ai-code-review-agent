# Package and Class Guide

This guide describes the classes currently present under `com.aicodereview.agent`, their responsibilities, collaborators, important methods, and reason for existing.

## `api`

### `CodeReviewController`

- **Responsibility:** HTTP boundary for starting reviews, reading stored activities, and opening SSE streams.
- **Key collaborators:** `CodeReviewService`, `ReviewActivityStore`, and `SseReviewActivitySubscriber`.
- **Important methods:** `review`, `getActivities`, and `streamReviewActivity` implement `POST /api/reviews`, `GET /api/reviews/{reviewId}/activities`, and `GET /api/reviews/{reviewId}/stream` respectively.
- **Why it exists:** Keeps HTTP mapping and DTO conversion separate from review orchestration, filesystem access, and AI prompts. The class is tagged for OpenAPI, its endpoints use `@Operation`, and the stream declares `text/event-stream`.

### `CodeReviewRequest`

- **Responsibility:** Request record containing `repositoryPath`.
- **Key collaborators:** Accepted by `CodeReviewController` and passed as a string to `CodeReviewService`.
- **Important methods:** Record accessor `repositoryPath()`.
- **Why it exists:** Gives the external request an explicit contract independent of domain objects.

### `CodeReviewResultResponse`

- **Responsibility:** Response record containing `reviewId`, `summary`, and `List<CodeReviewFinding>`.
- **Key collaborators:** Constructed by `CodeReviewController` from `CodeReviewResult`.
- **Important methods:** Record accessors for all three fields.
- **Why it exists:** Defines the public review response without coupling the review service to an API DTO.

## `review`

### `CodeReviewService`

- **Responsibility:** Orchestrates one review from context creation through the model/tool loop to structured output and lifecycle activity.
- **Key collaborators:** `ChatClient`, `RepositoryTools`, `ReviewContextManager`, `ReviewActivityPublisher`, `BeanOutputConverter`, `CodeReviewResponse`, and `CodeReviewResult`.
- **Important methods:** `review` validates input, creates a context, publishes lifecycle events, invokes the model with tools, extracts JSON, converts it, and returns a result. Private `buildReviewPrompt`, `validateRepositoryPath`, and `extractJson` support that flow.
- **Why it exists:** Centralizes the use-case workflow while leaving HTTP, filesystem mechanics, context storage, and event delivery to focused components.

The prompt directs the model to inspect evidence, prefer `getRepositoryTree`, minimize tool calls, produce at most three meaningful findings, and omit unsupported suspicions. `BeanOutputConverter` is best-effort conversion; `extractJson` can remove surrounding prose but cannot repair malformed or truncated JSON.

### `CodeReviewFinding`

- **Responsibility:** Immutable record for one finding.
- **Key collaborators:** Contained by `CodeReviewResponse` and exposed through `CodeReviewResultResponse`.
- **Important methods:** Accessors for `severity`, `category`, `file`, `line`, `issue`, `evidence`, `recommendation`, and `confidence`.
- **Why it exists:** Turns free-form review prose into a stable, actionable shape with evidence and confidence.

### `CodeReviewResponse`

- **Responsibility:** Internal model-output record containing `summary` and `findings`.
- **Key collaborators:** Produced through `BeanOutputConverter` and wrapped by `CodeReviewResult`.
- **Important methods:** `summary()` and `findings()`.
- **Why it exists:** Defines the structure requested from the model independently of review identity and HTTP concerns.

### `CodeReviewResult`

- **Responsibility:** Internal service result pairing `reviewId` with `CodeReviewResponse`.
- **Key collaborators:** Returned by `CodeReviewService` and mapped by `CodeReviewController`.
- **Important methods:** `reviewId()` and `review()`.
- **Why it exists:** Carries correlation identity without making the service depend on `CodeReviewResultResponse`.

### `ReviewContext`

- **Responsibility:** Immutable association between a generated `reviewId` and server-controlled `Path repositoryRoot`.
- **Key collaborators:** Created and retrieved by `ReviewContextManager`; consumed by `RepositoryTools`.
- **Important methods:** `reviewId()` and `repositoryRoot()`.
- **Why it exists:** Lets tool calls use an opaque ID and relative path instead of trusting model-generated absolute paths.

### `ReviewContextManager`

- **Responsibility:** Validates repository directories, normalizes roots, creates UUID review IDs, and retains contexts in a `ConcurrentHashMap`.
- **Key collaborators:** `CodeReviewService`, `ReviewContext`, and `RepositoryTools`.
- **Important methods:** `create(repositoryPath)` and `get(reviewId)`.
- **Why it exists:** Makes repository authority and review correlation a server-side concern. Unknown IDs and invalid paths are rejected.

## `activity`

### `ReviewActivityType`

- **Responsibility:** Enum of allowed activity categories.
- **Key collaborators:** `ReviewActivityEvent`, `CodeReviewService`, `RepositoryTools`, and SSE terminal-event handling.
- **Important values:** `REVIEW_STARTED`, `REPOSITORY_INSPECTION`, `FILE_READING`, `CODE_SEARCH`, `ANALYZING`, `GENERATING_FINDINGS`, `REVIEW_COMPLETED`, and `REVIEW_FAILED`.
- **Why it exists:** Provides a stable event vocabulary instead of scattered string constants.

### `ReviewActivityEvent`

- **Responsibility:** Immutable user-visible activity record with `reviewId`, `type`, `message`, and `Instant timestamp`.
- **Key collaborators:** Created by `DefaultReviewActivityPublisher`, stored through `ReviewActivityStore`, and delivered by subscribers.
- **Important methods:** Record accessors for its four fields.
- **Why it exists:** Exposes observable review actions without exposing private model reasoning.

### `ReviewActivityPublisher`

- **Responsibility:** Write-side contract for activity.
- **Key collaborators:** Called by `CodeReviewService` and `RepositoryTools`; implemented by `DefaultReviewActivityPublisher`.
- **Important method:** `publish(reviewId, type, message)`.
- **Why it exists:** Decouples activity producers from storage, logging, SSE, and future delivery mechanisms.

### `ReviewActivityStore`

- **Responsibility:** Contract for storing, reading, and clearing activity history by review ID.
- **Key collaborators:** Implemented by `InMemoryReviewActivityStore`; used by the publisher, controller, and SSE subscriber.
- **Important methods:** `store(event)`, `getActivities(reviewId)`, and `clear(reviewId)`.
- **Why it exists:** Separates event history from publication and transport, avoiding a circular dependency and allowing storage to be replaced later.

### `DefaultReviewActivityPublisher`

- **Responsibility:** Creates timestamped events, writes them to the store, logs them through SLF4J, and notifies registered subscribers.
- **Key collaborators:** `ReviewActivityStore` and `List<ReviewActivitySubscriber>`.
- **Important method:** `publish`.
- **Why it exists:** Provides one fan-out point while callers depend only on `ReviewActivityPublisher`.

### `InMemoryReviewActivityStore`

- **Responsibility:** Thread-safe, non-persistent activity history keyed by `reviewId`.
- **Key collaborators:** `DefaultReviewActivityPublisher`, `CodeReviewController`, and `SseReviewActivitySubscriber` through the `ReviewActivityStore` interface.
- **Important methods:** `store`, `getActivities`, and `clear`.
- **Why it exists:** Gives publisher and replay consumers a neutral shared store. Its `ConcurrentHashMap` and thread-safe per-review lists support concurrent access; all data is lost on restart.

## `streaming`

### `ReviewActivitySubscriber`

- **Responsibility:** Consumer contract for newly published activity.
- **Key collaborators:** Invoked by `DefaultReviewActivityPublisher`; implemented by `SseReviewActivitySubscriber`.
- **Important method:** `onActivity(event)`.
- **Why it exists:** Keeps the publisher independent of SSE and allows additional activity consumers.

### `SseReviewActivitySubscriber`

- **Responsibility:** Creates SSE subscriptions, replays stored events, pushes live events, and owns emitter lifecycle.
- **Key collaborators:** `ReviewActivityStore`, `ReviewActivitySubscriber`, Spring `SseEmitter`, and `CodeReviewController`.
- **Important methods:** Public `subscribe(reviewId)` and `onActivity(event)`; private helpers replay stored activities, send events, complete review emitters, remove emitters, and count subscribers.
- **Why it exists:** Isolates HTTP streaming mechanics from review and publishing code. A `ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>` permits multiple subscribers per review; timeout/error/completion callbacks clean up, while completed/failed activities close related emitters.

## `tool`

### `RepositoryTools`

- **Responsibility:** Controlled repository inspection boundary exposed to the AI agent.
- **Key collaborators:** `ReviewContextManager` resolves review scope and `ReviewActivityPublisher` reports tool actions.
- **Why it exists:** Gives the model useful, bounded capabilities without arbitrary filesystem access.

AI-exposed methods use `@Tool`, and their parameters use descriptive `@ToolParam` annotations:

- `getRepositoryTree(reviewId)`: recursively returns up to the configured implementation limit of repository-relative entries, excluding ignored directories. It provides broad context in one call to reduce repeated walking and token use.
- `listFiles(reviewId, relativePath)`: lists one repository-relative directory for targeted follow-up.
- `readFile(reviewId, relativePath)`: reads a regular file discovered during inspection.
- `searchCode(reviewId, searchTerm)`: searches supported text files and returns capped `relative/path:line: text` matches.

Internal helper methods are **not AI tools** because they have no `@Tool` annotation:

- `resolveSecurePath`: retrieves the context, resolves and normalizes a relative path, and requires it to start with the repository root.
- `isIgnoredPath`: excludes any path containing `.git`, `.idea`, `.vscode`, `.mvn`, `target`, `node_modules`, `build`, or `dist`.
- `isSearchableFile`: admits `.java`, `.xml`, `.yml`, `.yaml`, `.properties`, `.md`, and `.json` files for search.
- `findMatches`: reads a searchable file line by line and formats case-insensitive matches with repository-relative paths and line numbers.

## `config`

### `OpenApiConfig`

- **Responsibility:** Supplies OpenAPI title, version, description, and supported-capability metadata.
- **Key collaborators:** springdoc and its `OpenAPI`/`Info` models.
- **Important method:** `codeReviewOpenApi()` bean factory.
- **Why it exists:** Keeps API-wide documentation metadata out of controllers while enabling Swagger UI.

## Package interaction

```text
api -> review -> Spring AI -> tool -> local repository
          |                   |
          +------> activity <-+
                     |
                     +-> in-memory store
                     `-> streaming -> SSE clients
```
