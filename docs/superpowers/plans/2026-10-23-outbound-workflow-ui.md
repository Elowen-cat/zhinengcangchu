# Outbound Workflow UI Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:plan-execution to implement this plan. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Refine the outbound home, photo, voice and pending screens to references 9–12 while keeping draft review and atomic stock deduction.

**Architecture:** Dedicated outbound composables reuse the current recognition ViewModel and repository callbacks. Shared pure stock calculations account for every pending draft by product and batch, exclude expired batches, and separate batch remainder from total product remainder. Debug-only sample queues allow repeatable visual inspection without Room writes.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Room, JUnit, Android emulator/ADB.

**PLAN_BASE_SHA:** 6bd5181e1502e7e4073d61c538d299feca198534

The companion review CLI and plan-execution skill are unavailable in this session. Use the available independent Codex reviewer for required plan/code review, then execute directly with focused delegated UI work. Existing user authorization covers implementation, tests, GitHub main push, build and emulator replacement installation.

## Chunk 1: Four-screen workflow

### Task 1: Shared stock rules and presentation
**Files:** Create `app/src/main/java/com/youshu/app/ui/warehouse/WarehouseOutboundModels.kt`, `WarehouseOutboundComponents.kt`; test `app/src/test/java/com/youshu/app/ui/warehouse/WarehouseOutboundModelsTest.kt`.
- [x] Implement pure `outboundStock(state, productId, batchId, quantity, excludingDraftId)` returning batch free, batch remainder, total product remainder and validation error. Use Long sums; only OUTBOUND reservations count; exclude the edited draft. Require positive quantity, valid product/available batch, sum <= batch stock. Aggregate pending validation rejects nonpositive quantities and empty destination/reason as well.
- [x] Test shared batch overbooking, different batches, edited-draft exclusion, expired/zero-stock batches, invalid quantities and large sums. Example: stock 8 in chosen batch, product 24, another pending 2 in batch, current 6 => batch remainder 0, product remainder 16. Never label product stock as batch stock.
- [x] Add shared rounded bordered white panels, mint/blue notices, source badges, image thumbnail (actual draft photo or honest stock icon), centered header, quantity stepper. No invented recognition confidence.

### Task 2: Home and pending
**Files:** Create `WarehouseOutboundHomeScreen.kt`, `WarehouseOutboundPendingScreen.kt`; modify outbound dispatch in `WarehouseTransactionScreens.kt` and callbacks in `AppNavGraph.kt`.
- [x] Home: mint hero, camera/voice/pending entry cards, real recent pending records with sources and destinations, manual entry, draft-only notice. Keep actual counts and meaningful empty state.
- [x] Pending: aggregate stock validation banner, source-tagged cards, review checkbox, edit/delete, quantity stepper, batch/reason/destination, batch and product remainder. Quantity change persists with `reviewed = false` through onSaveDraft callback, not review-only callback. Changes remain blocked during pendingDraftWrite/confirmation.
- [x] Select-all atomically reviews current complete queue through existing reviewDrafts. Existing repository confirms ALL outbound pending; all rows must be reviewed and valid. Do not imply selected-subset posting. Confirmation dialog rechecks current validation and busy/error state before submission. Keep reload recovery and confirmed result feedback.

### Task 3: Photo / voice / manual entry
**Files:** Create `WarehouseOutboundEntryScreen.kt`; modify `AppNavGraph.kt` routing.
- [x] Photo: actual photo panel, retake/gallery actions, auto recognition, product selection with honest match status, batch selector and three-column facts. Keep photo result provenance, loading/error/retry and manual fallback.
- [x] Voice: microphone/record-again, editable transcript and explicit reanalysis, automatic interpretation after ASR, same product and quantity form. Keep permission and audio duration/size constraints, release recorder on disposal.
- [x] Shared form: plus/minus editable positive quantity, reason chips, destination field, batch remainder and product remainder. Do not guess quantity from photo count or initialize unrecognized photo/voice as 1. Manual entry may start at 1.
- [x] Automatic results never overwrite user-edited product/batch/quantity/destination. Use touched flags and result IDs/source matching; exclude stale requests after starting next item. Existing draft edits do not auto-recognize attachments.
- [x] Primary action saves unreviewed draft then navigates pending only on success. Next-item action saves valid draft first then resets on success; generate fresh ID; reset consumed photo/transcript/recognition result IDs and clear navigation photo handle. Save failures retain form. Do not let async results mutate a save snapshot.

### Task 4: Verification and delivery
**Files:** Modify debug `WarehouseUiPreviewActivity.kt`; create `PendingOutboundPreviewSamples.kt`; extend `AppNavGraph.kt` preview route parameter; update tests and evidence notes.
- [x] Seed photo/voice/photo outbound drafts only in opt-in isolated debug preview; show source pictures from existing debug resources where available. Navigate outbound home; inspect all four pages via real edit routes.
- [x] Build and run unit tests with `& .\scripts\build-warehouse-demo.ps1 -Tasks @(':app:assembleDebug', ':app:runDebugUnitTestsDirect')`. Fix failures and inspect every result.
- [x] Replace-install with `adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk`. Inspect home, photo, voice and pending screenshots, invalid quantity/overbooking, edit resetting review, review-all, confirmation and deducted stock/ledger in isolated preview. Never fabricate live provider verification if no actual request succeeds.
- [x] Review full code diff independently, resolve findings, rerun affected verification. Save APK and evidence in parent `出库链路调整` directory.
- [x] Commit task changes, fetch origin main, verify ancestor, push `HEAD:main` without force, verify remote SHA. Open updated outbound page in emulator; report actual test/build/install/sync results.
