# Inventory detail polish Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:plan-execution to implement this plan. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Match the reference action tiles, keep the batch total visible when collapsed, and show illustrative stock movements and supplier pricing in the isolated preview.

**Architecture:** Reuse Compose vector illustrations and existing navigation callbacks. Keep sample ledger data in the preview state, and enrich real movement presentation from confirmed documents without changing Room schemas. Preserve quantities, expiry exclusion, batch ordering, and manual confirmation behavior.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, existing Room repository flows, Android emulator.

**Plan base:** f26172ba706a73448a2d87a851f709d13e5ca4a7.

## Chunk 1: Detail actions, batch summary and sample ledger

### Task 1: Action tile and batch panel styling

**Files:**
- Modify: `app/src/main/java/com/youshu/app/ui/warehouse/WarehouseDashboardIcons.kt`
- Modify: `app/src/main/java/com/youshu/app/ui/warehouse/WarehouseInventoryScreens.kt`
- Create: `app/src/main/java/com/youshu/app/ui/warehouse/WarehouseInventoryDetailComponents.kt`

- [x] Add a purple opposing-arrow Transfer vector using the established 48 dp vector system.
- [x] Replace the three solid buttons with a shared rounded white panel containing equal-width green/blue/purple icon tiles. Retain callbacks and disable unavailable outbound/transfer with accessibility semantics.
- [x] Group batch header, batch rows and total in one coherent panel. Collapsed view shows the first batch and total; expanded view shows all and the same total. Keep a header expansion toggle so batch details remain accessible for a single batch.
- [x] Render stock movements in descending time with direction icon, quantity, batch, note, timestamp and optional supplier/unit purchase price. Keep long text wrapping and offer expand/collapse for more than five movements.

### Task 2: Preview data and real purchase metadata

**Files:**
- Modify: `app/src/main/java/com/youshu/app/ui/warehouse/WarehouseUiState.kt`
- Create: `app/src/main/java/com/youshu/app/ui/warehouse/WarehousePreviewMovements.kt`
- Modify: `app/src/test/java/com/youshu/app/ui/warehouse/WarehousePreviewSimulationTest.kt`

- [x] Add defaulted supplier and purchasePrice fields to WarehouseMovementUi. Map them from the movement's exact confirmed document, rather than borrowing data from another batch or product. No database migration.
- [x] Seed preview-only stock movements whose inbound/outbound/count balances equal current stock and whose transfers do not change totals. Use fixed example vendor names and per-unit prices, relative dates, several movement kinds, and explicit preview labeling.
- [x] Keep preview reset deterministic; add simulated inbound supplier/price to the new movement. Retain real stock isolation and manual review requirements.
- [x] Adjust preview simulation assertions to compare against the seeded ledger baseline, and cover ledger balances and metadata.

### Task 3: Build and present in emulator

- [x] Build the debug APK and run the existing direct JUnit suite, retaining the current demo configuration without printing secrets.
- [x] Install with `adb install -r` on the current emulator, launch the debug preview and open milk detail.
- [x] Inspect collapsed/expanded batch totals, the three buttons, movement types, prices and timestamps. Capture review screenshots locally. Leave the relevant screen open for the user.
- [x] Report the exact scope and any verification limitation; no version bump or public release is required for this UI preview.

**Review:** The local Codex companion CLI named by the skill is unavailable. Use an independent Codex agent to review this single chunk and the full plan before implementation.

**Review result:** Independent Codex review approved both the single chunk and full plan. Keep per-batch ledger balances, exact document provenance, totals in all expansion states and true enabled semantics.

## Completion evidence

- Debug build succeeded; direct JUnit suite passed 84 tests.
- Emulator inspection confirmed collapsed and expanded totals both show 24 bottles; expanded batches show 8, 6 and 10.
- Inbound, outbound and transfer tiles opened the correct product forms; no confirmation was posted during this check.
- Preview movement list expands to six milk records, with sample supplier and CNY 2.60 per bottle on inbound records.
- Latest APK installed with replacement; preview remains on milk inventory detail. Formal inventory data was not seeded or cleared.
- Screenshots and APK: sibling workspace folder 库存详情UI调整.

