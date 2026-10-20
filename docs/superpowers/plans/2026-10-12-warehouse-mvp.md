# Small-Store Warehouse MVP Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:plan-execution to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the existing Android inventory app into a reliable, demonstrable single-warehouse workflow for batch-aware receiving, issuing, traceable stock movements, and explainable replenishment suggestions.

**Architecture:** Keep Kotlin, Jetpack Compose, Room, Hilt, and the existing AI proxy. Add a warehouse domain alongside the legacy `Item` model, migrate active legacy records without deleting their source rows, and make posted movement records the only source of warehouse quantity changes. Recognition creates editable persisted drafts; deterministic validation and user confirmation perform postings.

**Tech Stack:** Kotlin 2.1, Jetpack Compose + Material 3, Room 2.6, Navigation Compose, Hilt, Coroutines, existing CameraX/audio recorder/AI proxy.

---

## Scope and product rules

- P0 is one warehouse, one device, local Room as the source of truth. A real multi-device workgroup needs authentication, a shared database, server-side authorization, concurrency control, and idempotent posting; the current `server/` only proxies AI/weather requests. Do not present collaboration as implemented until that backend exists and passes two-device tests.
- The shipped transaction loop is receive or issue → review persisted draft → revalidate → post one atomic movement and update batch balance. A pending draft never changes available stock.
- A batch owns quantity, expiry, and location. A product owns its name, category, base unit, photo, and reorder threshold. Require one base unit per product in P0; defer package conversion.
- Outbound reason and destination are recorded. `销售区` is a destination label only; this app does not track point-of-sale inventory or consumer sales.
- Replenishment calculations use eligible warehouse outbound movements, current stock, pending inbound quantity, and a visible target-cover/safety-stock rule. Do not label warehouse issues as retail sales. Exclude damage and corrections from demand estimates.
- AI may suggest product matches or parse voice into candidate fields. It may not choose an authoritative batch/expiry/location from an image, calculate authoritative stock, or post a transaction.
- Use one consistent demo dataset across every screen. Distinguish SKU counts, batch counts, document counts, and unit quantities. Anchor chart dates to the selected range and demo date.
- Defer market-price scraping, optimal retail pricing, full ERP/POS, multi-warehouse support, and configurable role builders.

## File map

Create the warehouse feature in focused packages:

- `app/src/main/java/com/youshu/app/data/local/entity/WarehouseProduct.kt` — warehouse SKU/product master.
- `app/src/main/java/com/youshu/app/data/local/entity/StockBatch.kt` — on-hand quantity, expiry, and location by batch.
- `app/src/main/java/com/youshu/app/data/local/entity/StockMovement.kt` — append-only posted movement facts.
- `app/src/main/java/com/youshu/app/data/local/entity/StockDocument.kt` and `StockDocumentLine.kt` — persisted draft/review/post lifecycle.
- `app/src/main/java/com/youshu/app/data/local/dao/WarehouseProductDao.kt`, `StockBatchDao.kt`, `StockMovementDao.kt`, and `StockDocumentDao.kt` — domain queries and atomic update primitives.
- `app/src/main/java/com/youshu/app/data/repository/WarehouseRepository.kt` — transaction boundary, stock checks, draft lifecycle, summaries.
- `app/src/main/java/com/youshu/app/data/warehouse/ReplenishmentCalculator.kt` and `WarehouseVoiceParser.kt` — pure rules that can be unit tested.
- `app/src/main/java/com/youshu/app/ui/screen/workbench/`, `inventory/`, `transaction/`, and `analytics/` — new warehouse screen boundaries.
- `app/src/main/java/com/youshu/app/ui/viewmodel/` — focused Workbench, Inventory, StockDraft, and ProductAnalysis view models.

