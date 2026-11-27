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

## Coverage of new features

Every new user-facing feature, screen, flow or entry point ships with its analytics in the same change.
Coverage is part of the feature, not a follow-up task. Decide the events before implementation: take them
from the task's tracking plan, or propose a short one (event, trigger, properties, question it answers)
when the task has none. The minimum:

- **Entry:** one "opened" event per screen or flow, with a closed-enum `source` naming each entry point.
- **Operation funnel:** initiated (setup validations passed), confirmed (handed to signing), completed,
  failed with a closed-enum `reason`, and abandoned with the `stage` the user left from.
- **Decisions that change the outcome:** selection method, changed settings such as slippage, retry.
- **Blockers:** a refusal the user cannot fix by input (wallet type, missing account, unavailable config)
  is its own event and must not be counted as abandonment.

Reuse an existing wire event and add a property when the semantics match, so the feature appears in existing
insights; add a new event only for a new meaning. Event names, property keys and values must match iOS
byte-for-byte. Cover emission with a unit test of the owning ViewModel/presenter, including the path
where the event must not fire. If a feature intentionally sends nothing, say so and why in the PR description.

Moving or replacing a screen is a change to its analytics. Events tied to a navigation destination id, a
back-stack host or a ViewModel's `init`/`onCleared` silently stop or change frequency when the screen moves
to another graph, becomes a tab or gets a longer-lived owner. Re-verify every existing event of the moved
screen and keep its trigger meaning ("user opened", "user left") rather than the old lifecycle hook.

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

When reviewing a change that adds or modifies user-facing behavior, report analytics as its own section:

1. List every new or moved screen, sheet, flow and entry point in the diff and mark each one covered or
   not covered. Uncovered items are findings, not notes.
2. Check that events of moved or replaced screens still fire with the same meaning and frequency.
3. Check every new or changed property against "What an event may carry"; an exact number or an identifier
   is a blocking finding.
4. Check that names and values match the tracking plan and iOS, and that emission is tested.
