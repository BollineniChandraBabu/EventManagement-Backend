# 0010 — Gzip media at rest

- **Status:** Accepted
- **Date:** 2026-11-26 (recorded)
- **Deciders:** backend owner
- **Related:** [../../backend/caching-strategy.md](../../backend/caching-strategy.md), [../data-flow.md](../data-flow.md)

## Context

Storage and egress are billed and quota-limited. Images and PDFs are the largest objects the product
stores, and the same bytes are often served more than once (email image endpoint, chat download,
signed URL).

## Decision

* Compress uploaded media with GZIP before the S3 `PUT` when `app.media.gzip.enabled` is true
  (default true).
* Label the object `Content-Encoding: gzip` so browsers and presigned URLs decode it transparently.
* Expand on read inside the application when the bytes are used for email embedding or download
  responses.
* Make the behaviour switchable by property; `GzipUtil` is idempotent (compressing already-gzipped
  bytes is a no-op).

## Consequences

**Positive**

* Smaller stored objects and lower egress for text-like content and some images.
* No client changes: `Content-Encoding` handles transparent decoding.
* Safe to toggle: `GzipUtil` detects the magic number and does not double-compress.

**Negative**

* Already-compressed formats (JPEG, PNG, MP4) gain little; CPU is spent for minimal benefit.
* Readers outside the application must honour `Content-Encoding`.
* Two states can exist in the bucket if the flag changes; reads must always go through
  `GzipUtil.decompress` (which handles both).

**Follow-up**

* Skip compression for formats that are already compressed.
* Measure actual savings before enabling for video.

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Compress at the CDN only | Storage and quota cost remain |
| Store uncompressed | Simpler but larger and slower |
| Re-encode images (WebP/AVIF) | Lossy, needs an image pipeline |
