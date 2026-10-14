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

Fee estimation and submission must use the same call builder, including service fees and batch mode. Do not replace confirmed amounts or protective price limits with a fresh quote just before signing. Recheck the selected wallet, available balance, and applicable permissions before submission. Do not substitute a root validator for a subnet validator without explicit configuration and eligibility checks in that subnet.

Before extending shared transaction APIs or introducing feature-specific submission tracking, inspect existing transfer, swap, and staking flows. Reuse their established submission and error-handling path. Cross-cutting reliability changes belong in shared infrastructure, not feature-specific signing callbacks or persistent pending locks.

## Independent loading on one screen

Load interactor data with `flowOf { ... }` or an interactor-provided Flow and share it with `shareInBackground()` when reused. Derive loading and presentation values with the established flow operators instead of copying them into `MutableStateFlow` from `init`. Keep mutable flows for user-editable state and explicit operation/retry controls, not redundant copies of loaded data. Name selections after the selected entity. Keep entity lookup by ID in the interactor rather than filtering its catalogue in the ViewModel.

Use `withSafeLoading()` only when its loading state is consumed by the UI, including disabling actions while data is loading. Do not add loading wrappers solely to catch exceptions; use `catch` with appropriate error reporting when no loading state is needed.

Use `mapNotNull` when a derived flow should emit only available values instead of forwarding null and checking it in every consumer. Preserve nullable emissions when null explicitly clears stale UI data, selection, or operation readiness during loading or failure.

Independent data, such as wallet positions and a market chart, must have independent sources, error states, and retries. Failure of one load must not hide another load's successful data. When converting an exception into an error or stale state, log diagnostic context and the cause without secrets or personal data.

Derive staking and dashboard updates from reactive balance or on-chain state sources. Do not introduce a feature-specific event bus or call dashboard refresh methods after submitting a transaction. Keep complex input flows in named private methods so that `combine` only assembles their results. Do not wrap `ChainRegistry.getChain` in a dedicated timeout; chain lookup uses the existing registry lifecycle.

## Remote API models

Model known JSON API and configuration contracts with typed remote DTOs returned directly by Retrofit. Keep domain mapping and semantic validation in repositories or mappers; do not traverse `JsonObject`, `JsonElement`, or untyped maps for responses with a known schema. Reserve raw JSON for genuinely dynamic payloads.

## Runtime response decoding

For named metadata structures, the SDK normalizes snake_case fields to camelCase. Bindings must read normalized names. Tests must use the structure returned by the decoder rather than the original Rust field names.

Respect Android minSdk when converting numbers: a passing JVM test does not prove a JDK method exists on the device. Do not use `BigInteger.intValueExact()` on older Android versions without confirmed support. Check the range first, then use `toInt()` to prevent overflow.

## Bittensor validator selection

Use the shared iOS-compatible Bittensor configuration for the commission rate and subnet logos. Root-validator defaults come from eligible existing positions or attested recommendations, not a separate fixed-address configuration.

Load the validator directory and filtering policy through attested infrastructure endpoints, not directly from tao.app. Do not invent local `maxTake` values or fallback eligibility policies when the backend is unavailable. Check UID, permit, take, and activity on-chain at one block hash using batched queries; revalidate the selected validator before submission. Backend names and optional statistics do not replace on-chain eligibility checks.

Generate a validator's icon from its hotkey with `AddressIconGenerator`, as for other addresses in the app. A chain logo or generic placeholder does not replace an address identicon. Explicitly pass the validator selected before amount entry into confirmation. Eligibility revalidation must not silently substitute another validator.

## Self-review and refactoring

Use existing numeric sign extensions (`isPositive`, `isNegative`, `isZero`, `isNonNegative`, and `isNonPositive`) instead of inline `signum()` comparisons or local duplicates. Respect their existing property/function syntax for each numeric type. Apply the established project conventions proactively when reviewing nearby code, not only to explicitly reported lines.

Give variables, classes, and functions clear, descriptive names that convey their purpose. Balance name length with the context it provides: avoid ambiguous abbreviations, but do not repeat information already clear from the surrounding scope.

Separate class property declarations with a blank line, following existing ViewModel references. Apply this to both single-line properties and multi-line flow declarations.

Model finite UI choices with enums or sealed types instead of magic integers. Keep semantic options such as `MAX` distinct from numeric amounts; do not encode them as a sentinel amount such as zero or rely on enum ordinals.

Refactor to solve a concrete correctness, ownership, consistency, duplication, testability, or complexity problem, not personal stylistic preference. Inspect existing implementations first. Keep authoritative state in one place and derive dependent values; inject dependencies rather than constructing them inside consumers. Distinguish unavailable data from zero or empty data, and do not disguise mocks or incomplete integrations as working functionality.

Prioritize operation correctness, then state and error handling, then architecture and duplication, then readability. For each change, identify the problem, the intended improvement, and the verification. Separate behavior changes from behavior-preserving refactoring. Avoid unrelated cleanup and speculative abstractions in `common`.
