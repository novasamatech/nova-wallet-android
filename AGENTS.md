# Android: established conventions

## UI reuse

Before creating a component or formatting implementation, inspect analogous screens and shared components. Use existing balance formatters rather than raw decimal strings. Match placeholders to the data type: an address identicon is not a fallback for a token or chain logo. For a title-and-search header, check `common/view/TitledSearchToolbar` first.

When applying TextAppearance during view binding, preserve the text's semantic color: use the existing Secondary style variants for captions and fiat values. A base text appearance can overwrite the color declared in layout XML; check the final bound style, not only the XML attributes. Validator labels and Est. APY captions remain secondary while only the percentage uses its positive or negative color.

Use injected `FiatFormatter` for fiat amounts, not deprecated `formatAsCurrency` extensions. Read native-token symbols and precision from the actual chain asset; never hardcode display tickers in Kotlin, runtime XML text, or string resources. This includes rates, amount presets, currency switches, and descriptions; pass symbols as formatting parameters. Subnet-token symbols come from subnet metadata, not the native asset.

Define chart-formatting extension points as named interfaces alongside the shared chart component rather than exposing formatting lambdas. Keep feature-specific formatting implementations outside the shared chart code.

Reuse `ExternalActions.showAddressActions` and `setupExternalActions` for address information: they provide the standard sheet with copying and explorer actions. In selection lists, the information action must not also select the row or confirm the selection.

Use the standard `RadioButton` with `wrap_content` dimensions, following existing selection screens. Fixed bounds copied from a design can clip the theme drawable; do not shrink the control's bounds to match the visible circle.

## String resources and localization

Keep string resources in `strings.xml`, not feature-specific resource files. During feature development, add source strings only to `common/src/main/res/values/strings.xml`. Do not add manual translations to locale-specific resources: translations are handled by the established pre-release pipeline.

## Tables inside cards

When a `TableView` is inside a card with its own background, disable the table background with `app:showBackground="false"`. Setting `android:background` is insufficient because the component overrides it during construction. Do not introduce nested containers absent from the design.

Use `TableView` to own row separators, including expandable row groups, so the final visible row has no trailing divider. A plain `LinearLayout` does not disable `TableCellView` legacy dividers; `shouldDrawDivider` alone does not hide a divider that was already made visible during attribute initialization.

## Sectioned lists

Sections belonging to one selection list must scroll together in one `RecyclerView`, including their headers and rows. Do not pin the first section or its card above the list without an explicit requirement. Reuse `common/list/GroupedListAdapter` and `toListWithHeaders`, with separate view types for distinct row types. A search header may remain pinned.

Show initial list loading in the list content area, not above its controls. Reuse `CustomPlaceholderAdapter` and existing shimmer shapes for skeleton rows; replace placeholders with the real grouped content when loading completes, and stop showing them on failure.

The manual Bittensor picker is a flat list: show Stake to root immediately, then subnet rows or their shimmer placeholders, separated by 16dp. Keep them in the same scrolling RecyclerView; do not add section headers or a Where to stake shortcut. Market summaries load independently of the runtime catalogue.

## Screen navigation arguments

Staking deep links carry an optional chain-asset target in the dashboard Fragment arguments. Wait until that asset is present and its staking sync has settled before deciding between existing-position details and starting staking. Consume the target before navigation and cancel pending automatic navigation when the user chooses a destination manually.

Follow existing screen navigation patterns: expose typed `Parcelable` payloads in router methods, build arguments through the destination Fragment's `getBundle(payload)`, and read the payload in that Fragment before binding it into the screen DI component. Do not scatter argument keys or parse individual Bundle values in navigators and DI modules. Pass operation inputs between screens in fragment payloads, not feature-scoped mutable draft/selection holders. Confirmation must reconstruct its operation from its own arguments, including the explicit wallet, amount, and validator. Pass selector inputs in its fragment arguments as well; keep the established communicator for returning the selection result. Repository caches and screen-local editable/execution state are not navigation payloads.

Preserve navigation XML organization: keep the leading include block together, group global actions with actions and related destinations together, and do not prepend feature nodes ahead of includes. Match neighboring XML indentation, place attributes on separate lines, and separate sibling declarations with blank lines.

## Operation confirmation

