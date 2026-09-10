# AI Code Review Agent

AI Code Review Agent is a Java 21 and Spring Boot service that uses Spring AI and Anthropic Claude to inspect a local source repository. The model autonomously selects repository tools, gathers code evidence, and returns a structured review.

## Implemented

- Anthropic Claude integration through Spring AI
- Review-scoped repository context identified by `reviewId`
- Repository boundary enforcement and path traversal protection
- Model-callable `listFiles`, `readFile`, and `searchCode` tools
- Filtering for ignored directories such as `.git`, `.idea`, `.vscode`, `.mvn`, `target`, `node_modules`, `build`, and `dist`
- Autonomous, iterative tool selection by the model
- Evidence-based review prompts with explicit stopping rules
- Structured `CodeReviewResponse` and `CodeReviewFinding` results
- Severity and confidence classification
- JSON extraction before `BeanOutputConverter` parsing
- Tool-call and raw-review logging
- Manual API testing with Postman

## Requirements

- Java 21
- An Anthropic API key

## Run locally

Set the API key:

```powershell
$env:ANTHROPIC_API_KEY="your-api-key"
```

Start the application:

```powershell
.\mvnw.cmd spring-boot:run
```

The service listens on `http://localhost:8080`.

## API endpoints

All endpoints are `GET` endpoints under `/api/ai`.

| Endpoint | Parameters | Purpose |
| --- | --- | --- |
| `/test` | None | Verifies the configured model connection with a simple prompt. |
| `/inspect` | `repositoryPath` | Inspects how `ChatClient` is configured and used in a repository; returns model text. |
| `/review` | `repositoryPath` | Performs an evidence-based Java/Spring review; returns `CodeReviewResponse`. |

Example:

```text
GET http://localhost:8080/api/ai/review?repositoryPath=C:\path\to\repository
```

For Postman, create a GET request, use the endpoint URL, and add `repositoryPath` as a query parameter. The repository path is accepted at the API boundary; repository tool calls use repository-relative paths.

## Review flow

```text
Request
→ ReviewContext creation
→ AI model decides which repository tools to call
→ Tool execution
→ Observation returned to model
→ Further tool calls as needed
→ Evidence-based findings
→ Structured CodeReviewResponse
```

The response contains a summary and a list of findings. Each finding can include severity, category, file, line, issue, evidence, recommendation, and confidence. A line number may be `null` when the exact line is unknown.

## Planned

The project does not currently include a React UI, SSE streaming, MCP integration, GitHub/PR integration, multi-agent review, RAG, or durable persistence. See [docs/ROADMAP.md](docs/ROADMAP.md) for planned work.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Learning notes](docs/LEARNING-NOTES.md)
- [Roadmap](docs/ROADMAP.md)

