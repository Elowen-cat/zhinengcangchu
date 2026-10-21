# Receive home polish Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:plan-execution to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the inbound landing screen to match the supplied reference, publish both UI changes to GitHub, and update the emulator APK.

**Architecture:** Add a focused Compose inbound landing screen and native gradient vectors, using the existing source/pending navigation callbacks. Keep the outbound landing screen, form logic, recognition logic and stock confirmation semantics intact. Derive the pending badge from real inbound drafts and clearly label preview data in the warehouse banner.

**Tech Stack:** Kotlin, Jetpack Compose, existing Android debug build, ADB, Git.

**Plan base:** 4382e09.

## Chunk 1: Inbound landing page

**Files:**
- Modify: `app/src/main/java/com/youshu/app/ui/warehouse/WarehouseTransactionScreens.kt`
- Create: `app/src/main/java/com/youshu/app/ui/warehouse/WarehouseReceiveHomeScreen.kt`
- Create: `app/src/main/java/com/youshu/app/ui/warehouse/WarehouseReceiveIcons.kt`

- [ ] Route inbound landing content to the new screen while retaining the current callbacks.
- [ ] Build a rounded mint warehouse banner with store icon, current workspace label, manual review explanation and preview marker where applicable.
- [ ] Build three evenly sized process steps with icons and arrows: 录入、核对、入库. Allow text to wrap and the page to scroll on smaller screens.
- [ ] Build rounded green photo, blue voice and orange pending cards with large vector icons and clear chevrons. The badge always uses the actual inbound draft count; zero does not imply pending work.
- [ ] Keep a quieter manual entry button, accessible touch targets, and preserve the input/review/confirmation flow. Avoid implying that adding a photo alone posts stock.

## Delivery

- [ ] Build the debug APK using the existing demo configuration without printing the demo token. No public version bump or download-page replacement is required.
- [ ] Install with replacement on the current Android Studio emulator. Inspect the full page and each navigation entry; return without posting any stock operation. Save a screenshot and APK in a local sibling artifact folder.
- [ ] Commit this change, fetch the remote main branch, and push the previous inventory-detail commit together with the new inbound change to the user-authorized repository. Verify remote main matches local HEAD.
- [ ] Leave the inbound landing screen open and report build/install/push results accurately.

**Review:** As in the prior plan, the skill's local companion CLI is unavailable. Use an independent Codex agent review for this plan and implementation.
