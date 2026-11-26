# 0007 — Fallback across Gemini models instead of a fixed model

- **Status:** Accepted
- **Date:** 2026-11-26 (recorded)
- **Deciders:** backend owner
- **Related:** [../../backend/external-api-integrations.md](../../backend/external-api-integrations.md)

## Context

A single configured Gemini model failed in practice: rate limits (429), retired model names (404) and
transient 5xx errors stopped wish generation. Pinning model names in configuration also means code or
config changes whenever Google retires a model.

## Decision

* Keep a primary model in `GEMINI_API_URL`.
* Ask Google for the models that support `generateContent` (`GeminiModelCatalog`), cache the list for
  `GEMINI_FALLBACK_MODEL_CACHE_MINUTES` and filter out non-text models with a regex.
* On 429, 404, 5xx or a network error, mark the model unavailable for
  `GEMINI_FALLBACK_COOLDOWN_SECONDS` and try the next one, up to
  `GEMINI_FALLBACK_MAX_RETRIES` extra attempts.
* Do not retry 400/401/403; rethrow them.
* When nothing answers, raise `AiUnavailableException` → HTTP 503.

## Consequences

**Positive**

* Wish generation survives rate limits and model retirements without a deploy.
* New models are picked up automatically; retired ones disappear.
* Failures are visible (503 plus a clear message) instead of silent.

**Negative**

* More provider calls and higher latency during an outage.
* The catalogue call itself can fail; the previous list is kept for only one minute.
* Output quality varies between models.

**Follow-up**

* Add per-model metrics if quality differences matter.
* Consider a paid tier with a guaranteed quota.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Fixed model name in config | Breaks on retirement and rate limits |
| Retry the same model with backoff | Rate limits often last longer than a request timeout |
| Queue wishes for later | Adds a broker and delays the greeting past the day |
