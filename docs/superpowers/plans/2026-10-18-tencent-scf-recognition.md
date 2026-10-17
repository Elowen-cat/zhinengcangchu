# Tencent SCF Recognition Migration Implementation Plan

> **For agentic workers:** Steps use checkbox syntax for tracking. This plan is being implemented in the current task; cloud publication requires the owner's Tencent Cloud account.

**Goal:** Move warehouse photo recognition from an unreachable `workers.dev` address to a Tencent Cloud SCF Function URL while keeping the DeepSeek key on the server.

**Architecture:** A Node.js 18 event function accepts the existing Android JSON contract at `/v1/recognize-photo` and `/v1/interpret-transcript`, validates the demo access token, calls DeepSeek, and returns the same `fields` JSON. The Android build reads its default HTTPS base URL from a build environment variable; the settings override remains available for migration without reinstalling.

**Tech Stack:** Tencent Cloud SCF event function, Node.js 18, DeepSeek Chat Completions, Android Kotlin/OkHttp.

---

## File map

- Create `tencent-scf/index.js`: standalone SCF `index.main_handler`, request validation, access check, bounded request size, DeepSeek call, response mapping.
- Create `tencent-scf/README.md`: exact console deployment, environment variables, Function URL setup, phone reachability check, cost controls and cutover.
- Modify `app/build.gradle.kts`: inject `WAREHOUSE_DEMO_BASE_URL` at build time instead of hardcoding `workers.dev`.
- Modify `app/src/main/java/com/youshu/app/data/warehouse/WorkerWarehouseRecognitionGateway.kt`: replace raw connection exceptions with actionable Chinese messages.
- Modify `app/src/main/java/com/youshu/app/ui/screen/settings/SettingsScreen.kt`: use a provider-neutral HTTPS placeholder.
- Modify `README.md` and `docs/warehouse-api-preparation.md`: record migration status and distinguish prepared code from a deployed working service.

## Chunk 1: Prepare the domestic service

- [x] Create a dependency-free Node.js 18 SCF event handler with the same two paths and `fields` response shape as the current Worker.
- [x] Keep `DEEPSEEK_API_KEY` and `APP_ACCESS_TOKEN` in SCF environment variables; never commit their values.
- [x] Reject missing credentials, oversized/invalid requests and unsupported methods before calling DeepSeek.
- [x] Document Tencent Cloud account, real-name verification, Function URL settings and phone reachability check.

## Chunk 2: Prepare Android cutover

- [x] Read default base URL from `WAREHOUSE_DEMO_BASE_URL`, using the current Cloudflare URL only as a compatibility fallback until the domestic endpoint is published.
- [x] Show a short Chinese message for DNS/connectivity failures and keep manual entry usable.
- [x] Update the settings placeholder and migration documentation.

## Chunk 3: Publish after the account exists

- [ ] The owner creates a Tencent Cloud account and completes the SCF setup; the DeepSeek key is entered only in Tencent Cloud environment variables.
- [ ] Deploy the SCF code and get the public Function URL; check `/health` on the actual phone with Wi-Fi and mobile data.
- [ ] Once reachable, build a new APK with the Function URL and the independent demo access token, install it without uninstalling the previous app, then verify photo recognition and manual inventory confirmation on the phone.
- [ ] Replace the download page APK only after the phone check succeeds.

No application tests are added or run in this task because the user requested a fix, not a test run. Real-phone service verification remains required before claiming the migration works.