Existing integration points to update are `data/local/database/AppDatabase.kt`, `di/DatabaseModule.kt`, `data/ai/AiInferenceRepository.kt`, `data/backup/AppBackupManager.kt`, `ui/navigation/Screen.kt`, `ui/navigation/AppNavGraph.kt`, `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/components/AppChrome.kt`, `ui/screen/home/HomeScreen.kt`, `ui/screen/search/SearchScreen.kt`, and `ui/screen/profile/ProfileScreen.kt`. Keep existing `Item`, `ItemDao`, and legacy screens until migration and backup parity are verified.

## Task 1: Establish a safe baseline and lock the demo story

**Files:** `README.md`, `docs/CONCEPT_BRIEF.md` (read-only source of scope), `docs/ui-mockups/` (read-only references).

- [ ] Create an implementation branch from the current base: `git switch -c codex/warehouse-mvp`.
- [ ] Configure the local Android SDK through an untracked `local.properties` or `ANDROID_HOME`; do not commit machine-specific SDK paths.
- [ ] Run `.\gradlew.bat :app:testDebugUnitTest` and ` .\gradlew.bat :app:assembleDebug`; record the baseline before feature edits.
- [ ] Freeze one demo date, one workspace name, 11 sample SKUs, 3 pending documents, and consistent batch/expiry/outbound values. Keep the existing milk example coherent: 24 bottles on hand, 8 safety stock, 4/day recent eligible outflow, 6 days cover, and a 12-bottle draft only when the declared target formula yields 12.
- [ ] Write the accepted routes and out-of-scope list into the plan or demo notes before expanding screenshots or copy.

**Verification:** The current checkout cannot resolve `:app:testDebugUnitTest` because no Android SDK location is configured. Once configured, the existing unit suite must pass before implementation proceeds.

## Task 2: Add batch-aware storage and a non-destructive migration

**Files:** Create the five warehouse entities and four DAOs listed in the file map; modify `data/local/database/AppDatabase.kt`, `di/DatabaseModule.kt`, and `app/build.gradle.kts` plus `gradle/libs.versions.toml` only if migration-test dependencies are needed. Add `app/src/androidTest/java/com/youshu/app/data/local/AppDatabaseMigrationTest.kt`.

- [ ] Define stable product, batch, document, document-line, and movement IDs; index foreign keys and lookup columns used by inventory and expiry queries.
- [ ] Add Room version 11 and `MIGRATION_10_11`. Copy each active legacy item into one warehouse product and one opening-stock batch, preserving name, category, unit, quantity, location path reference, expiry, price, notes, and image paths. Do not copy deleted items into active stock; retain every old row in the legacy table.
- [ ] Make migration repeat-safe by relying on Room versioning and unique legacy-source IDs. Never seed demo rows on every database open.
- [ ] Add an instrumented migration test that creates a version-10 database with representative active, expired, and deleted rows, migrates it, and checks preserved values plus no duplicate product/batch on reopen.
- [ ] Add a fresh-install seed path that is explicit and one-time; keep synthetic values labeled as demo data in the app/demo materials.

**Verification:** `:app:connectedDebugAndroidTest` passes the migration test on an emulator; existing unit tests still pass; a debug APK upgrades from version 10 without losing legacy data.

## Task 3: Make posting atomic and drafts safe

**Files:** Create `data/repository/WarehouseRepository.kt`; add pure validation/rules and tests under `app/src/test/java/com/youshu/app/data/warehouse/`; modify the warehouse DAOs as needed.

- [ ] Implement draft creation/update/removal without touching batch quantity or movement history.
- [ ] Implement inbound posting in one Room transaction: validate product, positive quantity, batch fields, and destination; update/create the batch; append one inbound movement; mark the document posted.
- [ ] Implement outbound posting in one Room transaction: reload current batch balance, reject zero/negative/insufficient stock, subtract quantity, append reason/destination movement, then mark posted.
- [ ] Add a unique idempotency key per document posting. Reconfirming the same document must return its prior result and must not insert a second movement.
- [ ] Preserve posted movements. Corrections use a linked reversal/adjustment movement rather than editing history.
- [ ] Test pending-does-not-change-stock, successful receive/issue, insufficient stock, invalid quantities, stale availability between review and post, repeated confirmation, and rollback when one database operation fails.

