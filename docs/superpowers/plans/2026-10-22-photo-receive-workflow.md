# Photo receive workflow Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:plan-execution to implement this plan. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Implement the reference photo-to-multiple-drafts workflow, with editable product cards and explicit pending review before stock posting.

**Architecture:** Keep the existing one-product-per-photo service contract and recognize every selected photo independently. Add a dedicated inbound photo ViewModel and UI, reuse existing camera and gallery, and save all resulting drafts in one Room transaction. Existing voice/manual/outbound behavior stays on its current route. Each submitted row retains a stable ID and its own image, supplier and unit price. No database migration or provider credential changes.

**Tech Stack:** Kotlin, Compose Material 3, Hilt ViewModel, SavedStateHandle, coroutines, existing recognition gateway and Room repository.

**Plan base:** 2cc94e4.

## Chunk 1: Photo session and pending persistence

- [x] Add a serializable per-photo line model with stable IDs, editable fields, validation, recognition state and touched-field tracking. Recognition never invents missing data. Retry results do not overwrite manual edits.
- [x] Add a navigation-scoped ViewModel: independent per-photo jobs (bounded concurrency), deletion cancels/ignores late results, restored interrupted jobs become retryable, and per-item failures remain editable. Store URI/field metadata rather than bitmaps in saved state.
- [x] Consume camera return URIs once so deleted photos cannot reappear; copy gallery selections into app storage; cap sessions at 12 images and report rejected/read-failed images.
- [x] Add atomic savePendingBatch and holder.saveDrafts methods with stable unique IDs and protection against modifying confirmed documents. Preserve all fields and image associations; always add photo results as unreviewed pending drafts. No stock changes during save.
- [x] Freeze a complete immutable session snapshot during save: disable edits, import, delete, retry and back navigation; reject late recognition updates. Saving is allowed only after active recognition/import finishes. On failure unfreeze with the same IDs and fields intact.

**Files:** new `WarehousePhotoReceiveModels.kt`, `WarehousePhotoReceiveViewModel.kt`; modify `WarehouseRepository.kt`, `WarehouseUiState.kt`, `AppNavGraph.kt`.

## Chunk 2: Photo and pending screens

- [x] Add a reference-style banner, thumbnail strip with remove/continue camera controls, gallery selection, honest recognition counts/status, and a clear empty state.
- [x] Show one editable result card per photo, with thumbnail, product name, quantity/unit, batch, expiry calendar, location picker, retry/error handling and optional supplier/unit price. Mark fields that still need verification; no fabricated confidence scores.
- [x] Keep camera/gallery/navigation/rotation state; show progress and allow correction of individual failed photos. Capture one product per photo explicitly.
- [x] Initialize an existing draft as exactly one card with all original attachments, even for older multi-photo drafts. Its fields are treated as manually filled and never auto-recognized or split; only explicitly added new photos create new cards.
- [x] Add a persistent footer for continue camera and atomic add-to-pending; show validation without an initial empty-form error. Disable duplicate saves and retain results on failure. Existing photo drafts use the same polished editing view and retain original attachments.
- [x] Improve inbound pending cards with photos, field summaries and clear per-item review, while keeping edit/delete and final confirmation intact. Preview examples are explicitly marked and remain isolated.

**Files:** new `WarehousePhotoReceiveScreen.kt`, `WarehousePhotoReceiveComponents.kt`; modify `WarehouseTransactionScreens.kt`, `AppNavGraph.kt`. Add debug-only opt-in sample session support for viewing the reference-like multi-product screen in the emulator, without issuing fake recognition calls or seeding formal stock.

## Chunk 3: Delivery

- [x] Build and inspect the new empty/multiple-photo/edit/failure/pending paths in the emulator. Exercise the preview save/review/confirm chain, retaining real stock data. Use the prior user-authorized detailed verification workflow for focused model and regression checks.
- [x] Verify independent image-to-line association, edited-field protection, delete/retry behavior, validation and batch save semantics. Document any live-recognition limitation rather than presenting demo fields as real model output.
- [x] Save screenshots and a local APK, install the latest package in Android Studio, commit and push to the authorized main branch, verify remote hash, and leave the relevant photo page visible.

**Review:** Independent Codex plan and implementation review replaces the unavailable companion CLI, as recorded in prior plans.

## Review and delivery evidence

Independent review approved the plan after save-freezing and legacy-attachment corrections, then approved the implementation and protected demo build script. Detailed evidence is recorded in docs/photo-receive-verification-2026-10-22.md. The final delivery also restores the original Tencent demo configuration without publishing it; scripts/build-warehouse-demo.ps1 and AGENTS.md enforce configured demo builds. Camera controls now use the warehouse palette with recognition handoff wording.