Bittensor presents Nova commission as a percentage disclosure on confirmation, not as a separate token or fiat amount on amount-entry, execution or result screens. Keep the exact fee in transaction calculations and carry the quoted commission rate in navigation payloads; do not reconstruct the configured percentage from a rounded fee amount. Use the Swap rate label and Bittensor-specific information sheets with actual symbols, slippage and quoted commission.

Sale confirmation orders trade details before the wallet/account card. Keep the network fee with the trade parameters, retain separators between all rows, and show You will earn as signed, color-coded profit with an optional fiat estimate. Like iOS, this estimate deducts Nova commission but is before the network fee. Average purchase price comes from successfully loaded history; show the per-subnet-token caption and distinguish no purchases from unavailable or incomplete history. Account operation-history requests accept only accountSubject and page; page size is server-owned, even though the response reports pageSize.

Operation confirmation does not support switching wallets from another screen. Preserve the explicit-wallet operation contract without introducing UI for that unsupported scenario.

Use `FeeLoaderMixinV2` for transaction fee loading, presentation, and retry handling, following existing `createDefault`/`createDefaultBy` and `connectWith` usages. Do not build a parallel fee-loading state machine in a feature ViewModel. Keep estimates associated with their operation inputs, and use the same loaded fee for display and validation. Retrying a fee estimate must not silently replace a confirmed quote, amount, or price limit.

Fee estimation and submission must use the same call builder, including service fees and batch mode. Do not replace confirmed amounts or protective price limits with a fresh quote just before signing. Bind confirmation data and signing to the explicit operation wallet, not the globally selected wallet. Do not reject an operation merely because the selected meta-account changed. Recheck that operation wallet's available balance and applicable permissions before submission. Do not substitute a root validator for a subnet validator without explicit configuration and eligibility checks in that subnet.

Bittensor root partial unstaking follows the iOS loaded-fee policy: estimate the gross requested amount once, then subtract that loaded fee from the call amount only when the fee is funded from stake. Here, using the same call builder does not mean identical amount arguments before the fee is known. Keep the requested gross debit and remaining-stake calculation unchanged, freeze the prepared call for submission, and revalidate the fee source. Do not introduce iterative fee or maximum-amount estimation for this flow. Full exits and grouped call slices remain unchanged.

Before extending shared transaction APIs or introducing feature-specific submission tracking, inspect existing transfer, swap, and staking flows. Reuse their established submission and error-handling path. Cross-cutting reliability changes belong in shared infrastructure, not feature-specific signing callbacks or persistent pending locks.

Bittensor uses the standard `submitExtrinsicAndAwaitExecution` path, like on-chain swaps. Do not add a durable submission journal, finalized-block recovery scanner, or wallet-wide pending lock without an explicit requirement. An unknown dispatch outcome must not be presented as success; do not promise automatic outcome recovery when none is implemented.

The explicitly approved root-claim exception is a process-local, 15-minute duplicate guard keyed by chain, coldkey and validator hotkey. Acquire atomically at submission handoff without an `onSigned` callback; this conservatively marks earlier than iOS signing. Clear on proven non-submission, dispatched failure, success, or a fresh on-chain snapshot showing that target is no longer eligible. Retain unknown outcomes and cancellation after executor handoff. Stale snapshots must not settle claims. Keep mutation in explicit acquire, finish and fresh-reconciliation methods; pending reads only derive active entries and expiry. Do not persist this guard, widen it to a wallet lock, or enable retry on an unknown-operation result screen.

Implement each validation rule as a separate Validation with one responsibility; keep data loading outside rule logic and let ValidationSystem compose the rules. Reuse existing amount and balance validations. Use the existing Validation/ValidationSystem and ValidationExecutor pipeline for expected transaction validation failures and warnings. Map failures through the established validation UI formatters; do not route them through custom exceptions or separate confirmation-dialog mechanisms. Revalidate time-sensitive data when continuing after a warning.

## Independent loading on one screen

Keep loading, missing data, and failed reads distinct in Bittensor presentation. Preserve loading states through formatting and use existing Android loading components while requests are pending; an em dash is a settled unavailable value, not a loading placeholder. Reset account-dependent loading on wallet changes. Hide optional values when the corresponding iOS presentation hides them, rather than displaying placeholder rows. Keep retries and unrelated field loading independent.

