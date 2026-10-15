# Android: established conventions

## UI reuse

Before creating a component or formatting implementation, inspect analogous screens and shared components. Use existing balance formatters rather than raw decimal strings. Match placeholders to the data type: an address identicon is not a fallback for a token or chain logo. For a title-and-search header, check `common/view/TitledSearchToolbar` first.

Use injected `FiatFormatter` for fiat amounts, not deprecated `formatAsCurrency` extensions. Read native-token symbols and precision from the actual chain asset; never hardcode display tickers in Kotlin, runtime XML text, or string resources. This includes rates, amount presets, currency switches, and descriptions; pass symbols as formatting parameters. Subnet-token symbols come from subnet metadata, not the native asset.

Define chart-formatting extension points as named interfaces alongside the shared chart component rather than exposing formatting lambdas. Keep feature-specific formatting implementations outside the shared chart code.

Reuse `ExternalActions.showAddressActions` and `setupExternalActions` for address information: they provide the standard sheet with copying and explorer actions. In selection lists, the information action must not also select the row or confirm the selection.

Use the standard `RadioButton` with `wrap_content` dimensions, following existing selection screens. Fixed bounds copied from a design can clip the theme drawable; do not shrink the control's bounds to match the visible circle.

## Tables inside cards

When a `TableView` is inside a card with its own background, disable the table background with `app:showBackground="false"`. Setting `android:background` is insufficient because the component overrides it during construction. Do not introduce nested containers absent from the design.

## Sectioned lists

Sections belonging to one selection list must scroll together in one `RecyclerView`, including their headers and rows. Do not pin the first section or its card above the list without an explicit requirement. Reuse `common/list/GroupedListAdapter` and `toListWithHeaders`, with separate view types for distinct row types. A search header may remain pinned.

## Screen navigation arguments

Follow existing screen navigation patterns: expose typed `Parcelable` payloads in router methods, build arguments through the destination Fragment's `getBundle(payload)`, and read the payload in that Fragment before binding it into the screen DI component. Do not scatter argument keys or parse individual Bundle values in navigators and DI modules.

Preserve navigation XML organization: keep the leading include block together, group global actions with actions and related destinations together, and do not prepend feature nodes ahead of includes. Match neighboring XML indentation, place attributes on separate lines, and separate sibling declarations with blank lines.

## Operation confirmation

Fee estimation and submission must use the same call builder, including service fees and batch mode. Do not replace confirmed amounts or protective price limits with a fresh quote just before signing. Bind confirmation data and signing to the explicit operation wallet, not the globally selected wallet. Do not reject an operation merely because the selected meta-account changed. Recheck that operation wallet's available balance and applicable permissions before submission. Do not substitute a root validator for a subnet validator without explicit configuration and eligibility checks in that subnet.

Before extending shared transaction APIs or introducing feature-specific submission tracking, inspect existing transfer, swap, and staking flows. Reuse their established submission and error-handling path. Cross-cutting reliability changes belong in shared infrastructure, not feature-specific signing callbacks or persistent pending locks.

Implement each validation rule as a separate Validation with one responsibility; keep data loading outside rule logic and let ValidationSystem compose the rules. Reuse existing amount and balance validations. Use the existing Validation/ValidationSystem and ValidationExecutor pipeline for expected transaction validation failures and warnings. Map failures through the established validation UI formatters; do not route them through custom exceptions or separate confirmation-dialog mechanisms. Revalidate time-sensitive data when continuing after a warning.

## Independent loading on one screen

Load interactor data with `flowOf { ... }` or an interactor-provided Flow and share it with `shareInBackground()` when reused. Derive loading and presentation values with the established flow operators instead of copying them into `MutableStateFlow` from `init`. Keep mutable flows for user-editable state and explicit operation/retry controls, not redundant copies of loaded data. Name selections after the selected entity. Keep entity lookup by ID in the interactor rather than filtering its catalogue in the ViewModel.

Use `withSafeLoading()` only when its loading state is consumed by the UI, including disabling actions while data is loading. Do not add loading wrappers solely to catch exceptions; use `catch` with appropriate error reporting when no loading state is needed.

Use `mapNotNull` when a derived flow should emit only available values instead of forwarding null and checking it in every consumer. Preserve nullable emissions when null explicitly clears stale UI data, selection, or operation readiness during loading or failure.

Independent data, such as wallet positions and a market chart, must have independent sources, error states, and retries. Failure of one load must not hide another load's successful data. When converting an exception into an error or stale state, log diagnostic context and the cause without secrets or personal data.

Derive staking and dashboard updates from reactive balance or on-chain state sources. Do not introduce a feature-specific event bus or call dashboard refresh methods after submitting a transaction. Keep complex input flows in named private methods so that `combine` only assembles their results. Do not wrap `ChainRegistry.getChain` in a dedicated timeout; chain lookup uses the existing registry lifecycle.

## Remote API models

Model known JSON API and configuration contracts with typed remote DTOs returned directly by Retrofit. Keep domain mapping and semantic validation in repositories or mappers; do not traverse `JsonObject`, `JsonElement`, or untyped maps for responses with a known schema. Reserve raw JSON for genuinely dynamic payloads.

Use `map...` and `...Mapping`/`...Mapper` names for transformations between DTOs and domain models. Reserve `bind...` and `...Binding` names in data conversion code for SCALE/runtime decoding, not JSON DTO mapping. Keep validation-only helpers named `require...` or `validate...`.

