# Spreedly Composer Server SDK for Java

## Table of Contents

- [Purpose](#purpose)
- [Prerequisites](#prerequisites)
- [Related](#related)
- [Installation](#installation)
- [Usage](#usage)
- [API](#api)
- [Development](#development)
- [License](#license)
- [Support](#support)

## Purpose

[Spreedly Composer](https://developer.spreedly.com) enables developers to decouple security logic from their application code. This has several benefits including:

- The ability to toggle and compare security services like fraud engines, MFA, KYC, and bot prevention.
- Faster responses to new attacks. When threats evolve and new vulnerabilities are identified, your application's security logic can be updated without changing a single line of code.
- The ability to put in placeholders for future security improvements while focusing on product development.
- A way to visualize all application security logic in one place.

The Composer Server SDK for Java makes integration with the Composer API easy and is maintained by the Spreedly team.

## Prerequisites

- Java 11 or higher
- You will need to obtain an API key for your application from the Composer developer center.

## Related

Check out the [Client SDK](https://npmjs.com/package/@dodgeball/trust-sdk-client) for how to integrate Composer into your frontend applications.

[Composer examples](https://github.com/spreedly/composer-examples) has a runnable example server for this SDK in `server/java`, and a web client that calls it.

## Installation

Add the Maven dependency:

```xml
<dependency>
    <groupId>com.spreedly</groupId>
    <artifactId>composer-sdk-server</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or with Gradle:

```gradle
implementation 'com.spreedly:composer-sdk-server:1.0.0'
```

## Usage

This example protects an order endpoint end to end, using Spring Boot 3, which needs Java 17 or higher. Your API calls a checkpoint before placing the order. When the workflow needs more input from the user, such as MFA, your frontend gathers it with the Client SDK and calls your API again with the verification ID. See [Identifiers](#identifiers) for what each ID is and [useVerification](#useverification) for how the second call works.

```java
// In your API
import com.spreedly.composer.Composer;
import com.spreedly.composer.messages.CheckpointRequest;
import com.spreedly.composer.messages.CheckpointResponse;
import com.spreedly.composer.messages.Event;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    // private final Dodgeball composer = Dodgeball.builder().setApiKeys(System.getenv("DODGEBALL_SECRET_KEY")).build(); // The prior name is still supported.
    private final Composer composer = Composer.builder()
        .setApiKeys(System.getenv("SPREEDLY_SECRET_KEY"))
        .build();

    @PostMapping("/api/orders")
    public ResponseEntity<Map<String, Object>> placeOrder(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-Dodgeball-Source-Token", required = false) String sourceToken,
            @RequestHeader(value = "X-Dodgeball-Verification-Id", required = false) String verificationId,
            HttpServletRequest request) {
        Object order = body.get("order");

        CheckpointRequest checkpointRequest = new CheckpointRequest(
            // The client IP for the risk engine; callers must not choose it. Behind trusted
            // reverse proxies, set server.forward-headers-strategy so this is the client's IP.
            new Event(request.getRemoteAddr(), Collections.singletonMap("order", order)),
            "PLACE_ORDER",
            request.getSession().getId(),
            (String) request.getSession().getAttribute("userId"),
            sourceToken,
            verificationId
        );
        CheckpointResponse checkpointResponse = composer.checkpoint(checkpointRequest).join();

        if (Composer.isAllowed(checkpointResponse)) {
            // Object placedOrder = database.createOrder(order);
            return ResponseEntity.ok(Collections.singletonMap("order", order));
        } else if (Composer.isDenied(checkpointResponse)) {
            // Return the verification so the frontend can show the reason
            return ResponseEntity.status(403).body(Collections.singletonMap("verification", checkpointResponse.verification));
        } else if (Composer.isRunning(checkpointResponse)) {
            // The workflow needs more input from the user (such as MFA or KYC)
            return ResponseEntity.status(202).body(Collections.singletonMap("verification", checkpointResponse.verification));
        } else {
            // No verdict: decide whether to return the error, proceed, retry, or reject. See Errors.
            return ResponseEntity.status(500).body(Collections.singletonMap("message", checkpointResponse.errors));
        }
    }
}
```

```js
// In your frontend, with the Client SDK
const placeOrder = async (order, previousVerificationId = null) => {
  const sourceToken = await dodgeball.getSourceToken();

  const endpointResponse = await axios.post("/api/orders", { order }, {
    headers: {
      "x-dodgeball-source-token": sourceToken,
      "x-dodgeball-verification-id": previousVerificationId
    },
    // Let the 202 and 403 responses through instead of throwing
    validateStatus: (status) => [200, 202, 403].includes(status)
  });

  if (endpointResponse.status === 200) {
    setIsOrderPlaced(true);
    return;
  }

  dodgeball.handleVerification(endpointResponse.data.verification, {
    onVerified: async (verification) => {
      // The user completed the extra step: call your API again with the verification ID
      await placeOrder(order, verification.id);
    },
    onApproved: async () => {
      setIsOrderPlaced(true);
    },
    onDenied: async (verification) => {
      setIsOrderDenied(true);
    },
    onError: async (error) => {
      setError(error);
      setIsPlacingOrder(false);
    }
  });
}
```

## API

### Configuration

---

The SDK requires a secret API key, set through the builder.

```java
Composer composer = Composer.builder()
    .setApiKeys("secret-api-key...")
    .build();
```

Optionally, you can set more options on the builder:

```java
import com.spreedly.composer.Logger;

Composer composer = Composer.builder()
    .setApiKeys("secret-api-key...")
    .setApiUrl("https://api.dodgeballhq.com")
    .setLogLevel(Logger.ComposerLogLevel.ERROR)
    .setRequestTimeoutMs(60000)
    .build();
```

| Option | Default | Description |
| --- | --- | --- |
| `setApiKeys` | Required | Your secret API key. |
| `setApiUrl` | `https://api.dodgeballhq.com` | The base URL of the Spreedly Composer API. Useful for sending requests to different environments such as `https://api.sandbox.dodgeballhq.com`. |
| `setLogLevel` | `INFO` | `TRACE`, `INFO`, `ERROR` or `NONE`. The SDK logs errors, plus redacted request and response payloads at `TRACE`, so `INFO` and `ERROR` currently log the same thing. The level is shared by every `Composer` in the process. See [Safe logging](#safe-logging). |
| `setRequestTimeoutMs` | `30000` | How long a single HTTP request may take, in milliseconds. Applies to every call the SDK makes. A value of `0` or less is logged and replaced with the default. See [Timeouts](#timeouts). |
| `setEnabled` | `true` | When `false`, the SDK makes no API calls: `checkpoint` returns an approved response and `event` returns a successful response without sending anything. |

### Safe logging

---

The SDK writes JSON log lines to standard output. At the default `INFO` level it writes no request or response payloads: a successful checkpoint logs nothing, and `ERROR` reports request failures. Payload logging is opt-in through `TRACE`, and even there the fields that describe your end users or carry provider credentials (`ip`, `data`, `stepData` and `nextSteps`) are replaced with `[REDACTED]`, so a verbose log shows the shape of a request or response without carrying PII. Turn `TRACE` on only in environments where you are comfortable with that extra detail.

### Identifiers

---

`checkpoint` and `event` both take these. Provide a `sessionId`, a `sourceToken`, or both.

| Parameter | Required | Description |
| --- | --- | --- |
| `sourceToken` | Either this or `sessionId` | A token representing the device making the request. Obtained from the [Client SDK](https://npmjs.com/package/@dodgeball/trust-sdk-client). |
| `sessionId` | Either this or `sourceToken` | The current session ID of the request. |
| `userId` | No | The ID of the user in your database, once you know it (for example, after registration). |
| `useVerificationId` | No | `checkpoint` only. The ID of a verification the user already completed. See [useVerification](#useverification). |

### Call a Checkpoint

---

Checkpoints represent key moments of risk in an application and are at the core of how Composer works. A checkpoint can represent any activity deemed to be a risk. Some common examples include: login, placing an order, redeeming a coupon, posting a review, changing bank account information, making a donation, transferring funds, creating a listing.

```java
Map<String, Object> data = Map.of(
    "transaction", Map.of("amount", 100, "currency", "USD"),
    "paymentMethod", Map.of("token", "ghi789")
);

CheckpointRequest request = new CheckpointRequest(
    new Event("127.0.0.1", data),
    "CHECKPOINT_NAME",
    "session_def456", // sessionId
    null              // userId
);

CheckpointResponse checkpointResponse = composer.checkpoint(request).join();
```

`checkpoint` returns a `CompletableFuture<CheckpointResponse>`. `CheckpointRequest` has public fields and two constructors: `(event, checkpointName, sessionId, userId)` and `(event, checkpointName, sessionId, userId, sourceToken, useVerificationId)`. Along with the [identifiers](#identifiers), it takes:

| Parameter | Required | Description |
| --- | --- | --- |
| `checkpointName` | Yes | The name of the checkpoint to call. |
| `event` | Yes | An `Event`. |
| `event.ip` | No | The IP address of the device where the request originated. Recommended whenever an end user is present; leave it `null` for checkpoints with no user, such as renewals. |
| `event.data` | No | Data your checkpoint's workflow uses. |
| `options` | No | A `CheckpointResponseOptions` with `sync` (defaults to `false`) and `timeout` (how long the SDK keeps polling an unresolved verification, in milliseconds; defaults to 10 minutes). See [Timeouts](#timeouts). |

### Timeouts

---

Two settings control timing and neither one caps the other. `setRequestTimeoutMs` limits how long a single HTTP request may take, and applies to every call the SDK makes. The `timeout` in checkpoint options limits how long the SDK keeps polling for an unresolved verification, and defaults to 10 minutes. The SDK always polls at least once.

```java
Composer composer = Composer.builder()
    .setApiKeys("secret-api-key...")
    .setRequestTimeoutMs(5000) // No single HTTP call may hang longer than 5 seconds
    .build();

CheckpointRequest request = new CheckpointRequest(new Event("127.0.0.1", null), "CHECKPOINT_NAME", "session_def456", null);
request.options = new CheckpointResponseOptions(null, 10000); // Stop polling after 10 seconds and return whatever we have

CheckpointResponse checkpointResponse = composer.checkpoint(request).join();
```

This example leaves `sync` unset, so it defaults to `false`, which we recommend. The first request returns right away with a pending verification, and the SDK polls until it resolves, so each request is short and `setRequestTimeoutMs` only guards against a hung connection. With `sync` set to `true`, the first request waits on the server until the workflow finishes, so `setRequestTimeoutMs` must be longer than your slowest workflow. Otherwise a slow but healthy checkpoint comes back as a failure.

When the polling budget runs out, the verification is simply still pending, which is not an error: `isRunning` returns `true`, and you can resume later by passing the verification ID to `useVerificationId`. The exception is when the last poll before the budget ran out failed: you get that failure instead, with `success` `false`, and `hasError` returns `true`.

### Checkpoint Responses

---

Calling a checkpoint creates a verification in Composer. Its `status` and `outcome` determine how your application should proceed. Use the [utility methods](#utility-methods) rather than reading these fields directly.

```java
public class CheckpointResponse {
    public boolean success;
    public ComposerApiError[] errors;          // code, message
    public String version;
    public ComposerVerification verification;  // id, status, outcome, stepData, nextSteps
    public boolean isTimeout;
}
```

#### Response fields

| Property | Description |
| --- | --- |
| `success` | `false` when no verification result could be produced. See [Errors](#errors). A verification that failed while running still has `success` `true`. |
| `errors` | `ComposerApiError` objects with a `message`, and a `code` when the API sends one. Usually empty when `success` is `true`. |
| `version` | The version of the Composer API that was used to make the request. |
| `verification` | The verification. Can be `null` when `success` is `false`. |
| `verification.id` | The ID of the verification that was created. |
| `verification.status` | `COMPLETE`, `PENDING`, `BLOCKED` or `FAILED`. |
| `verification.outcome` | `APPROVED`, `DENIED`, `PENDING`, `ERROR`, `USAGE_LIMIT` or `API_LIMIT`. |
| `verification.stepData` | Data from the workflow's steps. |
| `verification.nextSteps` | Steps the Client SDK runs next, such as MFA. Return the verification to your frontend unchanged so the Client SDK can start them right away. |
| `isTimeout` | `true` when polling stopped after 3 failed polls in a row. See `isTimeout` below. |

#### Statuses and outcomes

| `status` | `outcome` | Utility method | Meaning |
| --- | --- | --- | --- |
| `COMPLETE` | `APPROVED` | `isAllowed` | Let the request proceed. |
| `COMPLETE` | `DENIED` | `isDenied` | Deny the request. Return the verification to the frontend to show the reason. |
| `BLOCKED` | `PENDING` | `isRunning` | The workflow is waiting for input from the user, such as MFA or KYC. See [useVerification](#useverification). |
| `PENDING` | `PENDING` | `isRunning` | Still processing when the polling budget ran out. Resume later with `useVerificationId`. |
| `COMPLETE` | `PENDING` | `isUndecided` | The workflow finished without reaching a verdict. Your application decides how to proceed. |
| `FAILED` | `ERROR` | `hasError` | The workflow failed while running, for example a third-party service was unavailable. |
| `FAILED` | `USAGE_LIMIT` | `hasError` | Your account reached its usage limit. |
| `FAILED` | `API_LIMIT` | `hasError` | Your account is sending too many requests right now, or is deactivated. |

### Utility Methods

---

Each method is static, takes a checkpoint response and returns a `boolean`.

| Method | Returns `true` when |
| --- | --- |
| `Composer.isAllowed(checkpointResponse)` | The request is allowed to proceed. |
| `Composer.isDenied(checkpointResponse)` | The request is denied. |
| `Composer.isRunning(checkpointResponse)` | No verdict yet: the workflow is still processing or waiting on the user. |
| `Composer.isUndecided(checkpointResponse)` | The workflow finished without a verdict. |
| `Composer.hasError(checkpointResponse)` | `success` is `false`, or the workflow failed while running. |
| `Composer.isTimeout(checkpointResponse)` | Polling stopped after 3 failed polls in a row. Running out of the polling `timeout` is not a timeout in this sense; see [Timeouts](#timeouts). |

### Errors

---

Only invalid input throws. Everything else comes back as a response, so a checkpoint never throws because of the network or the API.

| What happened | Throws? | What you get | Utility method |
| --- | --- | --- | --- |
| Missing API key | Yes, from `build()`: `ComposerMissingConfigException` (an `IllegalStateException`) | | |
| A `null` request or `event`, a blank `checkpointName`, or neither `sessionId` nor `sourceToken` | Yes, from `checkpoint`, before it returns the future: `ComposerMissingParameterException` (an `IllegalArgumentException`) | | |
| The API rejected the request, for example there is no published workflow for the checkpoint name, or a usage or API limit was reached | No | `success` `false`, the API's message in `errors` | `hasError` |
| Network error, request timeout, or a response the SDK can't read | No | `success` `false`, a `500` in `errors` | `hasError` |
| 3 polls failed in a row | No | `success` `false`, a `503` in `errors` | `hasError`, `isTimeout` |
| The workflow failed while running | No | `success` `true`, status `FAILED`, outcome `ERROR` | `hasError` |
| Invalid input to `event` | Never | A response with `success` `false`; the SDK logs it (see [Track an Event](#track-an-event)) | |

When `success` is `false` there is no verdict at all. Don't treat it as a denial; decide whether to retry, proceed, or reject.

### useVerification

---

Sometimes the workflow needs more input from the user before it can reach a verdict. For example, if the user must complete MFA, the checkpoint response has `status` `BLOCKED` and `outcome` `PENDING`, and `isRunning` returns `true`. Return the verification to your frontend and pass it to the Client SDK's `handleVerification()` method, which gathers the input from the user. When the user is done, the frontend calls your API again with the verification ID, and your API passes it to `useVerificationId`. The checkpoint then uses that verification instead of creating a new one, so the user isn't asked twice. The [Usage](#usage) example shows the full flow.

**Important Note:** To prevent replay attacks, each verification ID can only be passed to `useVerificationId` once.

### Track an Event

---

You can track additional information about a user's journey by submitting tracking events from your server. This information will be added to the user's profile and is made available to checkpoints.

```java
NotificationRequest request = new NotificationRequest(
    new ApiEvent("ADD_TO_CART", Map.of("product", Map.of("id", "sku_123", "quantity", 1))),
    "session_def456", // sessionId
    null,             // userId
    null              // sourceToken
);

CompletableFuture<NotificationResponse> responseFuture = composer.event(request);
```

`ApiEvent` is in `com.spreedly.composer.api`. `NotificationRequest` takes `(event, sessionId, userId, sourceToken)`. Along with the [identifiers](#identifiers), it takes:

| Parameter | Required | Description |
| --- | --- | --- |
| `event` | Yes | An `ApiEvent`. |
| `event.type` | Yes | A name for where the user is in their journey. Can be any non-blank string you choose. |
| `event.data` | No | Data to track. |
| `event.eventTime` | No | When the event happened, in milliseconds since the epoch. Defaults to now. |

`event` returns a `CompletableFuture<NotificationResponse>`. It doesn't throw for invalid input or failed requests, so tracking can't break your request. `notify` and `track` are deprecated aliases. Problems are logged at `ERROR` instead, and the response has `success` `false`:

- If the request or `event` is `null`, `event.type` is blank, or neither `sessionId` nor `sourceToken` is provided, the event is not sent and the SDK logs `Composer event was not sent: <reason>`.
- If the API rejects the event, the SDK logs `Composer event was not accepted by the API.`
- If the request fails (network error or timeout), the SDK logs `There was an error making a request.`

## Development

### Versioning

The SDK version is declared in two places that must be kept in sync when releasing:

- `pom.xml` (`version`)
- `src/main/java/com/spreedly/composer/messages/Constants.java` (`SDK_VERSION`, sent in the `Composer-SDK-Version` header)

Update both when bumping the version.

### Testing

Run unit tests:

```sh
mvn test
```

Each run writes a coverage report to `target/site/jacoco/index.html`.

### Building

To build the project and install it to your local Maven repository (`-Dgpg.skip` skips release signing):

```sh
mvn clean install -Dgpg.skip
```

To generate JavaDoc:

```sh
mvn javadoc:javadoc
```

### Testing with the examples

To try local SDK changes end to end, clone [Composer examples](https://github.com/spreedly/composer-examples) next to this repository, run `mvn clean install -Dgpg.skip` here to install your build into your local Maven repository, and run the `server/java` example as the examples README describes. The example's `pom.xml` must depend on the version in this repository's `pom.xml`:

```xml
<dependency>
  <groupId>com.spreedly</groupId>
  <artifactId>composer-sdk-server</artifactId>
  <version>1.0.0</version>
</dependency>
```

Reinstall here and rebuild the example after each change.

## License

This SDK is released under the Apache-2.0 License. See [LICENSE](LICENSE) for details, and [THIRD-PARTY-LICENSES](THIRD-PARTY-LICENSES) for the licenses of its dependencies.

## Support

For support, please contact:

- Email: support@spreedly.com
- Documentation: https://developer.spreedly.com