Load interactor data with `flowOf { ... }` or an interactor-provided Flow and share it with `shareInBackground()` when reused. Derive loading and presentation values with the established flow operators instead of copying them into `MutableStateFlow` from `init`. Keep mutable flows for user-editable state and explicit operation/retry controls, not redundant copies of loaded data. Name selections after the selected entity. Keep entity lookup by ID in the interactor rather than filtering its catalogue in the ViewModel.

Use `withSafeLoading()` only when its loading state is consumed by the UI, including disabling actions while data is loading. Do not add loading wrappers solely to catch exceptions; use `catch` with appropriate error reporting when no loading state is needed. Retry infrastructure must not impose UI loading states; apply loading-state transformations at the consuming ViewModel, per attempt when retries must reset the UI.

Use `mapNotNull` when a derived flow should emit only available values instead of forwarding null and checking it in every consumer. Preserve nullable emissions when null explicitly clears stale UI data, selection, or operation readiness during loading or failure.

Independent data, such as wallet positions and a market chart, must have independent sources, error states, and retries. Failure of one load must not hide another load's successful data. When converting an exception into an error or stale state, log diagnostic context and the cause without secrets or personal data.

Use the established retry dialog for failed screen reads instead of adding inline error/retry controls. Preserve automatic recovery of reactive sources and distinguish stale-data warnings from terminal loading errors.

Derive staking and dashboard updates from reactive balance or on-chain state sources. Do not introduce a feature-specific event bus or call dashboard refresh methods after submitting a transaction. Keep complex input flows in named private methods so that `combine` only assembles their results. Do not wrap `ChainRegistry.getChain` in a dedicated timeout; chain lookup uses the existing registry lifecycle.

External staking balances belong to the existing wallet balance update lifecycle, not only a staking screen subscription. Key valuations by the explicit wallet and asset. Persist an initial partial valuation with its completeness marker, but never replace a stored complete value with incomplete or stale data. Replace partial and complete entries atomically so they cannot be counted twice. Fresh confirmed empty holdings must clear both entries. Keep portfolio totals nullable until fully priced. Reuse shared position streams so dashboard, asset balances and details do not duplicate runtime subscriptions. Include positive external balances in the existing automatic asset-discovery path, respecting disabled discovery and explicit visibility choices.

## Remote API models

Model known JSON API and configuration contracts with typed remote DTOs returned directly by Retrofit. Keep domain mapping and semantic validation in repositories or mappers; do not traverse `JsonObject`, `JsonElement`, or untyped maps for responses with a known schema. Reserve raw JSON for genuinely dynamic payloads.

Use `map...` and `...Mapping`/`...Mapper` names for transformations between DTOs and domain models. Reserve `bind...` and `...Binding` names in data conversion code for SCALE/runtime decoding, not JSON DTO mapping. Keep validation-only helpers named `require...` or `validate...`.

Keep Retrofit API interfaces and their remote DTOs in separate files from repository implementations, following the existing API/remote-model/repository structure.

Validate response pagination against the requested page and the endpoint contract before treating history as complete. A skipped page is not a complete history. Admit responses to caches only after structural and semantic validation; compute monotonic cache deadlines from request start so transport latency does not extend freshness.

Analytics consent controls analytics collection, queued events and uploads, not the lifetime or availability of authentication shared with other features. Opt-out must cancel analytics work and clear its queue without disabling unrelated authenticated requests.

Nova infrastructure uses the explicitly selected profile-3 bearer transport. Profile-2 implementations and vectors are legacy compatibility code, not a fallback for profile-3 errors. Keep registration identity, key aliases and preferences separate between profiles and bind every bootstrap operation to one exact configured HTTPS origin.

Persist opaque profile-3 bearers encrypted with an Android Keystore AES key under no-backup storage before first use. Derive deadlines from issuance-start elapsed realtime and server lifetime, with a safety margin and boot identity; never revive a token using the calendar clock. Coalesce issuance in a supervised owner scope so one cancelled waiter cannot cancel other requests. Bootstrap uses its own dispatcher, without credentials/body logging, redirects or proof retries.

Renew a rejected bearer only on the contract's complete invalid-token signal, with at most one replay of a replayable business request. Compare token, scope and generation before invalidation; never delete a newer bearer after a late rejection. Honor rate/backoff cooldowns without resetting registration or switching protocols. Profile 3 does not bypass Play Integrity device/app/certificate checks.

Distinguish optional presentation fallbacks from transaction inputs. Missing logos may use a placeholder, but a failed commission-config request must not silently supply a default fee. A documented default for an absent or invalid field in a successfully decoded config is a separate case.

