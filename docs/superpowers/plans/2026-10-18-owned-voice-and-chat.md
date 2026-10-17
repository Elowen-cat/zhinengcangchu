# Owned Voice and Chat Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:plan-execution to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let competition users use photo and voice features without entering service credentials, using only services owned by the developer.

**Architecture:** The Android build carries the demo service URL and a limited demo access token. Tencent SCF keeps the DeepSeek key server side, proxies bounded text/image requests, and uses a Tencent SCF execution role for Tencent ASR. Warehouse voice and agent voice both call the same ASR endpoint; the legacy backend remains disabled.

**Tech Stack:** Kotlin Android, Node.js 18 Tencent SCF, Tencent ASR SentenceRecognition, DeepSeek Chat Completions.

---

## Chunk 1: Server endpoints

### Task 1: Add bounded audio transcription

**Files:** `tencent-scf/index.js`, `tencent-scf/README.md`

- [x] Validate a short 16 kHz WAV body and authenticate with the demo token.
- [x] Sign Tencent ASR SentenceRecognition with SCF role credentials and return a plain transcript.
- [x] Document ASR activation, role permission, costs, and the need to verify on a real phone.

### Task 2: Add text/image chat proxy

**Files:** `tencent-scf/index.js`

- [x] Accept bounded DeepSeek chat requests, force a supported model and nonstreaming response, and keep the provider key server side.
- [x] Repackage `warehouse-recognition-scf.zip` with `index.js` at ZIP root.

## Chunk 2: Android voice paths

### Task 3: Migrate legacy client calls

**Files:** `app/src/main/java/com/youshu/app/data/ai/AiInferenceRepository.kt`, `app/src/main/java/com/youshu/app/data/agent/AgentClient.kt`, new owned-service client

- [x] Route agent chat and image calls to the owned SCF endpoint.
- [x] Route recorded agent WAV audio to Tencent ASR through SCF.
- [x] Preserve local inventory tools, using a complete response as a single UI update.

### Task 4: Migrate warehouse voice

**Files:** `app/src/main/java/com/youshu/app/ui/warehouse/WarehouseTransactionScreens.kt`, `WarehouseRecognitionViewModel.kt`

- [x] Capture a short WAV in the warehouse form and send it to the same ASR endpoint.
- [x] Keep the transcript editable and require human review before confirming inventory.

## Chunk 3: Release preparation

### Task 5: Owner-only setup and artifact

**Files:** `app/build.gradle.kts`, `README.md`, `tencent-scf/README.md`

- [x] Build a local 1.8.2 preview APK with the Tencent Function URL and demo token; do not publish it before phone checks.
- [ ] Keep service setup in owner settings; ordinary users receive a preconfigured build.
- [ ] Update the public download only after real-device verification.

No tests are added or run unless the user requests them. Static review and packaging inspection are allowed.
