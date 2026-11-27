# Analytics

Declare each analytics event as a separate class directly extending `AnalyticsEvent`, with explicit named `name` and `properties` arguments, following the existing events. Use `mapOf` for required properties and `buildMap` with explicit null checks for optional properties. Omit unknown optional values; preserve meaningful `false` and zero values. Parameterless events may use `object`, following existing examples.

```kotlin
class SignRequestShown(source: SignSource, method: String, chain: String) : AnalyticsEvent(
    name = "sign_request_shown",
    properties = mapOf(
        "source" to source.value,
        "method" to method,
        "chain" to chain
    )
)
```

Do not replace event classes with factory functions such as `fun typeSelected(...) = event(...)` or generic vararg event builders. Feature namespaces may group event classes, but each event must still directly extend `AnalyticsEvent`. Send events through the existing `AnalyticsService`; do not introduce a parallel analytics pipeline. When refactoring declarations, preserve wire event names, property keys and types, omission rules, and emission timing.

Keep event-specific types and values at the tracking boundary. Make decisions about routing, feature behavior
and warnings with the types that own that behavior. Map their values to analytics enums and event properties
when building an event. A navigation payload may carry an analytics-only source for later attribution.

Analytics consent controls analytics collection, queued events and uploads, not the lifetime or availability of authentication shared with other features. Opt-out must cancel analytics work and clear its queue without disabling unrelated authenticated requests.

## What an event may carry

An event must not contain a value that singles out a user or can be matched against public chain data.
Never send:

- exact numbers: amounts, balances, fees, rates, prices, counts, durations or timestamps of a user action;
- addresses, account ids, public keys, hotkeys, transaction or extrinsic hashes, wallet names or ids;
- free text, user input, raw error messages or exception class names.

Send a range or a category instead: the existing buckets in `ValueBucketing.kt` (`AmountBucket` in USD,
`DurationBucket`, `SlippageBucket`, `NftCountBucket`), a closed enum, or a boolean. Add a new bucket type
when none fits; do not send the raw number "for now". Public catalogue identifiers already used by existing
events (asset symbol, network, chain id, subnet `netuid`, provider id) are allowed. When the value cannot be
bucketed, follow the bucket's own rule (`UNKNOWN` or omitting the property); never fall back to the exact value.

## Review

Analytics coverage is checked in review, not left to the author's memory. Every review of a change with
user-facing behavior has an analytics pass and reports it as its own section:

1. Walk the diff and list each place where user-facing code was added or changed: a new screen, sheet,
   flow, entry point or action, a changed outcome of an existing action, and a moved or replaced screen.
2. For each place decide whether it is covered by an event, existing or added in the change. A place
   without one is a finding that names the place and the missing event (name, trigger, properties); the
   events are then added in the same change, taken from the tracking plan when the task has one.
3. For a moved or replaced screen, check that its existing events still fire with the same meaning and
   frequency. Events tied to a navigation destination id, a back-stack host or a ViewModel's
   `init`/`onCleared` silently stop or change when the screen moves to another graph, becomes a tab or
   gets a longer-lived owner.
4. Check every new or changed property against "What an event may carry"; an exact number or an
   identifier is a blocking finding.
5. Check that names and values match the tracking plan and iOS byte-for-byte, that an existing wire event
   is reused with a new property where the meaning matches, and that emission is unit-tested, including
   the path where the event must not fire.

If a place intentionally sends nothing, the review records that and why.
