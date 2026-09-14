# AI Code Review Agent

An agentic AI learning project that reviews a local source repository. A Spring AI `ChatClient` lets the model select native repository tools, gather code evidence, and return a small structured set of findings. The application also records review activity and exposes it through REST and live Server-Sent Events (SSE).

## Implemented capabilities

- Model-driven tool selection with native Spring AI tool calling
- Server-controlled review context identified by a generated `reviewId`
- Secure, repository-relative tree, listing, file-read, and code-search tools
- Evidence-based findings with severity and confidence classifications
- Structured conversion through `BeanOutputConverter`, preceded by defensive JSON extraction
- Thread-safe, in-memory activity storage by `reviewId`
- Stored activity retrieval and live SSE delivery, including replay, multiple subscribers, and terminal-event completion
- springdoc OpenAPI documentation and Swagger UI

Anthropic Claude, Groq through an OpenAI-compatible integration, and earlier Ollama/Qwen local-model experiments have been tested. Provider tool-calling behavior, free-tier constraints, and token limits differ, so the architecture does not assume one permanent provider.

## Technology

- Java 21
- Spring Boot 3.5.x
- Spring AI 1.1.x
- Maven
- Spring Web and `SseEmitter`
- springdoc OpenAPI / Swagger UI
- SLF4J logging

## Architecture

```text
Client -> CodeReviewController -> CodeReviewService -> ChatClient -> LLM
                                  |                    |
                                  |                    v
                                  |              RepositoryTools -> repository
                                  v
                          review context + activity
```

The service creates a review context, invokes the model with the repository tools, converts the final JSON to review DTOs, and returns `reviewId`, summary, and findings. Activities published by the service and tools are stored and pushed to any SSE subscribers.

## Packages

```text
com.aicodereview.agent
|-- api        HTTP request, response, activity, and streaming endpoints
|-- review     review orchestration, context, and structured review models
|-- activity   activity events, publishing, and in-memory storage
|-- streaming  activity subscription and SseEmitter lifecycle
|-- tool       repository capabilities exposed to the model
`-- config     OpenAPI configuration
```

See [Package and Class Guide](docs/PACKAGE-CLASS-GUIDE.md) and [Architecture](docs/ARCHITECTURE.md).

## REST API

### Start a review

```http
POST /api/reviews
Content-Type: application/json
```

Example request on Windows:

```json
{
  "repositoryPath": "C:\\work\\sample-project"
}
```

The response contains:

```json
{
  "reviewId": "generated-review-id",
  "summary": "Concise review summary",
  "findings": [
    {
      "severity": "HIGH",
      "category": "Correctness",
      "file": "src/main/java/example/Example.java",
      "line": 42,
      "issue": "Description of the issue",
      "evidence": "Observed code evidence",
      "recommendation": "Suggested change",
      "confidence": "HIGH"
    }
  ]
}
```

The prompt requests at most three meaningful findings and excludes findings without inspected evidence.

### Read stored activity

```http
GET /api/reviews/{reviewId}/activities
```

### Stream activity with SSE

```http
GET /api/reviews/{reviewId}/stream
Accept: text/event-stream
```

```powershell
curl.exe -N http://localhost:8080/api/reviews/{reviewId}/stream
```

On connection, the endpoint replays events already stored for the review and then streams new `ReviewActivityEvent` objects live. Multiple clients or browser tabs may subscribe to the same `reviewId`; `REVIEW_COMPLETED` or `REVIEW_FAILED` completes its emitters. This behavior has been exercised successfully with multiple concurrent `curl` subscribers.

## Build and run

```powershell
mvn clean compile -DskipTests
mvn spring-boot:run
```

Swagger UI is available while the application is running at:

```text
http://localhost:8080/swagger-ui.html
```

Provider credentials and model selection must be supplied through the application's environment/configuration; do not commit secrets.

## Current limitations

- Review contexts and activities are in memory and disappear on restart.
- Reviews currently target a supplied local repository path, not a Git diff or GitHub pull request.
- Model output conversion is best-effort; prose can be stripped around JSON, but truncated or malformed JSON cannot be repaired.
- Provider rate limits, token limits, and tool-calling support affect review behavior.
- There is no frontend, durable history, Langfuse tracing, or centralized exception mapping yet.

Implemented and planned work are separated in the [Roadmap](docs/ROADMAP.md).
