# Unified Pending Inbound Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:plan-execution to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Match reference 6 with one pending inbound queue containing photo, voice and manual drafts, explicit source badges, review progress and guarded confirmation.

**Architecture:** Keep the existing direction-filtered draft state and repository transaction. Route inbound pending to a focused Compose screen; leave outbound on its existing screen. Add debug-only opt-in mixed samples to the isolated preview, never the formal database. Reuse the existing edit, review, delete and confirm callbacks so edit invalidates review and source is preserved.

**Tech Stack:** Kotlin, Jetpack Compose Material3, existing WarehouseUiState/Room, ADB emulator.

**PLAN_BASE_SHA:** fd57f92f1cfb6cba7c05c7d5796fffdb1b3ed05f

## Chunk 1: Pending inbound presentation and delivery

### Task 1: Screen and cards

**Files:**
- Create `app/src/main/java/com/youshu/app/ui/warehouse/WarehouseInboundPendingScreen.kt`.
- Modify `app/src/main/java/com/youshu/app/ui/warehouse/WarehouseTransactionScreens.kt` to dispatch inbound to the new screen and preserve outbound behavior.
- Remove the superseded `InboundPendingCard` in `WarehousePhotoReceiveComponents.kt`; relocate its replacement into the new screen file.

- [ ] Build the reference-style warning banner, count summary (`待确认 N 项 · 已核对 M 项 · 还需核对 N-M 项`) and zero-safe review progress. Show preview labels explicitly. Empty queue must display an inviting empty state with continuation enabled, confirmation disabled.
- [ ] Use each draft's immutable `EntrySource` to show `图片录入`/`语音录入`/`手动录入` with camera/microphone/edit icons. Use a photo thumbnail where supplied, otherwise a source-appropriate placeholder. Show quantity/unit, batch, optional expiry, location and optional supplier/price without fabricating missing data.
- [ ] Put review checkbox at the card's upper right, a labeled edit button beside name/details, and an accessible overflow delete action. Unreviewed expiry is orange with a `请核对` indicator; reviewed expiry returns to normal, but past/invalid dates remain warned. All text must wrap on narrow screens.
- [ ] Keep individual checkboxes, omit bulk mark-all to emphasize actual per-item review. Bottom bar has `继续录入` and disabled `全部核对后确认入库` until all reviewed. After review use `确认入库（N项）` (preview: `模拟确认入库（N项）`). Retain a final confirmation dialog and repository validation/error reporting. Disable mutation during submission and recheck eligibility before calling onConfirm. Continue goes to the receive home for photo/voice/manual choice.

### Task 2: Preview and navigation

**Files:**
- Modify `app/src/main/java/com/youshu/app/ui/navigation/AppNavGraph.kt` with an optional empty `pendingPreviewDrafts` parameter. Seed only when `uiPreview` is true and once per saved preview session, after enablePreview; navigate to inbound pending.
- Modify `app/src/debug/java/com/youshu/app/ui/warehouse/WarehouseUiPreviewActivity.kt` with `--ez pending_preview true` option.
- Create `app/src/debug/java/com/youshu/app/ui/warehouse/PendingInboundPreviewSamples.kt` with two photo drafts and one voice draft, two reviewed/one unreviewed. Use existing debug drawables and matching preview batch date/location, and explicit example descriptions. Formal app launches remain empty unless actual drafts exist.

- [ ] Implement samples and continuation without changing existing photo-preview behavior. Use stable sample IDs and existing preview products so confirmation is valid and inventory totals can be checked.

### Task 3: Verify and deliver

- [ ] Run `scripts/build-warehouse-demo.ps1 -Tasks @(':app:assembleDebug', ':app:runDebugUnitTestsDirect')` using protected demo configuration, never print secrets. Existing user authorization covers full-flow verification.
- [ ] Replace-install with `adb install -r` on emulator-5554. Check mixed source labels, initial 3/2/1 and progress, disabled confirm, each review toggle, edit preserving source/resetting reviewed, deletion counts, date warnings, empty state and continuation. Confirm isolated sample queue and inspect stock/movements; no formal data writes.
- [ ] Capture final screenshots and save actual verification notes in `docs/pending-inbound-verification-2026-10-22.md`.
- [ ] Commit the related files, fetch origin main, require main to be an ancestor, push HEAD:main and compare remote SHA. Copy final APK to a named artifact folder and reopen opt-in mixed preview for user viewing.

**Review:** The skill's companion CLI and plan-execution skill are unavailable locally. Use an independent Codex subagent for mandatory plan approval and final change review, execute the approved plan directly. Existing user authorization covers implementation, testing, push and emulator installation; no extra approval pause is required.