## Analytics event declarations

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

## Runtime response decoding

Use the SDK's `AccountId` alias for raw Substrate account identifiers in signatures and explicitly typed properties, not `ByteArray`. Preserve meaningful domain names such as `coldKey` when they clarify the account's role; changing the type does not require renaming the identifier. Keep protocol-specific wire field names unchanged. Keep `AccountIdKey` for content-based equality and collection keys, and reserve `ByteArray` for actual binary payloads rather than account IDs.

For named metadata structures, the SDK normalizes snake_case fields to camelCase. Bindings must read normalized names. Tests must use the structure returned by the decoder rather than the original Rust field names.

Respect Android minSdk when converting numbers: a passing JVM test does not prove a JDK method exists on the device. Do not use `BigInteger.intValueExact()` on older Android versions without confirmed support. Check the range first, then use `toInt()` to prevent overflow.

## Bittensor validator selection

Persist subnet identity as netuid plus registration block, not the reusable netuid alone. Legacy favorites without registration evidence must not silently attach to the current incarnation. Preserve unresolved records until an explicit user selection replaces them; keep the existing wallet scope unless a settings-scope migration is explicitly agreed.

Keep user-facing validator sorting separate from eligibility and recommendation policy. Offer APY (the default), reported stake, and name in the filter sheet, following the current iOS contract. Show APY as a trailing value, not in the commission subtitle. Validator search is always visible in the standard TitledSearchToolbar, matching names or full addresses case-insensitively and without diacritics. Keep only the filter action in its toolbar; do not add a separate search toggle. Shorten address-only selection labels with the shared ellipsizeAddress helper. Use fresh reported yields, put missing rates and stakes last, and never substitute zero for missing data. Pin the preselected validator and retain selected/preselected validators when the default Hide unusual APY filter is enabled. That filter uses attested recommendation membership, not an invented numeric APY threshold; recommendations must not override the chosen sort. Preserve readable disabled directory rows during recommendation recovery without making them selectable. Keep the standard address-actions sheet.

Pool/Age quick pick draws once from the top ten qualifying subnets in a complete runtime catalogue. Preserve the selected subnet and its registration block across retries; do not silently reroll unavailable or re-registered targets. Check trading availability and a positive current price before navigation. Manual selection or leaving the screen invalidates pending quick-pick results.

Use the shared iOS-compatible Bittensor configuration for the commission rate and subnet logos. Root-validator defaults come from eligible existing positions or attested recommendations, not a separate fixed-address configuration.

Load the validator directory and filtering policy through attested infrastructure endpoints, not directly from tao.app. Do not invent local `maxTake` values or fallback eligibility policies when the backend is unavailable. Check UID, permit, take, and activity on-chain at one block hash using batched queries; revalidate the selected validator before submission. Backend names and optional statistics do not replace on-chain eligibility checks.

Apply the attested policy's permit and activity requirements conditionally, matching the iOS contract; do not silently override explicit flags with a stricter local policy. UID and commission validation remain mandatory. Recommendation failure may leave directory information readable, but must not invent eligibility gates or selectable recommendations.

Keep snapshot loading separate from validator mapping, and run independent storage reads concurrently within the same block-bound query context. Use `BittensorNetuid.Root` and `BittensorNetuid.Subnet` to distinguish root from dynamic subnets. APIs that only support dynamic subnets must accept `BittensorNetuid.Subnet`, not the base type or a raw integer. Use `isRoot()` instead of spreading root-ID comparisons through callers, and convert raw IDs at the boundary.

Generate a validator's icon from its hotkey with `AddressIconGenerator`, as for other addresses in the app. A chain logo or generic placeholder does not replace an address identicon. Discovery details include an independently loaded Available validators preview with compact, color-coded APY and access to the existing selector; amount entry also supports selection. Keep amount presets and potential-receive previews out of discovery details. Pass the chosen validator explicitly into amount entry and confirmation. Eligibility revalidation must not silently substitute another validator.

Discovery details must not load validator eligibility or wait for it to enable Use this subnet. Amount entry resolves its initial validator when none was explicitly supplied. Missing optional runtime hold values mean zero according to the chain/iOS contract; disable storage-default decoding for optional reads and apply explicit field-specific fallbacks instead of decoding an Option sentinel as a number.

## Self-review and refactoring

