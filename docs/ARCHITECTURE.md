# Architecture

## Scope

The current application is a synchronous Spring Boot REST service for reviewing a local repository with Anthropic Claude through Spring AI. Repository access is exposed to the model through a constrained tool layer.

## Implemented components

| Component | Responsibility |
| --- | --- |
| `AiConnectionController` | Exposes test, inspection, and structured review endpoints; builds prompts; registers tools; extracts JSON; converts the result. |
| `ReviewContextManager` | Validates a repository directory, creates a UUID `reviewId`, and keeps the review-to-root mapping in memory. |
| `ReviewContext` | Associates a `reviewId` with its normalized, absolute repository root. |
| `RepositoryTools` | Provides `listFiles`, `readFile`, and `searchCode` to the model and logs their execution. |
| `CodeReviewResponse` | Defines the structured summary and findings collection. |
| `CodeReviewFinding` | Defines severity, category, location, issue, evidence, recommendation, and confidence fields. |

## Review execution flow

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

Spring AI's `ToolCallAdvisor` and `ToolCallingManager` support the iterative tool-call cycle. The application makes the tools available, while the model decides which tools to invoke and when it has sufficient evidence to stop.

## Repository access boundary

An incoming `repositoryPath` is converted to a normalized absolute path and validated as an existing directory. The resulting root is registered under a generated `reviewId`.

Every path-based tool call supplies that `reviewId` and a repository-relative path. `RepositoryTools` resolves and normalizes the path against the registered root, then rejects any result that does not remain beneath that root. This prevents `..` path traversal from escaping the selected repository.

`listFiles` omits configured ignored directories from directory results. `searchCode` recursively excludes ignored paths and searches selected text formats: Java, XML, YAML, properties, Markdown, and JSON. Search results are capped at 100 matches. `readFile` reads a specified regular text file.

Ignored directories are:

```text
.git, .idea, .vscode, .mvn, target, node_modules, build, dist
```

## Review contract

The review prompt requires inspected evidence, prohibits invented line numbers, excludes weak findings, and defines severity and confidence criteria. Findings with low confidence are not requested. The structured contract is:

```text
CodeReviewResponse
├── summary
└── findings[]
    ├── severity
    ├── category
    ├── file
    ├── line
    ├── issue
    ├── evidence
    ├── recommendation
    └── confidence
```

`BeanOutputConverter` supplies the model's format instructions and converts its result. Because model output can include prose around the object, the controller first extracts the substring from the first `{` through the last `}`. Conversion remains best-effort and invalid or absent JSON raises an error.

## Operational characteristics

- Tool calls and the raw review response are logged.
- Review contexts live only in the application process and have no cleanup lifecycle.
- Requests and responses are synchronous.
- Postman is the current manual API testing workflow.

## Planned architecture

SSE streaming, a React/Vite client, MCP, Git diff or PR review, orchestration enhancements, and durable persistence are not implemented. Planned work is tracked in [ROADMAP.md](ROADMAP.md).

