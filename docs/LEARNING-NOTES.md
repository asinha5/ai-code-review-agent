# Learning Notes

## Goal of This Project

The primary purpose of this project is to understand how an agentic application works when built from first principles with Java and Spring AI.

The focus is not simply "call an LLM and get a code review."

The focus is:

```text
How does an AI decide what information it needs,
call tools,
observe results,
continue reasoning,
and stop when it has enough evidence?
```

## 1. Agentic AI vs a Normal LLM Call

A normal request looks like:

```text
Prompt -> LLM -> Response
```

This project uses:

```text
Prompt
  |
  v
LLM
  |
  +--> getRepositoryTree
  |       |
  |       v
  |    observation
  |
  +--> readFile
  |       |
  |       v
  |    observation
  |
  +--> searchCode
  |       |
  |       v
  |    observation
  |
  v
Final structured answer
```

The model is selecting actions dynamically.

That is the core agentic behavior being learned.

## 2. Tool Calling

Spring AI exposes Java methods as tools using `@Tool`.

Example conceptually:

```java
@Tool
public String readFile(
        String reviewId,
        String relativePath) {
    ...
}
```

Spring AI provides the tool schema to the model.

The model does not execute Java code itself.

It returns a structured tool request, Spring AI executes the Java method, and the tool result is returned to the model.

## 3. Tool Calling Requires Model Support

An important lesson from development was that not every model behaves equally well with tool calling.

Some models may output something that looks like tool JSON as normal assistant text rather than returning a proper native tool call.

Spring AI needs proper tool-call metadata to execute tools automatically.

Therefore:

```text
"Model can generate JSON"
        !=
"Model reliably supports native tool calling"
```

## 4. Tool Contracts Matter

Adding detailed `@ToolParam` descriptions improved the tool contract.

Important instructions include:

```text
Use exactly the supplied reviewId.
Use repository-relative paths.
Do not invent or modify the reviewId.
```

A tool schema should be treated like an API contract for the model.

Ambiguous parameters increase model mistakes.

## 5. Server-Controlled Review Context

Passing absolute filesystem paths around through model calls is unnecessary and risky.

The project instead creates:

```text
reviewId -> repositoryRoot
```

The model uses:

```text
reviewId + relativePath
```

The application resolves the actual path.

This is a practical example of keeping security-sensitive state outside the model.

## 6. Coarse-Grained Tools Can Reduce Token Usage

Initially the model successfully explored:

```text
.
src
src/main
src/main/java
...
```

but each tool call adds another model/tool round trip and more context.

A better tool was introduced:

```text
getRepositoryTree
```

Now the agent can:

```text
getRepositoryTree
      |
      v
select important files
      |
      +--> readFile
      +--> searchCode
```

Lesson:

A good agent tool is not necessarily the smallest possible operation.

Tool granularity should balance:

- control
- context size
- latency
- number of round trips
- token consumption

## 7. Agent Prompt Design

The current review prompt gives the agent:

- role
- goal
- review ID
- tool-use guidance
- review categories
- evidence rules
- severity rules
- confidence rules
- stop condition
- output schema

The stop condition is particularly important.

Without it, an agent may continue exploring simply because more tools are available.

## 8. Evidence-Based Findings

The prompt explicitly tells the model:

```text
Do not invent issues.
Every finding must be supported by evidence discovered through tools.
Prefer fewer high-confidence findings.
```

This reduces speculative code review findings.

Future work should validate these findings programmatically where possible.

## 9. Structured Output

Free-form prose is difficult for an API/UI to consume.

The project converts model output into:

```text
CodeReviewResponse
```

with structured findings.

`BeanOutputConverter` helps communicate the expected schema.

A current practical fallback extracts the text between the first `{` and last `}` before conversion.

Important limitation:

```text
JSON extraction can remove surrounding prose.
It cannot repair incomplete or malformed JSON.
```

## 10. Output Token Limits Can Break Structured Responses

A model response can be correct in intent but still fail if it is truncated before the closing JSON braces.

This is why final-output size, token budgets, and the number of findings matter.

Current prompt controls include:

```text
Return at most 3 findings.
Keep each finding concise.
Keep the summary concise.
```

## 11. Rate Limits Are an Architecture Concern

During model testing, token-per-minute limits were reached even when individual prompts appeared reasonable.

Agentic flows amplify token use because:

```text
initial prompt
 + tool schemas
 + tool result
 + next model call
 + another tool result
 + ...
```

Therefore optimization includes:

- fewer tool calls
- smaller tool results
- targeted file reading
- capped search results
- concise prompts
- concise final output
- cheap/local deterministic tests

## 12. Review Activity Is Not Chain of Thought

The UI should not expose internal model reasoning.

Instead, it can expose observable actions:

```text
Inspecting repository structure
Reading SomeService.java
Searching code for exception handling
Processing review findings
```

These are actions the system actually performed.

This is why `ReviewActivityEvent` exists.

## 13. Logging and User Activity Are Different

SLF4J answers:

```text
What happened technically inside the application?
```

Review activity answers:

```text
What useful progress should the user see?
```

Langfuse will later answer:

```text
What happened during the AI trace?
How many tokens were used?
Which tools were called?
How long did each model invocation take?
```

Do not collapse all three into one mechanism.

## 14. Interface-Based Activity Design

Two interfaces were created:

```text
ReviewActivityPublisher
ReviewActivityStore
```

This avoids coupling the agent/tool code to the eventual delivery technology.

For example, `RepositoryTools` should not know anything about:

```text
SseEmitter
HTTP
React
```

It only publishes an activity.

This is a useful application of dependency inversion.

## 15. Why SSE Is Next

A code review can take many seconds while the model calls tools.

Without streaming:

```text
request
...
...
...
final response
```

the user sees no progress.

With Server-Sent Events:

```text
Review started
Inspecting repository
Reading file
Searching code
...
Review completed
```

can appear while work is happening.

SSE is a good fit because this use case mostly requires one-way server-to-browser progress events.

## 16. Cost-Aware Development

The project should not call a paid or rate-limited model merely to verify deterministic plumbing.

Examples that can be tested without an LLM:

- Maven compilation
- path validation
- repository traversal
- activity storage
- REST endpoints
- SSE connection lifecycle
- DTO serialization

Use a real model when validating:

- autonomous tool selection
- model/tool compatibility
- prompt behavior
- final finding quality

This makes debugging faster and cheaper.

## 17. Native Spring AI Tools Before MCP

The project deliberately starts with native Spring AI tools.

Reason:

First understand:

```text
tool schema
tool request
tool execution
tool result
agent loop
```

Then introduce MCP and compare:

```text
native tools vs MCP
```

This makes MCP a comprehensible protocol choice rather than a black box.

## 18. Current Mental Model

The application can currently be thought of as five layers:

```text
1. API
   receives review request

2. Review orchestration
   manages review lifecycle

3. AI agent
   decides actions

4. Tools
   provide controlled capabilities

5. Activity / observability
   exposes what the system is doing
```

The next layers will be:

```text
6. Streaming
7. UI
8. AI observability
9. Git awareness
10. MCP
```