Keep Retrofit API interfaces and their remote DTOs in separate files from repository implementations, following the existing API/remote-model/repository structure.

## Runtime response decoding

Use the SDK's `AccountId` alias for raw Substrate account identifiers in signatures and explicitly typed properties, not `ByteArray`. Preserve meaningful domain names such as `coldKey` when they clarify the account's role; changing the type does not require renaming the identifier. Keep protocol-specific wire field names unchanged. Keep `AccountIdKey` for content-based equality and collection keys, and reserve `ByteArray` for actual binary payloads rather than account IDs.

For named metadata structures, the SDK normalizes snake_case fields to camelCase. Bindings must read normalized names. Tests must use the structure returned by the decoder rather than the original Rust field names.

Respect Android minSdk when converting numbers: a passing JVM test does not prove a JDK method exists on the device. Do not use `BigInteger.intValueExact()` on older Android versions without confirmed support. Check the range first, then use `toInt()` to prevent overflow.

## Bittensor validator selection

Use the shared iOS-compatible Bittensor configuration for the commission rate and subnet logos. Root-validator defaults come from eligible existing positions or attested recommendations, not a separate fixed-address configuration.

Load the validator directory and filtering policy through attested infrastructure endpoints, not directly from tao.app. Do not invent local `maxTake` values or fallback eligibility policies when the backend is unavailable. Check UID, permit, take, and activity on-chain at one block hash using batched queries; revalidate the selected validator before submission. Backend names and optional statistics do not replace on-chain eligibility checks.

Keep snapshot loading separate from validator mapping, and run independent storage reads concurrently within the same block-bound query context. Use `BittensorNetuid.Root` and `BittensorNetuid.Subnet` to distinguish root from dynamic subnets. APIs that only support dynamic subnets must accept `BittensorNetuid.Subnet`, not the base type or a raw integer. Use `isRoot()` instead of spreading root-ID comparisons through callers, and convert raw IDs at the boundary.

Generate a validator's icon from its hotkey with `AddressIconGenerator`, as for other addresses in the app. A chain logo or generic placeholder does not replace an address identicon. Explicitly pass the validator selected before amount entry into confirmation. Eligibility revalidation must not silently substitute another validator.

## Self-review and refactoring

Use existing numeric sign extensions (`isPositive`, `isNegative`, `isZero`, `isNonNegative`, and `isNonPositive`) instead of inline `signum()` comparisons or local duplicates. Respect their existing property/function syntax for each numeric type. Apply the established project conventions proactively when reviewing nearby code, not only to explicitly reported lines.

Give variables, classes, and functions clear, descriptive names that convey their purpose. Balance name length with the context it provides: avoid ambiguous abbreviations, but do not repeat information already clear from the surrounding scope.

Make protocol units explicit in names (for example, raw u16 commission versus a decimal commission rate). Use named protocol constants or standard numeric bounds instead of unexplained literals, and keep unit conversions in focused helpers. Preserve wire-field names at the API/storage boundary; use descriptive domain terminology beyond it.

Keep domain invariants in their owning types. Do not repeat constructor range checks in callers; handle construction failures at the input boundary according to the existing error policy, preserving whether invalid entries are skipped or the whole input is rejected.

Separate class property declarations with a blank line, following existing ViewModel references. Apply this to both single-line properties and multi-line flow declarations.

Put each successive call in a method chain on a new line. Separate fetching a remote response from transforming its contents. Extract nested transformations and error handling into focused, named functions. Use the existing `mapAsync` helper for concurrent collection mapping instead of manually combining `async` and `awaitAll`; preserve required concurrency limits and cancellation propagation.

Do not repeat filtering already guaranteed by a backend resource contract. Verify that contract before removing a filter; keep client-owned validation, such as address decoding and on-chain eligibility checks.

Model finite UI choices with enums or sealed types instead of magic integers. Keep semantic options such as `MAX` distinct from numeric amounts; do not encode them as a sentinel amount such as zero or rely on enum ordinals.

Refactor to solve a concrete correctness, ownership, consistency, duplication, testability, or complexity problem, not personal stylistic preference. Inspect existing implementations first. Keep authoritative state in one place and derive dependent values; inject dependencies rather than constructing them inside consumers. Distinguish unavailable data from zero or empty data, and do not disguise mocks or incomplete integrations as working functionality.

Prioritize operation correctness, then state and error handling, then architecture and duplication, then readability. For each change, identify the problem, the intended improvement, and the verification. Separate behavior changes from behavior-preserving refactoring. Avoid unrelated cleanup and speculative abstractions in `common`.

Keep read and mapping methods free of hidden workflow state changes. Pass an explicit wallet, validator and amount context into fee estimation and preparation rather than relying on the last observer's cached details. Associate asynchronous results with their originating request and invalidate stale presentation as soon as that request changes.

Prefer named models for meaningful requests and load results over positional `Pair`/`Triple` values. Use scope functions for short transformations; keep validation, mutation and operation sequencing explicit. Do not manufacture zero-valued data merely to call a formatter and erase the result afterwards.

Share subscriptions, decoding, and loading when multiple consumers need different parts of the same runtime response. Key shared streams by chain and account identity, bind their lifetime to active consumers, and keep dashboard and screen state derived from the same source. Keep recovery independent for unrelated reads, and keep screen-specific sources out of screens that do not consume them.
