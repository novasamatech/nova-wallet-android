# Data, state and runtime

## Independent loading

Load interactor data with `flowOf { ... }` or an interactor-provided Flow and share it with `shareInBackground()` when reused. Derive loading and presentation values with the established flow operators instead of copying them into `MutableStateFlow` from `init`. Keep mutable flows for user-editable state and explicit operation/retry controls, not redundant copies of loaded data. Name selections after the selected entity. Keep entity lookup by ID in the interactor rather than filtering its catalogue in the ViewModel.

Use `withSafeLoading()` only when its loading state is consumed by the UI, including disabling actions while data is loading. Do not add loading wrappers solely to catch exceptions; use `catch` with appropriate error reporting when no loading state is needed. Retry infrastructure must not impose UI loading states; apply loading-state transformations at the consuming ViewModel, per attempt when retries must reset the UI.

Use `mapNotNull` when a derived flow should emit only available values instead of forwarding null and checking it in every consumer. Preserve nullable emissions when null explicitly clears stale UI data, selection, or operation readiness during loading or failure.

Independent data, such as wallet positions and a market chart, must have independent sources, error states, and retries. Failure of one load must not hide another load's successful data. When converting an exception into an error or stale state, log diagnostic context and the cause without secrets or personal data.

Use the established retry dialog for failed screen reads instead of adding inline error/retry controls. Preserve automatic recovery of reactive sources and distinguish stale-data warnings from terminal loading errors.

Derive staking and dashboard updates from reactive balance or on-chain state sources. Do not introduce a feature-specific event bus or call dashboard refresh methods after submitting a transaction. Keep complex input flows in named private methods so that `combine` only assembles their results. Do not wrap `ChainRegistry.getChain` in a dedicated timeout; chain lookup uses the existing registry lifecycle.

External staking balances belong to the existing wallet balance update lifecycle, not only a staking screen subscription. Key valuations by the explicit wallet and asset. Persist an initial partial valuation with its completeness marker, but never replace a stored complete value with incomplete or stale data. Replace partial and complete entries atomically so they cannot be counted twice. Fresh confirmed empty holdings must clear both entries. Keep portfolio totals nullable until fully priced. Reuse shared position streams so dashboard, asset balances and details do not duplicate runtime subscriptions. Include positive external balances in the existing automatic asset-discovery path, respecting disabled discovery and explicit visibility choices.

## API models and mapping

Model known JSON API and configuration contracts with typed remote DTOs returned directly by Retrofit. Keep domain mapping and semantic validation in repositories or mappers; do not traverse `JsonObject`, `JsonElement`, or untyped maps for responses with a known schema. Reserve raw JSON for genuinely dynamic payloads.

Use `map...` and `...Mapping`/`...Mapper` names for transformations between DTOs and domain models. Reserve `bind...` and `...Binding` names in data conversion code for SCALE/runtime decoding, not JSON DTO mapping. Keep validation-only helpers named `require...` or `validate...`.

Keep Retrofit API interfaces and their remote DTOs in separate files from repository implementations, following the existing API/remote-model/repository structure.

Validate response pagination against the requested page and the endpoint contract before treating history as complete. A skipped page is not a complete history. Admit responses to caches only after structural and semantic validation; compute monotonic cache deadlines from request start so transport latency does not extend freshness.

Distinguish optional presentation fallbacks from transaction inputs. Missing logos may use a placeholder, but a failed commission-config request must not silently supply a default fee. A documented default for an absent or invalid field in a successfully decoded config is a separate case.

## Runtime decoding

Use the SDK's `AccountId` alias for raw Substrate account identifiers in signatures and explicitly typed properties, not `ByteArray`. Preserve meaningful domain names such as `coldKey` when they clarify the account's role; changing the type does not require renaming the identifier. Keep protocol-specific wire field names unchanged. Keep `AccountIdKey` for content-based equality and collection keys, and reserve `ByteArray` for actual binary payloads rather than account IDs.

For named metadata structures, the SDK normalizes snake_case fields to camelCase. Bindings must read normalized names. Tests must use the structure returned by the decoder rather than the original Rust field names.

Respect Android minSdk when converting numbers: a passing JVM test does not prove a JDK method exists on the device. Do not use `BigInteger.intValueExact()` on older Android versions without confirmed support. Check the range first, then use `toInt()` to prevent overflow.

## Request ownership and shared subscriptions

Keep read and mapping methods free of hidden workflow state changes. Pass an explicit wallet, validator and amount context into fee estimation and preparation rather than relying on the last observer's cached details. Associate asynchronous results with their originating request and invalidate stale presentation as soon as that request changes.

Share subscriptions, decoding, and loading when multiple consumers need different parts of the same runtime response. Key shared streams by chain and account identity, bind their lifetime to active consumers, and keep dashboard and screen state derived from the same source. Keep recovery independent for unrelated reads, and keep screen-specific sources out of screens that do not consume them.

## Cancellation and state transitions

Preserve CancellationException through catch/result/retry boundaries. A cancelled screen must not
emit a generic retry dialog onto the next screen. Do not convert failed financial reads to zero.
Keep retries bounded and user-recoverable; failed loading must leave the pending state.
Check initial load, success, empty/missing, failure, retry, wallet/asset change and leaving the screen.
Do not retain a stale selection or enable submission from a result belonging to an earlier request.
