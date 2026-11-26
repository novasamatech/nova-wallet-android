# Data, state and runtime

## Absence and state

Use `null` only for genuinely absent values, such as an unset selection or an omitted optional field.
Represent meaningful alternatives such as loading, failure, stale data, an unavailable calculation, or a
different validation mode with an explicit sealed class or interface. Reuse an existing sealed type when
it fits. Keep each case and its associated data together so callers can handle the cases exhaustively.

Keep nullable API fields and database columns where their contracts allow absence. Map them to explicit
states at the boundary when their meaning affects business logic or presentation.

## Independent loading

Load interactor data with `flowOf { ... }` or an interactor-provided Flow and share it with `shareInBackground()` when reused. Derive loading and presentation values with the established flow operators instead of copying them into `MutableStateFlow` from `init`. Keep mutable flows for user-editable state and explicit operation/retry controls, not redundant copies of loaded data. Name selections after the selected entity. Keep entity lookup by ID in the interactor rather than filtering its catalogue in the ViewModel.

Use the existing `ExtendedLoadingState<T>` for loading, loaded data and failure; do not introduce local
`Loading` / `Loaded` / `Failed` state hierarchies with the same meaning. Reuse `withSafeLoading()`,
`mapLoading`, `onError`, `firstIfLoaded` and other existing helpers where their semantics fit. Preserve the
error cause and coroutine cancellation. A separate domain state is justified only for additional domain
transitions that this shared loading model cannot express.

Use `withSafeLoading()` only when its loading state is consumed by the UI, including disabling actions while data is loading. Do not add loading wrappers solely to catch exceptions; use `catch` with appropriate error reporting when no loading state is needed. Retry infrastructure must not impose UI loading states; apply loading-state transformations at the consuming ViewModel, per attempt when retries must reset the UI.

Use `mapNotNull` when a derived flow should emit only present optional values and dropping absence is
intentional. Emit explicit states when loading, failure, or invalidation needs to clear stale UI data or
operation readiness. A nullable selection can emit `null` to indicate that the selection was cleared.

Independent data, such as wallet positions and a market chart, must have independent sources, error states, and retries. Failure of one load must not hide another load's successful data. When converting an exception into an error or stale state, log diagnostic context and the cause without secrets or personal data.

Use the established retry dialog for failed screen reads instead of adding inline error/retry controls. Preserve automatic recovery of reactive sources and distinguish stale-data warnings from terminal loading errors.

Derive staking and dashboard updates from reactive balance or on-chain state sources. Do not introduce a feature-specific event bus or call dashboard refresh methods after submitting a transaction. Keep complex input flows in named private methods so that `combine` only assembles their results. Do not wrap `ChainRegistry.getChain` in a dedicated timeout; chain lookup uses the existing registry lifecycle.

External staking balances belong to the existing wallet balance update lifecycle, not only a staking screen subscription. Key valuations by the explicit wallet and asset. Persist an initial partial valuation with its completeness marker, but never replace a stored complete value with incomplete or stale data. Replace partial and complete entries atomically so they cannot be counted twice. Fresh confirmed empty holdings must clear both entries. Represent loading, incomplete, and unavailable portfolio valuations with explicit sealed states. Reuse shared position streams so dashboard, asset balances and details do not duplicate runtime subscriptions. Include positive external balances in the existing automatic asset-discovery path, respecting disabled discovery and explicit visibility choices.

## API models and mapping

Model known JSON API and configuration contracts with typed remote DTOs returned directly by Retrofit. Keep domain mapping and semantic validation in repositories or mappers; do not traverse `JsonObject`, `JsonElement`, or untyped maps for responses with a known schema. Reserve raw JSON for genuinely dynamic payloads.

When a source encapsulates cached backend access, expose endpoint-specific methods. Keep URL construction,
response types, transport callbacks and endpoint cache policy inside that source. When several sources
share backend mechanics, inject one shared client with internal generic request helpers (`get`/`observe`).
Only sources call those helpers; repositories receive typed sources. Do not copy the generic pipeline
into every source or introduce a public universal-source facade. Endpoint-specific concurrency limits
belong to the owning source; the common client must not infer them from URL strings.
Return typed payloads from source APIs, not cache envelopes or TTL metadata. Unwrap responses once in
the shared client (`get: T`, `observe: Flow<T>`), rather than repeating `.value` in every source.
Use an explicit internal lifetime-aware path only when a source aggregates cached pages. Cache expiry controls
refresh inside the source/client; it must not rewrite backend freshness or imply transaction eligibility.
Preserve backend-reported availability and separate live transaction validation.
Reuse repository/domain mappers for validation before storing a response.

