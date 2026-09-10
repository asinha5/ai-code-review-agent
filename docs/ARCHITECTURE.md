# Architecture

## 1. Purpose

The AI Code Review Agent is designed as a learning-focused agentic application.

Instead of sending an entire repository to an LLM in one prompt, the model receives a small set of repository tools and autonomously decides how to inspect the code.

This makes the system genuinely tool-driven:

```text
Goal
 |
 v
LLM decides next action
 |
 +--> tool call
 |      |
 |      v
 |   observation
 |      |
 +------+
 |
 v
final structured review
```

## 2. High-Level Architecture

```text
                         +-------------------+
                         |   React UI        |
                         |   planned         |
                         +---------+---------+
                                   |
                          REST / future SSE
                                   |
                                   v
                         +-------------------+
                         | API Layer         |
                         | CodeReviewController
                         +---------+---------+
                                   |
                                   v
                         +-------------------+
                         | Review Layer      |
                         | CodeReviewService |
                         +----+----------+---+
                              |          |
                 creates      |          | publishes
                 context      |          | activities
                              v          v
                    +---------------+   +----------------------+
                    | ReviewContext |   | ReviewActivity       |
                    | Manager       |   | Publisher / Store    |
                    +-------+-------+   +----------------------+
                            |
                            | reviewId -> repositoryRoot
                            |
                            v
                  +----------------------+
                  | Spring AI ChatClient |
                  +----------+-----------+
                             |
                             v
                     +---------------+
                     | LLM Provider  |
                     +-------+-------+
                             |
                     native tool calls
                             |
                             v
                  +----------------------+
                  | RepositoryTools      |
                  +----------+-----------+
                             |
                             v
                  +----------------------+
                  | Local Repository     |
                  +----------------------+
```

## 3. API Layer

The API package owns HTTP concerns.

It should know about:

- request payloads
- response payloads
- path variables
- status codes
- future SSE endpoints

It should not own:

- AI prompt logic
- repository traversal
- file access
- tool security
- model orchestration

This keeps the controller thin.

## 4. Review / Orchestration Layer

`CodeReviewService` coordinates a review.

Current sequence:

```text
validate repository path
        |
        v
create ReviewContext
        |
        v
publish REVIEW_STARTED
        |
        v
build structured-output prompt
        |
        v
publish ANALYZING
        |
        v
invoke ChatClient with RepositoryTools
        |
        v
model/tool loop executes
        |
        v
extract final JSON
        |
        v
convert to CodeReviewResponse
        |
        v
publish REVIEW_COMPLETED
```

Any exception results in `REVIEW_FAILED`.

The service intentionally does not directly perform filesystem operations.

## 5. Review Context

A review context contains:

```text
reviewId
repositoryRoot
```

Example:

```text
reviewId = 22ac1541-874a-4c0f-8960-6b803636d68c
repositoryRoot = C:\...\some-project
```

The LLM receives only the `reviewId` and relative paths when calling repository tools.

This provides two benefits:

1. The model does not repeatedly generate or manipulate the absolute root path.
2. Filesystem access can be validated server-side.

## 6. Tool Layer

`RepositoryTools` is the controlled boundary between the AI and the local filesystem.

Current tools:

```text
getRepositoryTree(reviewId)
listFiles(reviewId, relativePath)
readFile(reviewId, relativePath)
searchCode(reviewId, searchTerm)
```

The tool methods are exposed to Spring AI with `@Tool`.

Parameters use `@ToolParam` descriptions to make the tool contract explicit to the model.

### Why `getRepositoryTree` exists

Originally the agent explored repositories with multiple calls:

```text
.
src
src/main
src/main/java
...
```

This worked but consumed unnecessary model input tokens.

`getRepositoryTree` provides coarse-grained discovery in one call:

```text
Repository tree
     |
     v
Agent selects relevant files
     |
     +--> readFile
     +--> searchCode
```

This reduces repetitive tool calls.

## 7. Secure Path Resolution

For file-oriented tools:

```java
repositoryRoot
        .resolve(relativePath)
        .normalize();
```

Then:

```java
if (!resolved.startsWith(repositoryRoot)) {
    throw new IllegalArgumentException(...);
}
```

Conceptually:

```text
AI asks for relative path
        |
        v
server resolves path
        |
        v
normalize
        |
        v
inside configured root?
     /       \
   yes       no
    |         |
 allow      reject
```

## 8. Structured Output

The model returns a structured review rather than arbitrary prose.

```text
CodeReviewResponse
 |
 +-- summary
 |
 +-- findings[]
       |
       +-- severity
       +-- category
       +-- file
       +-- line
       +-- issue
       +-- evidence
       +-- recommendation
       +-- confidence
```

`BeanOutputConverter<CodeReviewResponse>` supplies the expected output structure to the model and converts the final JSON into Java records.

A small `extractJson()` method currently protects against responses that contain surrounding prose.

It does not repair truncated or malformed JSON.

## 9. Review Activity Architecture

Review activity is intentionally separate from technical logging.

```text
Business / agent action
        |
        v
ReviewActivityPublisher
        |
        v
ReviewActivityEvent
        |
        +--> application log
        |
        +--> in-memory activity store
        |
        +--> future SSE subscribers
```

Current event types include:

- REVIEW_STARTED
- REPOSITORY_INSPECTION
- FILE_READING
- CODE_SEARCH
- ANALYZING
- GENERATING_FINDINGS
- REVIEW_COMPLETED
- REVIEW_FAILED

The event message describes an observable action, not the model's private reasoning.

Examples:

```text
Inspecting repository structure
Reading file: src/main/java/.../CodeReviewService.java
Searching code for: ChatClient
Code review completed
```

## 10. Why Publisher and Store Are Separate Interfaces

Two responsibilities are exposed:

```text
ReviewActivityPublisher
        |
        +--> write events

ReviewActivityStore
        |
        +--> read / clear events
```

The current implementation supports both interfaces.

This gives callers a narrow dependency:

- `CodeReviewService` and `RepositoryTools` need to publish.
- API/SSE code needs to read or subscribe.
- Neither side needs to know the concrete implementation.

## 11. Logging vs Activity Events vs Langfuse

The project uses three different observability concepts.

### SLF4J

Purpose:

- application diagnostics
- exceptions
- tool invocation debugging
- developer troubleshooting

### ReviewActivityEvent

Purpose:

- user-visible progress
- future React activity timeline
- concise review lifecycle events

### Langfuse — planned

Purpose:

- AI trace
- model invocation
- latency
- tool usage
- token usage
- errors
- model/provider cost

These should remain separate.

## 12. Current State Management

Current state is intentionally in memory.

```text
ReviewContextManager
    -> active review context

DefaultReviewActivityPublisher
    -> activity events by reviewId
```

No database is required yet.

Persistence can be introduced later only when review history becomes a real requirement.

## 13. Future SSE Flow

Planned:

```text
React
 |
 | GET /api/reviews/{reviewId}/stream
 |
 v
SseEmitter
 |
 v
Review activity subscriber
 |
 v
ReviewActivityPublisher
```

The important architectural rule is that `RepositoryTools` should never depend directly on `SseEmitter`.

The domain publishes an activity event; transport code decides how to deliver it.

## 14. Future MCP Direction

Native Spring AI tools are being implemented first so the mechanics of tool calling are clear.

Later, repository access can be exposed through MCP:

```text
Current:
LLM -> Spring AI @Tool -> RepositoryTools

Future experiment:
LLM -> MCP client -> MCP repository/filesystem server
```

This will allow a direct comparison of native tools versus MCP rather than treating MCP as magic infrastructure.
