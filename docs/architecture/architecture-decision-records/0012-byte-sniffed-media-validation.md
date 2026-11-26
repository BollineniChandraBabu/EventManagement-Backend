# 0012 — Validate uploads by content, not by name

- **Status:** Accepted
- **Date:** 2026-11-26 (recorded)
- **Deciders:** backend owner
- **Related:** [../../security/secure-coding-guidelines.md](../../security/secure-coding-guidelines.md), [../../backend/business-logic.md](../../backend/business-logic.md)

## Context

Uploads come from browsers that can lie about both the file name and the declared content type. A
renamed SVG or an executable served back as an image is a stored-XSS and content-spoofing risk.

## Decision

* Detect the media type from the leading bytes (`MediaContentTypes.detect`): PNG, JPEG, GIF, WebP, BMP,
  MP4, WebM, MOV, AVI, MKV, MPEG, 3GP, MP3, WAV, OGG, M4A, FLAC, PDF.
* Accept a declared content type only when it agrees with the detected type
  (`MediaValidation.validateCommon`).
* Enforce per-kind size limits: image 5 MB, audio 10 MB, PDF 20 MB, video 50 MB, chat attachment
  50 MB.
* Store the detected type and a matching extension; never reuse the client extension.
* Keep profile pictures image-only (`validateImage`).

## Consequences

**Positive**

* Renamed or mislabelled files are rejected before storage.
* Served media carries a truthful content type.
* Size limits protect memory and quotas.

**Negative**

* The supported-type list must be maintained; a new format needs a signature check.
* Some legitimate files with unusual containers are rejected.
* Byte sniffing is not malware scanning.

**Follow-up**

* Add a virus scan step if untrusted users ever upload.
* Consider `Content-Disposition: attachment` for non-image chat downloads (currently `inline`).

## Alternatives considered

| Alternative | Why rejected |
| --- | --- |
| Trust `Content-Type` | Trivially spoofed |
| Trust the file extension | Trivially spoofed |
| Third-party scanning service | Extra provider and latency; not required for a family app |