Keep responsibilities separated when a class spans several workflows: data sources own transport and pagination; repositories own data access and caches; interactors own wallet-context orchestration and financial calculations; presentation owns UI loading states and formatting. Do not move an overloaded repository wholesale into a mixin. Preserve cache keys, invalidation, incomplete-data semantics, and cancellation when extracting responsibilities. Existing shared contracts should be assessed in context rather than replaced solely because of their type names.

Use existing numeric sign extensions (`isPositive`, `isNegative`, `isZero`, `isNonNegative`, and `isNonPositive`) instead of inline `signum()` comparisons or local duplicates. Respect their existing property/function syntax for each numeric type. Apply the established project conventions proactively when reviewing nearby code, not only to explicitly reported lines.

Give variables, classes, and functions clear, descriptive names that convey their purpose. Balance name length with the context it provides: avoid ambiguous abbreviations, but do not repeat information already clear from the surrounding scope.

Make protocol units explicit in names (for example, raw u16 commission versus a decimal commission rate). Use named protocol constants or standard numeric bounds instead of unexplained literals, and keep unit conversions in focused helpers. Preserve wire-field names at the API/storage boundary; use descriptive domain terminology beyond it.

Keep domain invariants in their owning types. Do not repeat constructor range checks in callers; handle construction failures at the input boundary according to the existing error policy, preserving whether invalid entries are skipped or the whole input is rejected.

Separate class property declarations with a blank line, following existing ViewModel references. Apply this to both single-line properties and multi-line flow declarations.

Order class members as properties, init blocks, then functions. Do not interleave methods or init blocks with property declarations. When moving init blocks, check initialization dependencies and side effects first; preserve property initialization order and the relative order of multiple init blocks.

For calls with multiple arguments that include a lambda or are followed by a trailing lambda, put each parenthesized argument on its own line. Keep the closing parenthesis on a separate line; do not compress such calls just because they fit the line-length limit.

Put successive transformation calls in a method chain on separate lines when this improves readability. Keep short expressions, simple property access, and short predicate calls together; do not mechanically split expressions such as `finalizedBlock(chainId).number` or `hash.equals(transactionHash, ignoreCase = true)`. Keep a short call and its arguments on one line when they fit naturally. When an `if` condition contains a multi-line collection predicate or nested transformation, extract a descriptively named Boolean before the `if` so the control flow remains immediately visible.

Separate fetching a remote response from transforming its contents. Extract nested transformations and error handling into focused, named functions. Use the existing `mapAsync` helper for concurrent collection mapping instead of manually combining `async` and `awaitAll`; preserve required concurrency limits and cancellation propagation.

Do not repeat filtering already guaranteed by a backend resource contract. Verify that contract before removing a filter; keep client-owned validation, such as address decoding and on-chain eligibility checks.

Model finite UI choices with enums or sealed types instead of magic integers. Keep semantic options such as `MAX` distinct from numeric amounts; do not encode them as a sentinel amount such as zero or rely on enum ordinals.

Refactor to solve a concrete correctness, ownership, consistency, duplication, testability, or complexity problem, not personal stylistic preference. Inspect existing implementations first. Keep authoritative state in one place and derive dependent values; inject dependencies rather than constructing them inside consumers. Distinguish unavailable data from zero or empty data, and do not disguise mocks or incomplete integrations as working functionality.

Prioritize operation correctness, then state and error handling, then architecture and duplication, then readability. For each change, identify the problem, the intended improvement, and the verification. Separate behavior changes from behavior-preserving refactoring. Avoid unrelated cleanup and speculative abstractions in `common`.

Keep read and mapping methods free of hidden workflow state changes. Pass an explicit wallet, validator and amount context into fee estimation and preparation rather than relying on the last observer's cached details. Associate asynchronous results with their originating request and invalidate stale presentation as soon as that request changes.

Prefer named models for meaningful requests and load results over positional `Pair`/`Triple` values. Use scope functions for short transformations; keep validation, mutation and operation sequencing explicit. Do not manufacture zero-valued data merely to call a formatter and erase the result afterwards.

Share subscriptions, decoding, and loading when multiple consumers need different parts of the same runtime response. Key shared streams by chain and account identity, bind their lifetime to active consumers, and keep dashboard and screen state derived from the same source. Keep recovery independent for unrelated reads, and keep screen-specific sources out of screens that do not consume them.

