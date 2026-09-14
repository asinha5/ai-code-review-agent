# AI Code Review Agent

An asynchronous AI-assisted code reviewer for local source repositories. A Spring Boot backend gives Claude controlled repository tools, produces a structured review, streams progress to a React dashboard, and exports traces to a Langfuse-compatible OTLP endpoint.

## What is implemented

- Java 21, Spring Boot, Spring AI, and Anthropic Claude (`claude-haiku-4-5`)
- Server-owned review contexts that bind a UUID `reviewId` to a normalized local repository root
- AI tools to inspect a repository tree, list directories, read files, and search code
- Structured review summaries and findings with severity, category, file, line, evidence, recommendation, and confidence
- Asynchronous `PENDING` → `RUNNING` → `COMPLETED` or `FAILED` review execution
- Stored activity history, replayable Server-Sent Events, multiple subscribers, and emitter cleanup
- React + Vite UI with SSE activity, five-second polling fallback, elapsed time, result cards, failure handling, and reset
- Swagger/OpenAPI and observability through Actuator, Micrometer, OpenTelemetry OTLP, and Langfuse

## Architecture

```text
React + Vite -> CodeReviewController -> CodeReviewService -> AsyncReviewExecutor
                                                        -> Spring AI / Claude -> RepositoryTools
AsyncReviewExecutor + RepositoryTools -> activity publisher -> store + SSE -> React
AsyncReviewExecutor -> ReviewExecutionStore -> result endpoint -> React
AsyncReviewExecutor -> Micrometer / OpenTelemetry OTLP -> Langfuse
```

The detailed component relationships are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Project layout

```text
src/main/java/com/aicodereview/agent/  Spring Boot API, review, tools, activity, SSE, tracing
src/main/resources/application.yml     Runtime configuration
frontend/                              React + Vite dashboard
docs/                                  Architecture, class guide, learning notes, roadmap
```

## Run the backend

Set the required environment variables, then start Spring Boot from the repository root:

```powershell
$env:ANTHROPIC_API_KEY = "..."
$env:LANGFUSE_OTEL_ENDPOINT = "..."
$env:LANGFUSE_AUTH_HEADER = "..."
./mvnw.cmd spring-boot:run
```

The backend listens on `http://localhost:8080`. Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

## Run the frontend

In another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. During development, Vite proxies `/api` to `http://localhost:8080`.

## API overview

| Endpoint | Purpose |
| --- | --- |
| `POST /api/reviews` | Starts a review and returns `202 Accepted` with `reviewId` and `PENDING` status. |
| `GET /api/reviews/{reviewId}/result` | Reads the current execution status and final result when completed. |
| `GET /api/reviews/{reviewId}/activities` | Reads stored activity events. |
| `GET /api/reviews/{reviewId}/stream` | Opens a `review-activity` SSE stream, replaying stored events first. |

The browser starts a review, connects an `EventSource` using the returned ID, and fetches the final result on terminal activity. If the stream fails, it polls activities and result every five seconds until the review is terminal.

## Observability and data handling

The backend uses Spring Boot Actuator, Micrometer tracing, the OpenTelemetry bridge, and the OTLP exporter. `ReviewTracingService` creates an `ai-code-review` span tagged with `review.id`, repository name, terminal status, and finding count. Spring AI model observations can add model, token, and latency data where the provider and instrumentation supply it.

`ChatModelCompletionContentObservationFilter` maps Spring AI prompt and completion text to Langfuse observation input/output fields. This is sensitive: exported telemetry can contain AI prompts, model responses, source-code excerpts, and review findings or evidence. The current Spring AI configuration also enables prompt/completion logging. Use a trusted observability backend, restrict access and retention, and disable or minimize content observation outside controlled environments.

Never commit credentials. The configuration reads only these environment-variable names:

- `ANTHROPIC_API_KEY`
- `LANGFUSE_OTEL_ENDPOINT`
- `LANGFUSE_AUTH_HEADER`

## Current limits

Review contexts, execution records, and activity history are in memory and disappear on restart. The app does not yet review Git diffs, integrate with GitHub pull requests, use MCP, persist history, or coordinate multiple specialist agents.
