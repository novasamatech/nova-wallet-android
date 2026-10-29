# Bittensor constraints

These product/domain constraints were preserved from RC AGENTS.md. They are NOT architectural examples.
Read the applicable general UI/data/transaction rules too. Do not infer wider conventions from Bittensor code.
An iOS-specific statement records an existing parity requirement, not proof of current iOS behavior:
for new parity work inspect the explicitly requested iOS revision.

## Manual picker

The manual Bittensor picker is a flat list: show Stake to root immediately, then subnet rows or their shimmer placeholders, separated by 16dp. Keep them in the same scrolling RecyclerView; do not add section headers or a Where to stake shortcut. Market summaries load independently of the runtime catalogue.

## Operation presentation and explicit exceptions

Bittensor presents Nova commission as a percentage disclosure on confirmation, not as a separate token or fiat amount on amount-entry, execution or result screens. Keep the exact fee in transaction calculations and carry the quoted commission rate in navigation payloads; do not reconstruct the configured percentage from a rounded fee amount. Use the Swap rate label and Bittensor-specific information sheets with actual symbols, slippage and quoted commission.

Sale confirmation orders trade details before the wallet/account card. Keep the network fee with the trade parameters, retain separators between all rows, and show You will earn as signed, color-coded profit with an optional fiat estimate. Like iOS, this estimate deducts Nova commission but is before the network fee. Average purchase price comes from successfully loaded history; show the per-subnet-token caption and distinguish no purchases from unavailable or incomplete history. Account operation-history requests accept only accountSubject and page; page size is server-owned, even though the response reports pageSize.

Bittensor root partial unstaking follows the iOS loaded-fee policy: estimate the gross requested amount once, then subtract that loaded fee from the call amount only when the fee is funded from stake. Here, using the same call builder does not mean identical amount arguments before the fee is known. Keep the requested gross debit and remaining-stake calculation unchanged, freeze the prepared call for submission, and revalidate the fee source. Do not introduce iterative fee or maximum-amount estimation for this flow. Full exits and grouped call slices remain unchanged.

Bittensor uses the standard `submitExtrinsicAndAwaitExecution` path, like on-chain swaps. Do not add a durable submission journal, finalized-block recovery scanner, or wallet-wide pending lock without an explicit requirement. An unknown dispatch outcome must not be presented as success; do not promise automatic outcome recovery when none is implemented.

The explicitly approved root-claim exception is a process-local, 15-minute duplicate guard keyed by chain, coldkey and validator hotkey. Acquire atomically at submission handoff without an `onSigned` callback; this conservatively marks earlier than iOS signing. Clear on proven non-submission, dispatched failure, success, or a fresh on-chain snapshot showing that target is no longer eligible. Retain unknown outcomes and cancellation after executor handoff. Stale snapshots must not settle claims. Keep mutation in explicit acquire, finish and fresh-reconciliation methods; pending reads only derive active entries and expiry. Do not persist this guard, widen it to a wallet lock, or enable retry on an unknown-operation result screen.

## Loading presentation

Keep loading, missing data, and failed reads distinct in Bittensor presentation. Preserve loading states through formatting and use existing Android loading components while requests are pending; an em dash is a settled unavailable value, not a loading placeholder. Reset account-dependent loading on wallet changes. Hide optional values when the corresponding iOS presentation hides them, rather than displaying placeholder rows. Keep retries and unrelated field loading independent.

## Validator selection

Use the role aliases in [BittensorAccountIds.kt](../../feature-staking-impl/src/main/java/io/novafoundation/nova/feature_staking_impl/domain/bittensor/BittensorAccountIds.kt) for raw coldkey/hotkey IDs in new or touched signatures. They denote public account IDs, not secret keys or SS58 address strings, and remain SDK AccountId aliases rather than distinct compiler-enforced types. Keep AccountIdKey where content-based equality is needed for collection keys.

Persist subnet identity as netuid plus registration block, not the reusable netuid alone. Legacy favorites without registration evidence must not silently attach to the current incarnation. Preserve unresolved records until an explicit user selection replaces them; keep the existing wallet scope unless a settings-scope migration is explicitly agreed.

Keep user-facing validator sorting separate from eligibility and recommendation policy. Offer APY (the default), reported stake, and name in the filter sheet, following the current iOS contract. Show APY as a trailing value, not in the commission subtitle. Validator search is always visible in the standard TitledSearchToolbar, matching names or full addresses case-insensitively and without diacritics. Keep only the filter action in its toolbar; do not add a separate search toggle. Shorten address-only selection labels with the shared ellipsizeAddress helper. Use fresh reported yields, put missing rates and stakes last, and never substitute zero for missing data. Pin the preselected validator and retain selected/preselected validators when the default Hide unusual APY filter is enabled. That filter uses attested recommendation membership, not an invented numeric APY threshold; recommendations must not override the chosen sort. Preserve readable disabled directory rows during recommendation recovery without making them selectable. Keep the standard address-actions sheet.

Pool/Age quick pick draws once from the top ten qualifying subnets in a complete runtime catalogue. Preserve the selected subnet and its registration block across retries; do not silently reroll unavailable or re-registered targets. Check trading availability and a positive current price before navigation. Manual selection or leaving the screen invalidates pending quick-pick results.

Use the shared iOS-compatible Bittensor configuration for the commission rate and subnet logos. Root-validator defaults come from eligible existing positions or attested recommendations, not a separate fixed-address configuration.

Load the validator directory and filtering policy through attested infrastructure endpoints, not directly from tao.app. Do not invent local `maxTake` values or fallback eligibility policies when the backend is unavailable. Check UID, permit, take, and activity on-chain at one block hash using batched queries; revalidate the selected validator before submission. Backend names and optional statistics do not replace on-chain eligibility checks.

Apply the attested policy's permit and activity requirements conditionally, matching the iOS contract; do not silently override explicit flags with a stricter local policy. UID and commission validation remain mandatory. Recommendation failure may leave directory information readable, but must not invent eligibility gates or selectable recommendations.

Keep snapshot loading separate from validator mapping, and run independent storage reads concurrently within the same block-bound query context. Use `BittensorNetuid.Root` and `BittensorNetuid.Subnet` to distinguish root from dynamic subnets. APIs that only support dynamic subnets must accept `BittensorNetuid.Subnet`, not the base type or a raw integer. Use `isRoot()` instead of spreading root-ID comparisons through callers, and convert raw IDs at the boundary.

Generate a validator's icon from its hotkey with `AddressIconGenerator`, as for other addresses in the app. A chain logo or generic placeholder does not replace an address identicon. Discovery details include an independently loaded Available validators preview with compact, color-coded APY and access to the existing selector; amount entry also supports selection. Keep amount presets and potential-receive previews out of discovery details. Pass the chosen validator explicitly into amount entry and confirmation. Eligibility revalidation must not silently substitute another validator.

Discovery details must not load validator eligibility or wait for it to enable Use this subnet. Amount entry resolves its initial validator when none was explicitly supplied. Missing optional runtime hold values mean zero according to the chain/iOS contract; disable storage-default decoding for optional reads and apply explicit field-specific fallbacks instead of decoding an Option sentinel as a number.

## Position actions

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