## Bittensor position actions

Grouped exits freeze a deterministic per-validator withdrawal plan in the confirmation payload. A partial withdrawal may span several validators; derive every slice and Max from that validator's current hold, owner and collateral constraints. Revalidate membership/order, total drift and availability before signing, and use the same planned calls and atomic batch mode for fee estimation and submission. Batched exits require free balance for the network fee. Never silently enlarge a requested amount to remove dust: use the standard validation warning and explicit Unstake all action.

Keep the Where to stake carousel separate from the manual subnet picker. APY quick pick ranks subnets by the maximum known yield among their recommended validators, not every validator. All quick-pick orders exclude thin pools and choose from the top ten eligible subnets. Missing yields are not zero; preserve partial successful loads, but report a failed load when no usable rates remain. Never hardcode illustrative yield percentages as live product claims.

Value subnet portfolio positions using fresh prices from the attested backend catalogue first. For missing prices of held subnet IDs only, use the batch runtime alpha-price fallback; never replace an available fresh backend price. Keep transaction quotes and execution validation on-chain; portfolio pricing must not change the reviewed trade terms. Missing or failed prices from both sources leave holdings visible with an unknown valuation, not a zero balance. Publish holdings and their loading/stale status independently of pricing; a price response must value the latest same-account positions without changing their status.

The portfolio chart uses the attested backend account portfolio history, not today's holdings repriced at historical subnet prices. This is partial provider-reported history, not a complete wallet ledger. Preserve backend window and freshness metadata; display stale history with an explicit warning, and never turn failures into successful empty data. Use completed historical points and append a live anchor only from fully priced current holdings. Retain each selected point's reported native amount rather than deriving it from fiat. Offer only the backend day/week/month/quarter presets, match non-USD FX samples using period-specific tolerances, and hide period returns when the server window lacks coverage. Account, currency and period changes invalidate old results; balance changes recalculate the live anchor without reloading unchanged account-history requests.

For Bittensor native transfer history, follow the explicitly approved iOS commission-recipient policy: hide every successful transfer to the current or historical Nova fee recipients, including standalone transfers. Do not require a matching staking call for this recipient rule. Preserve failed transfers and other assets; keep subnet-account technical-transfer classification separate.

Invalidate cost-basis history after a submitted own subnet buy/sell with a successful or unknown outcome, using the account and infrastructure origin captured for that operation. Passive reward compounding must not invalidate or cancel history loads. During the post-trade settlement window, fetched results may be displayed but must not seed a later subscription's cache. Keep account/origin isolation and reject results from obsolete generations.

Root reward summaries show all redeemable rewards, while claim eligibility is evaluated per validator. Do not show inline minimum-claim, available-to-withdraw or statistics-unavailable captions on root position details. Keep claim thresholds and withdrawal restrictions in domain validation. Where a minimum is presented in the claim flow, use redeemable (not accrued) rewards and round the displayed minimum upward. Root stake holds restrict withdrawals, not additional stake.

Root and subnet position screens share the primary gradient background and transparent toolbar. Present validator APY using the shared compact percentage formatter, with positive/negative colors and independent loading. Do not let long fractional rates squeeze validator names. Place subnet Buy/Sell actions in the shared rounded, bordered bottom-button container. The root hold card uses the runtime interval and earliest remaining group hold, not a fixed duration or the longest validator hold; hide it when any group member is unlocked.

Keep a root position visible when it has redeemable rewards even if its stake is zero. Dashboard classification and portfolio empty states must preserve access to those rewards. An Active badge describes on-chain registration, not a positive stake amount.

Subnet buy/sell execution uses the same swap-style screen for progress, success, and failure; do not open the root-staking result sheet for subnet failures. Keep operation details visible on failure. Try again returns to review rather than silently resubmitting, and unknown transaction outcomes must not offer retry.

Keep root staking and subnet trading as separate flows. Root positions use Add stake and Unstake. Subnet positions have their own position-details screen with Buy more and Sell actions; do not route an existing subnet position to the discovery screen or label its trading flow as root staking. Use the corresponding Figma amount, confirmation, waiting, and result frames.

Adding root stake from an existing position must pass its explicit wallet and validator in a typed screen payload. Keep that validator fixed and open standard address actions on tap; a fresh staking flow may still select a validator. If the fixed validator is no longer eligible, report that condition rather than silently switching to a recommendation.