**Verification:** All warehouse repository tests pass; batch quantity equals opening balance plus posted inbound minus posted outbound plus adjustments; no pending or failed document changes on-hand stock.

## Task 4: Replace the top-level navigation and build the warehouse shell

**Files:** Modify `ui/navigation/Screen.kt`, `ui/navigation/AppNavGraph.kt`, `ui/theme/Color.kt`, `ui/theme/Theme.kt`, and `ui/components/AppChrome.kt`; create Workbench/Inventory screen and view-model files; adapt `HomeScreen.kt` and `SearchScreen.kt` only where reuse is clean.

- [ ] Change top-level routes and labels to 工作台 / 库存 / 分析 / 我的. Keep detail and transaction routes off the bottom bar and preserve top-level tab state.
- [ ] Replace the purple family palette with the mockup's muted teal, cool-white background, navy text, blue-gray secondary text, and accessible warning colors. Keep blur limited to small navigation/overlay surfaces.
- [ ] Build a vertically scrolling workbench with workspace label, expiring/low-stock/pending-receipt/today-outflow metrics, pending tasks, and receive/issue/count shortcuts.
- [ ] Build the inventory list with search and filters for all/low-stock/expiring/out-of-stock plus location and category. Each row shows SKU name, available quantity/unit, batch count, next expiry, and location.
- [ ] Build batch detail with collapsed/expanded batch states, FEFO priority text, batch quantity/expiry/location, and movement history.
- [ ] Use real repository flows, not hard-coded numbers. Ensure bottom navigation and sticky actions do not cover the last scroll item; include loading, empty, and error states.

**Verification:** Manual emulator smoke check can switch all four tabs, preserve filters/scroll on return, open product/batch detail, and reach the last row on a short device.

## Task 5: Complete receiving and outbound workflows before adding AI

**Files:** Create `ui/screen/transaction/ReceiveHomeScreen.kt`, `OutboundHomeScreen.kt`, `StockEntryReviewScreen.kt`, and `PendingDocumentsScreen.kt`; create/update `StockDraftViewModel.kt`; add their routes in `Screen.kt` and `AppNavGraph.kt`.

- [ ] Implement manual receiving as the first complete vertical slice: choose product, quantity/unit, batch, printed expiry, supplier/optional purchase cost, and warehouse location; save as a persisted pending draft.
- [ ] Implement manual outbound with batch, quantity, reason, and destination. `销售区` remains a destination label; do not create a sales-area stock balance.
- [ ] Implement a review queue with source method, editable lines, current availability, projected remaining stock, remove/edit actions, and one explicit final post action.
- [ ] Show fresh stock validation immediately before post. If stock changed, block posting and ask the user to edit the draft.
- [ ] Reopen the app with drafts still present. Posting success returns a document/movement summary and refreshes inventory, workbench, and analysis.
- [ ] Test inbound then outbound on the same batch, two batches with different expiry, damaged outbound excluded from demand, and draft survival across process recreation.

**Verification:** On one emulator, receive 24 bottles into a batch, issue 6 to `销售区`, and verify 18 remain with exactly one inbound and one outbound movement. No AI or network is required for this happy path.

## Task 6: Add photo/voice-assisted drafts and per-product replenishment

**Files:** Modify `data/ai/AiInferenceRepository.kt`; create `data/warehouse/WarehouseVoiceParser.kt` and `ReplenishmentCalculator.kt`; add tests under `app/src/test/java/com/youshu/app/data/warehouse/`; create `ui/screen/analytics/ReplenishmentOverviewScreen.kt`, `ProductAnalysisScreen.kt`, and corresponding view models.