Cache ownership is a general data-layer rule, not a Bittensor-only convention: caching belongs in
sources, never in repositories. Sources own both persistent and in-memory caches, cache validation,
restoration, refresh, invalidation and request/subscription sharing. Repositories compose and map source
results; they must not depend on cache implementations, build cache keys, serialize cached payloads or
manage expiry/revisions. Apply this rule to new work and authorized refactors; do not rewrite unrelated
legacy code merely because it violates the rule. Consumers call
explicit domain methods instead of constructing storage keys or passing cache-control flags. Keep each
key definition in one place and reuse it for reads, writes and invalidation. Orchestration calls those
owners directly; do not add a separate invalidator just to reconstruct their keys. Preserve explicit
operation/account identity, cancellation and rejection of obsolete writes when simplifying this design.

Use `map...` and `...Mapping`/`...Mapper` names for transformations between DTOs and domain models. Reserve `bind...` and `...Binding` names in data conversion code for SCALE/runtime decoding, not JSON DTO mapping. Keep validation-only helpers named `require...` or `validate...`.

Keep Retrofit API interfaces and their remote DTOs in separate files from repository implementations, following the existing API/remote-model/repository structure.

Validate response pagination against the requested page and the endpoint contract before treating history as complete. A skipped page is not a complete history. Admit responses to caches only after structural and semantic validation; compute monotonic cache deadlines from request start so transport latency does not extend freshness.

Distinguish optional presentation fallbacks from transaction inputs. Missing logos may use a placeholder, but a failed commission-config request must not silently supply a default fee. A documented default for an absent or invalid field in a successfully decoded config is a separate case.

### nova-utils config mirror

nova-utils `master` files are read from the nova-infra mirror first, GitHub is the per-request fallback.
`*_URL` build fields and URLs inside configs stay raw GitHub URLs. `NovaUtilsMirror.addTo` adds the mirror to
an `OkHttpClient` (`NetworkModule`, the image loader in `CommonModule`). A new `OkHttpClient` or `ImageLoader`
that reads nova-utils must go through it too, as the last step on the finished client, after
`TimeHeaderInterceptor`; without it a client silently reads GitHub only.

The policy is shared with iOS and stated in `NovaUtilsMirror` KDoc: a GET of a `master` path without dot
segments is mapped; one deadline for the whole attempt (10 s, at most half of the caller's timeout); accepted
is a 200 with an `X-Config-Commit` header and a non-empty body, valid JSON for `.json`; a status of 500 and
above, a 429, or a network error after which GitHub answers pauses the mirror (60 s, doubling to 30 min, in
memory); anything else only falls back. Accepted decision: the mirror is trusted as GitHub is, with no integrity
or freshness check; the header's presence is checked, its value is not read. OkHttp limits are in
`NovaUtilsMirrorInterceptor` KDoc.

## Runtime decoding

Use the SDK's `AccountId` alias for raw Substrate account identifiers in signatures and explicitly typed properties, not `ByteArray`. Preserve meaningful domain names such as `coldKey` when they clarify the account's role; changing the type does not require renaming the identifier. Keep protocol-specific wire field names unchanged. Keep `AccountIdKey` for content-based equality and collection keys, and reserve `ByteArray` for actual binary payloads rather than account IDs.

For named metadata structures, the SDK normalizes snake_case fields to camelCase. Bindings must read normalized names. Tests must use the structure returned by the decoder rather than the original Rust field names.

Respect Android minSdk when converting numbers: a passing JVM test does not prove a JDK method exists on the device. Do not use `BigInteger.intValueExact()` on older Android versions without confirmed support. Check the range first, then use `toInt()` to prevent overflow.

## Request ownership and shared subscriptions

Keep read and mapping methods free of hidden workflow state changes. Pass an explicit wallet, validator and amount context into fee estimation and preparation rather than relying on the last observer's cached details. Associate asynchronous results with their originating request and invalidate stale presentation as soon as that request changes.

Share subscriptions, decoding, and loading when multiple consumers need different parts of the same runtime response. Key shared streams by chain and account identity, bind their lifetime to active consumers, and keep dashboard and screen state derived from the same source. Keep recovery independent for unrelated reads, and keep screen-specific sources out of screens that do not consume them.

Use `ComputationalCache.observeSharedFlow` when sharing should last only while collected; do not repeat
`flow`/`coroutineScope`/`emitAll` wrappers in every source. Keep `useSharedFlow` with an explicit scope
when that scope intentionally owns a longer lifetime. Express recurring refresh through a named Flow
operation that states its triggers and intervals, rather than manual channels/jobs at each call site.

## Cancellation and state transitions

Preserve CancellationException through catch/result/retry boundaries, including `runCatching` and
`Result.onFailure`: rethrow cancellation before continuing to another action or presenting an error.
A returned `Result` must be handled by the consumer; do not silently ignore it or discard its failure
with `getOrNull()` unless an explicit optional-data fallback applies. A cache-maintenance failure after
submission must not replace the observed transaction outcome with a transaction failure.
A cancelled screen must not
emit a generic retry dialog onto the next screen. Do not convert failed financial reads to zero.
Keep retries bounded and user-recoverable; failed loading must leave the pending state.
Check initial load, success, empty/missing, failure, retry, wallet/asset change and leaving the screen.
Do not retain a stale selection or enable submission from a result belonging to an earlier request.
