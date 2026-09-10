# Roadmap

The roadmap is intentionally incremental. Each phase should be implemented, run, observed, understood, and documented before moving to the next.

## Phase 1 — Foundation ✅

Completed:

- Java 21 project
- Spring Boot application
- Maven setup
- Git repository
- Spring AI integration
- basic AI connectivity

Learning focus:

```text
Spring Boot + Spring AI fundamentals
```

## Phase 2 — Native Agent Tools ✅

Completed:

- `listFiles`
- `readFile`
- `searchCode`
- `getRepositoryTree`
- `@Tool`
- `@ToolParam`
- model-driven native tool calling

Learning focus:

```text
LLM decides -> Spring AI executes tool -> result returns to LLM
```

## Phase 3 — Secure Review Context ✅

Completed:

- `ReviewContext`
- `ReviewContextManager`
- UUID review IDs
- normalized repository root
- relative tool paths
- path traversal protection
- ignored repository directories

Learning focus:

```text
Never make the LLM the security boundary.
```

## Phase 4 — Structured Review ✅

Completed:

- `CodeReviewFinding`
- `CodeReviewResponse`
- structured prompt
- severity and confidence
- evidence rules
- maximum finding count
- JSON conversion
- API response with review ID

Learning focus:

```text
Turn agent output into a stable application contract.
```

## Phase 5 — Review Activity ✅

Completed:

- `ReviewActivityType`
- `ReviewActivityEvent`
- `ReviewActivityPublisher`
- `ReviewActivityStore`
- `DefaultReviewActivityPublisher`
- in-memory event storage
- service-level lifecycle activities
- tool-level activities
- REST retrieval by review ID

Learning focus:

```text
Expose agent actions without exposing private reasoning.
```

## Phase 6 — Server-Sent Events ⏭ NEXT

Planned:

- review activity subscriber mechanism
- SSE connection management
- `/api/reviews/{reviewId}/stream`
- push new activity events to connected clients
- connection cleanup
- completion/error handling
- test with Postman or curl without an LLM

Target flow:

```text
ReviewActivityPublisher
        |
        v
SSE subscriber
        |
        v
browser / React
```

Learning focus:

```text
Live server-to-client streaming.
```

## Phase 7 — React + Vite UI

Planned:

- repository path input
- start-review action
- active review ID
- live activity timeline
- review summary
- finding cards
- severity indicators
- loading/error/completed states

Possible layout:

```text
+--------------------------------------------------+
| Repository path                                  |
| [........................................] Review |
+--------------------------------------------------+
| Live Agent Activity                              |
| ✓ Review started                                 |
| ✓ Inspecting repository                          |
| → Reading CodeReviewService.java                 |
+--------------------------------------------------+
| Review Summary                                   |
+--------------------------------------------------+
| Findings                                         |
| HIGH | Security | ...                            |
| MED  | Design   | ...                            |
+--------------------------------------------------+
```

Learning focus:

```text
Designing a frontend around an asynchronous agent workflow.
```

## Phase 8 — Langfuse Observability

Planned:

- trace each review
- correlate with review ID
- model invocation timing
- tool calls
- token usage
- model/provider metadata
- errors
- cost where available

Keep separate from `ReviewActivityEvent`.

Learning focus:

```text
Agent observability vs application logging vs UI progress.
```

## Phase 9 — Validation and Resilience

Planned:

- centralized API exception handling
- review-not-found handling
- invalid repository errors
- file-size limits
- binary-file protection
- maximum tree/search output
- timeout strategy
- malformed model output handling
- model/provider error mapping
- possible finding verification

Learning focus:

```text
Moving from demo behavior toward production-grade boundaries.
```

## Phase 10 — Prompt and Token Optimization

Planned experiments:

- reduce prompt repetition
- minimize tool descriptions without losing reliability
- cap repository tree
- selective source-file reading
- smarter search limits
- compare tool-call counts
- compare model latency
- compare token usage

Learning focus:

```text
Agent quality, cost, and latency are coupled.
```

## Phase 11 — Prompt Caching

Optional, provider-dependent.

Experiment with caching stable prompt/tool context where supported.

Learning focus:

```text
When caching helps long or repeated agent interactions.
```

## Phase 12 — MCP

After native tools are well understood:

- add MCP client
- expose repository/filesystem capabilities through MCP
- compare MCP tools with native Spring AI tools
- evaluate portability and complexity

Learning focus:

```text
MCP as a standardized tool/context protocol.
```

## Phase 13 — Git-Aware Review

Planned:

- detect Git repository
- read changed files
- compare branches or commits
- parse diff
- review only changed code where appropriate

Target use case:

```text
Review my current changes rather than the entire repository.
```

## Phase 14 — GitHub Pull Request Review

Planned:

- GitHub integration
- retrieve PR metadata and diff
- run agent review against PR
- optionally prepare review comments

Learning focus:

```text
Agent integration with real engineering workflows.
```

## Phase 15 — Persistence — Only If Needed

Possible PostgreSQL use cases:

- review history
- findings history
- audit trail
- comparison between reviews
- user/project configuration

Do not introduce a database merely because one is available.

## Phase 16 — Multi-Agent Experiment — Optional

Only after the single-agent workflow is stable.

Possible agents:

```text
Coordinator
  |
  +-- Java quality reviewer
  +-- Security reviewer
  +-- Spring reviewer
  +-- Final synthesizer
```

Compare multi-agent quality/cost/latency against the simpler single-agent architecture.

## Learning Principle

For every phase:

```text
Concept
  ->
Architecture
  ->
Implement
  ->
Run
  ->
Observe
  ->
Understand
  ->
Experiment
  ->
Document
```

The project should remain understandable at every step.
