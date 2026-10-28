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

Analytics consent controls analytics collection, queued events and uploads, not the lifetime or availability of authentication shared with other features. Opt-out must cancel analytics work and clear its queue without disabling unrelated authenticated requests.