- [ ] Reuse CameraX and the existing image transport to return candidate product name/specification only. Do not infer quantity from a package photo or silently accept AI-guessed expiry/location.
- [ ] Reuse `WavAudioRecorder` and `transcribeSpeech`; parse the transcript into editable product, quantity/unit, reason, and destination candidates. Missing or ambiguous fields stay visibly unresolved.
- [ ] Add camera/voice source tags to drafts. Both paths end at the same editable review form and pending-document queue.
- [ ] Implement pure replenishment rules using a selectable period, eligible posted outflow, available stock, pending inbound, safety stock, and target coverage. Return both the recommendation and a human-readable calculation trace.
- [ ] Build a summary screen with “建议补货 / 暂不补货” lists and SKU counts; tapping a product opens its own analysis with a period selector, per-SKU outflow chart, stock/safety/coverage metrics, recommendation quantity, and calculation basis.
- [ ] Show “样本不足” when history is too short; do not label warehouse outflow as consumer sales or claim price optimization. Display purchase-price comparisons only when dated supplier/cost records exist.
- [ ] Test parser ambiguity, low/no historical data, excluded damage, pending receipts, expiry risk, and recommendation calculations against hand-worked examples.

**Verification:** At least three sample SKUs produce explainable and reproducible calculations; photo and voice recognition failure still allow manual editing; neither path can post without the shared confirmation step.

## Task 7: Finish profile, backup compatibility, and competition demo

**Files:** Modify `ui/screen/profile/ProfileScreen.kt`, `data/backup/AppBackupManager.kt`, `app/src/main/res/values/strings.xml`, `README.md`, and any affected database/backup tests.

- [ ] Keep the profile page structure but use warehouse/workspace labels and counts backed by real local data. Do not claim cloud members/admin permissions until server identity and shared inventory exist.
- [ ] Extend backup export/import to include products, batches, movements, and pending drafts; support older backups by converting legacy item snapshots to opening batches.
- [ ] Update README and app copy to describe the warehouse prototype accurately and disclose the existing upstream project and the newly implemented work.
- [ ] Run unit tests, migration instrumentation tests, `:app:assembleDebug`, and install the APK on the target emulator/device.
- [ ] Manually exercise: fresh install → receive by manual/photo/voice → review → post → issue to sales area or mark damage → view batch history → view replenishment summary/detail → backup and restore.
- [ ] Capture final screenshots from the running APK and compare against mockups for clipping, text size, contrast, insets, loading/error/empty states, and button reachability.
- [ ] Freeze feature work at least one day before submission; fix only crashes, data correctness, and navigation blockers.

**Verification:** All automated tasks pass on an Android SDK-equipped environment; the APK launches and completes the core receive/issue/replenishment demonstration without network access except optional recognition.

## Suggested delivery order

| Slice | Target | Keep if time shrinks |
| --- | --- | --- |
| Branch/baseline + warehouse data/migration | First 2 days | Mandatory |
| Atomic manual receive/issue + persisted confirmation | Next 2–3 days | Mandatory; this is the core trust story |
| Workbench, inventory, batch detail, teal theme | Next 1–2 days | Mandatory but simplify charts/decoration |
| Photo/voice candidate parsing | Next 1–2 days | Keep at least one robust multimodal path; both must remain review-first |
| Replenishment overview/detail | Next 1 day | Keep transparent deterministic rules; skip unsupported price claims |
| Multi-device group backend, roles, price scenarios | Separate follow-up | Defer unless the shared backend can be completed and tested on two devices |
| Migration, backup, APK smoke, demo data and story | Final 1–2 days | Mandatory; stop adding features |

**Current environment note:** The repo is on `main` with user-created untracked `docs/CONCEPT_BRIEF.md` and `docs/ui-mockups/`; preserve these. At review time the Gradle baseline is blocked because this machine has no configured Android SDK. Do not interpret that as an application test failure.

<!-- PLAN_BASE_SHA: 3d24b8ac4777a07a9c4b81b3a1dc5a252cde097c -->
