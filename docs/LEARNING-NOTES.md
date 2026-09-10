# Learning Notes

## Agent investigation

Tool availability does not guarantee a useful investigation. A model may stop early, read irrelevant files, or overstate conclusions. Prompts therefore need explicit evidence requirements, investigation guidance, and stopping rules.

The current review prompt applies these principles:

- Search when it helps narrow the repository.
- Read only files relevant to a potential finding.
- Do not infer behavior from filenames.
- Do not invent line numbers.
- Stop after sufficient evidence is collected.
- No evidence means no finding.

## Model and tool calling

Native tool calling is more important to this design than simply choosing a model marketed for code. The model must reliably select tools, provide valid arguments, incorporate observations, and continue investigating when necessary. Code knowledge alone does not create the agent loop.

## Repository paths

Absolute Windows paths caused escaping and tool-argument issues during development. The API accepts the repository root once, creates a `reviewId`, and repository tools subsequently operate with that identifier and repository-relative paths. This also creates a clear security boundary for path validation.

## Evidence and findings

A plausible concern is not a finding. Reported issues must be supported by code the model inspected. Severity should reflect demonstrated impact, confidence should reflect the strength of the evidence, and an unknown exact line should remain `null`.

## Structured output

Structured model output is best-effort. Even with `BeanOutputConverter` format instructions, a model may wrap JSON in explanatory prose. The current implementation extracts the outer JSON object before conversion. This is a pragmatic compatibility measure, not full schema validation or guaranteed recovery from malformed output.

## Development cost

Repeated Claude calls during development consume paid tokens and can slow iteration. Prefer local or free checks for compilation, path security, tool behavior, parsing, and controller logic. Use paid model calls at meaningful integration checkpoints where actual model and tool-call behavior must be verified.

## Current testing boundary

Postman supports manual endpoint and model-integration testing. The repository does not currently contain an automated test suite for repository tools, security boundaries, structured conversion, or orchestration behavior; these remain documentation and engineering gaps.

