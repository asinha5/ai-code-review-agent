# AI Code Review Agent

An agentic AI learning project built with Java, Spring Boot, and Spring AI.

The application reviews a local source-code repository by allowing an LLM to autonomously inspect repository structure, read selected files, search code, and produce structured engineering findings.

The project is intentionally built incrementally to learn the mechanics behind agentic systems rather than hiding them behind a large agent framework.

## Current Goal

Given a local Java/Spring repository path, the application should:

1. Create a review session.
2. Give the AI access to safe repository tools.
3. Let the AI decide which tools to call and which files to inspect.
4. Generate concise, evidence-based findings.
5. Track the review activity by `reviewId`.
6. Later stream review activity live to a React UI using SSE.

## Current Technology Stack

- Java 21
- Spring Boot 3.5.x
- Spring AI 1.1.x
- Maven
- Groq through Spring AI's OpenAI-compatible integration for selected development tests
- React + Vite planned for the UI
- In-memory state for review context and activity tracking
- Langfuse planned for AI observability
- MCP planned after the native Spring AI tool-calling implementation is understood

## Current Architecture

```text
Client / Postman
      |
      v
CodeReviewController
      |
      v
CodeReviewService
      |
      +----> ReviewContextManager
      |          |
      |          +---- creates reviewId + repository root
      |
      +----> ReviewActivityPublisher
      |
      +----> Spring AI ChatClient
                   |
                   v
             LLM / Agent Loop
                   |
                   v
             RepositoryTools
               |   |   |
               |   |   +---- searchCode
               |   +-------- readFile
               +------------ getRepositoryTree / listFiles
                   |
                   v
          Local source repository
```

The agent loop is conceptually:

```text
decide -> call tool -> observe result -> decide -> call tool -> ... -> final response
```

## Main Packages

```text
com.aicodereview.agent
|
+-- api
|   +-- CodeReviewController
|   +-- CodeReviewRequest
|   +-- CodeReviewResultResponse
|
+-- review
|   +-- CodeReviewService
|   +-- CodeReviewFinding
|   +-- CodeReviewResponse
|   +-- CodeReviewResult
|   +-- ReviewContext
|   +-- ReviewContextManager
|   +-- ReviewActivityType
|   +-- ReviewActivityEvent
|   +-- ReviewActivityPublisher
|   +-- ReviewActivityStore
|   +-- DefaultReviewActivityPublisher
|
+-- tool
    +-- RepositoryTools
```

See `docs/PACKAGE-CLASS-GUIDE.md` for detailed responsibilities.

## Current REST APIs

### Start a review

```http
POST /api/reviews
Content-Type: application/json
```

Example request:

```json
{
  "repositoryPath": "C:\\Users\\ASUS\\OneDrive\\Desktop\\SelfLearningProjects\\Java\\some-project"
}
```

The response contract contains:

```json
{
  "reviewId": "generated-review-id",
  "summary": "Concise review summary",
  "findings": []
}
```

### Read review activities

```http
GET /api/reviews/{reviewId}/activities
```

Example activity:

```json
{
  "reviewId": "test-123",
  "type": "ANALYZING",
  "message": "Test activity event",
  "timestamp": "2026-09-10T11:52:49.936684500Z"
}
```

## Repository Tools

The model can currently use four repository tools.

### `getRepositoryTree`

Returns a recursive repository tree while excluding ignored directories.

The prompt instructs the agent to use this first so it can understand the repository in one call rather than walking directory-by-directory.

### `listFiles`

Lists entries in a particular directory.

This is intended for targeted follow-up inspection when the repository tree is not enough.

### `readFile`

Reads a repository file.

The model should use it only for files that appear relevant to the review.

### `searchCode`

Searches supported text files for a term and returns repository-relative file paths, line numbers, and matching lines.

Supported formats currently include:

- `.java`
- `.xml`
- `.yml`
- `.yaml`
- `.properties`
- `.md`
- `.json`

## Repository Safety

Repository tools do not accept arbitrary absolute paths from the model.

Each review has a server-created `reviewId`. The server maps that identifier to a normalized repository root.

Tool paths are resolved relative to that repository root:

```text
reviewId
   |
   v
ReviewContextManager
   |
   v
repositoryRoot.resolve(relativePath).normalize()
```

The resolved path must still start with the configured repository root. This prevents simple path traversal attempts such as:

```text
../../..
```

Ignored directories include:

```text
.git
.idea
.vscode
.mvn
target
node_modules
build
dist
```

## Structured Findings

Each finding contains:

- severity
- category
- file
- line, when known
- issue
- evidence
- recommendation
- confidence

The current prompt limits the model to at most three findings and asks for HIGH or MEDIUM confidence findings only.

## Review Activity Tracking

The application now records user-visible review activity such as:

```text
REVIEW_STARTED
ANALYZING
REPOSITORY_INSPECTION
FILE_READING
CODE_SEARCH
GENERATING_FINDINGS
REVIEW_COMPLETED
REVIEW_FAILED
```

These events are separate from normal application logs.

Current flow:

```text
CodeReviewService / RepositoryTools
              |
              v
     ReviewActivityPublisher
              |
              v
DefaultReviewActivityPublisher
              |
              +---- logs the event
              |
              +---- stores event in memory by reviewId
```

The next step is to stream these events to the frontend using Server-Sent Events (SSE).

## Build

```bash
mvn clean compile -DskipTests
```

## Current Development Strategy

The project intentionally separates cheap deterministic development from model-dependent testing.

Use local compilation and deterministic tests for:

- controllers
- review context
- path security
- activity storage
- SSE
- DTOs
- repository tools

Use a live LLM only for meaningful agent-loop checkpoints.

This keeps development cost low and makes failures easier to isolate.

## Current Status

Completed:

- Spring Boot project setup
- Spring AI integration
- Native Spring AI tool calling
- Secure repository context
- Repository tree inspection
- File reading and code searching
- Structured code review response
- Review ID returned through API
- Review activity model
- Tool-level activity publishing
- In-memory activity store
- REST endpoint to read activities

Next:

- SSE live activity streaming
- React + Vite UI
- Langfuse observability
- stronger validation and error handling
- prompt/token optimization
- optional prompt caching
- MCP implementation
- Git diff / pull request awareness
- optional persistence and multi-agent experimentation

See `docs/ROADMAP.md` for the planned progression.
