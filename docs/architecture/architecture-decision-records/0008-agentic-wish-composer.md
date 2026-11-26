# 0008 — Agentic wish composer behind a flag with a validator tool

- **Status:** Accepted
- **Date:** 2026-11-26 (recorded)
- **Deciders:** backend owner
- **Related:** [../design-patterns.md](../design-patterns.md), [../system-design.md](../system-design.md)

## Context

The single-prompt generator occasionally produced placeholders (`[Name]`, `%s`), repeated the same
subject as last year, or ignored the relationship. Prompt wording alone could not guarantee quality,
and a bad message is sent to a real family member.

## Decision

* Introduce a generic tool-calling loop (`AgentRunner`) with a hard step cap, unknown-tool feedback and
  exception-to-error conversion.
* Implement `WishComposerAgent` with tools `get_recipient_profile`, `get_recent_wishes` and a terminal
  validator `submit_wish` that rejects placeholders and duplicate subjects; rejections are fed back
  so the model self-corrects.
* Wrap `AiServiceImpl` in `AgenticAiService` marked `@Primary` so every existing caller benefits, and
  fall back to the single-prompt path on any failure.
* Ship it behind `app.agent.wish.enabled` (default true in the current properties, documented as
  off-by-default historically) and cap steps with `app.agent.wish.max-steps`.

## Consequences

**Positive**

* Output must pass a deterministic validator before it is accepted.
* Recent-wish awareness reduces repetition.
* No caller changes; the fallback keeps availability.
* The same runner powers the delivery-ops and copilot agents.

**Negative**

* More tokens and latency per wish (several model turns).
* Prompt behaviour is harder to test than a single call; tests focus on the runner and the validator.
* Two code paths to maintain.

**Follow-up**

* Add a golden-set evaluation for message quality.
* Consider caching the recipient profile within a run.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Better prompt only | Cannot guarantee no placeholders or no duplicates |
| Post-hoc validation with regeneration | Simpler, but no access to recent wishes or profile context |
| Human review before send | Defeats automation |
