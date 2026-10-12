# Changelog

## 1.0.0 (unreleased)

First release as `com.spreedly:composer-sdk-server`. If you're upgrading from `com.dodgeballhq.protect:protect-core` 1.2.0, read **Changed** and **Removed**.

### Added
- `setRequestTimeoutMs` on the builder: a timeout for each HTTP request, default 30000 ms. A value of `0` or less is logged and replaced with the default.
- `setLogLevel` on the builder (`TRACE`, `INFO`, `ERROR`, `NONE`; default `INFO`). The SDK writes JSON log lines to stdout, and the level is shared by every `Composer` in the process. At `TRACE`, `ip`, `data`, `stepData` and `nextSteps` are redacted.
- `USAGE_LIMIT` and `API_LIMIT` outcomes. Both come back with `success` `false`, so `hasError` is `true`.
- `Composer.isTimeout(response)`.
- `event()` logs an error when an event isn't sent or the API doesn't accept it.

### Changed
- Maven coordinates are `com.spreedly:composer-sdk-server`, the package is `com.spreedly.composer`, and the main class is `Composer`. `com.spreedly.composer.Dodgeball` still works for `builder()`, `checkpoint`, `event`, `notify` and `track`; call the static helpers on `Composer`.
- Renamed classes: `DodgeballApiError` is `ComposerApiError`, `DodgeballVerification` is `ComposerVerification`, `CheckpointRequest.Options` is `CheckpointResponseOptions`, and `DodgeballServices` is `ComposerServices`. `ApiEvent` is in `com.spreedly.composer.api`.
- Builder methods: `setDbUrl` is `setApiUrl` and `enable(boolean)` is `setEnabled(boolean)`. `build()` throws `ComposerMissingConfigException`, an `IllegalStateException`, when the API key is missing.
- Requires Java 11 or later. The license is Apache 2.0 (was MIT).
- Renamed request fields: `sessionExternalId` is `sessionId`, `customerExternalId` is `userId`, and `priorCheckpointId` is `useVerificationId`. The `CheckpointRequest` constructor order is now `(event, checkpointName, sessionId, userId, sourceToken, useVerificationId)`, so positional calls still compile with the wrong values; check every call site.
- `NotificationRequest` is `(ApiEvent event, sessionId, userId, sourceToken)`, and the event name moved to `ApiEvent.type`.
- `CheckpointResponseOptions.timeout` is an `Integer` and `sync` is a `Boolean`. A new `CheckpointRequest` has `options` `null`.
- `checkpoint()` throws `ComposerMissingParameterException` (an `IllegalArgumentException`) before returning the future when the request, `checkpointName`, `event`, or both `sessionId` and `sourceToken` are missing. Blank strings count as missing. This happens even when the SDK is disabled.
- `event.ip` is optional on checkpoints.
- `event()` doesn't throw for invalid input or failed requests. It logs the problem and returns a response with `success` `false`.
- `sync` defaults to `false` and is sent as given. With `sync` `true`, the first request waits for the workflow to finish, so set `setRequestTimeoutMs` longer than your slowest workflow.
- `timeout` is now only how long the SDK polls, and defaults to 10 minutes. It's no longer sent to the API.
- `isTimeout` is `true` only after 3 consecutive failed polls, which return `success` `false`, a `null` `verification` and a `503`. When the polling budget runs out, the still-pending verification is returned; check `isRunning`.
- `isRunning` requires the outcome to be `PENDING` as well as the status to be `PENDING` or `BLOCKED`.
- `hasError` is `true` when `success` is `false` or the outcome is `ERROR`.
- `isAllowed`, `isDenied`, `isUndecided` and `hasError` compare status and outcome case-insensitively.
- Network failures and timeouts return `success` `false` with a `500` "Internal Server Error" in `errors`. HTTP error responses return the API's `errors`.
- `ComposerApiError` has an `Integer code`. `ComposerVerification.stepData` is a `Map<String, Object>`, and `nextSteps` is a `List<Map<String, Object>>`.
- When disabled, the placeholder verification ID is `COMPOSER_IS_DISABLED` (was `DODGEBALL_IS_DISABLED`).

### Removed
- `CheckpointResponseOptions.webhook` and its three-argument constructor; it's now `(sync, timeout)`. `checkpoint()` polls a pending verification as before.
- `setSandbox()`. Use `setApiUrl("https://api.sandbox.dodgeballhq.com")`.
- `VerificationStep`, `VerificationStepData`, `LibConfig`, `Dodgeball.Delegate`, and the static `createCheckpoint`, `verifyCheckpoint` and `executeNotification` helpers.
- The `Event(Map)` constructor. Use `Event(ip, data)`.
- `stepData` and `nextSteps` on `CheckpointResponse`. Read them from `verification`.
- `ComposerApiError.stack` and the `CheckpointResponse` and `NotificationResponse` constructors that took an exception.
- Retrofit and `converter-gson` as runtime dependencies. Declare them yourself if you used them.
