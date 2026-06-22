# Boost for Reddit test strategy

This document defines the local Boost test layout and prevents accidental overwriting of working app/data states.

## Installed package roles

| Package | Role | Rule |
| --- | --- | --- |
| `com.rubenmayayo.reddit` | Normal user app | Do not touch unless explicitly approved. |
| `com.rubenmayayo.reddit.dev` | Active DEV lane | All Boost development/runtime testing happens here. Before overwriting it, ensure a known-good rollback APK is archived. |
| Any other `com.rubenmayayo.reddit.*` package | Avoid | Do not create extra app variants unless there is a specific, explicit reason. |

## DEV lane and known-good auth baseline

The active development package is `com.rubenmayayo.reddit.dev`.

The current known-good rollback baseline is:

- Package: `com.rubenmayayo.reddit.dev`
- Label: `Boost DEV`
- Version name: `1.12.12`
- Version code: `210011212`
- Target SDK: `32`
- Status: logout/login verified
- Purpose: clean Patcheddit-compatible Boost DEV auth baseline

Known marker state:

Present:

- Modify login WebView
- RedReader user-agent
- `redreader://rr_oauth_redir`
- Morphe runtime/extension markers

Not present:

- SDK35 target
- `POST_NOTIFICATIONS`
- inline Giphy patch
- static image viewer patch

The known-good rollback APK is archived locally under:

- `local-artifacts/apk-archive/*-working-dev-auth-baseline/`

`local-artifacts/` is intentionally ignored by Git.

## Current full SDK35 candidate

The current full candidate is APK-only unless explicitly installed.

Expected state:

- Package: `com.rubenmayayo.reddit`
- Label: `Boost`
- Target SDK: `35`
- `POST_NOTIFICATIONS`: present
- Installed by default: no

It should be archived under:

- `local-artifacts/apk-archive/*-current-sdk35-full-apk-only/`

This candidate is useful for static validation and release-gate input, but it is not runtime-validated until installed and tested.

## Runtime test policy

Runtime testing happens in the DEV lane: `com.rubenmayayo.reddit.dev`.

Do not silently create a growing set of app variants.

Allowed strategies:

### Strategy A: APK-only validation

Use this when checking build integrity, metadata, markers, target SDK, permissions, MPP contents, release-gate state, and archive state.

This does not prove runtime behavior.

### Strategy B: DEV-lane runtime testing

Use `com.rubenmayayo.reddit.dev` for runtime testing.

Rules:

- Confirm the normal package still exists before install.
- Confirm a known-good DEV rollback APK exists in `local-artifacts/apk-archive/`.
- Build the candidate from clean Boost.
- Rewrite the candidate package to `com.rubenmayayo.reddit.dev`.
- Preserve the launcher label `Boost DEV`.
- Sign with the DEV test keystore so `adb install -r` can preserve app data.
- Install over `com.rubenmayayo.reddit.dev`.
- Test login, media routing, downloads, notifications, Crashlytics noise, and logcat.
- Archive the tested APK and logs.
- If the candidate fails, reinstall the known-good rollback APK from the APK archive.

### Strategy C: normal package replacement

Only use this when explicitly approved and the risk to the normal Boost install/data is accepted, or when signing/Manager flow guarantees a safe update path.

This is not the default strategy.

## Release validation minimum

Before any release:

- Build MPP with `./gradlew :patches:buildAndroid`.
- Verify MPP contains `classes.dex`.
- Verify MPP contains `extensions/boostforreddit.mpe`.
- Verify Manager metadata is concise and current-release only.
- Verify README SHA matches MPP.
- Verify no APK/build artifacts are staged.
- Verify required marker strings.
- Verify old release values are gone.
- Verify remote release asset SHA and contents after publish.

For Boost target SDK changes, runtime validation must include:

- installed target SDK
- requested permissions
- `POST_NOTIFICATIONS`
- appops notification state
- notification channels
- actual download notification visibility
- relevant logcat

For Crashlytics changes, runtime validation must include:

- no `CrashlyticsRegistrar` marker
- no FirebaseCrashlytics startup network noise
- no firebase-settings Crashlytics spam
- app still launches normally
- FirebaseApp/FirebaseInitProvider/Analytics may remain alive

## Current chosen strategy

Default strategy from this point:

- Keep `com.rubenmayayo.reddit` untouched.
- Use `com.rubenmayayo.reddit.dev` as the active DEV lane.
- Build candidates APK-only first.
- Archive candidates locally.
- Runtime-test by installing over `com.rubenmayayo.reddit.dev`.
- Before overwriting DEV, verify that a known-good rollback APK exists.
- Do not create extra app variants by default.
