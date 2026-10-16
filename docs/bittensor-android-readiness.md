# Bittensor Android readiness

Implementation checkout: `codex/bittensor-subnet-waiting` in the isolated `android-subnet-waiting` worktree. The primary Android checkout is not modified by this work.

## Implemented

- Root Add stake and Unstake, including amount entry, immutable confirmation, runtime withdrawal constraints, and fee funding checks.
- Dedicated subnet position details with Buy more and Sell. Both trades use the corresponding Figma amount, confirmation, waiting, and completed states. The existing swap timer and asset views are reused.
- Root validator selection and infrastructure recommendations for subnet validators. On-chain eligibility is checked again before submission; a confirmed validator is never silently replaced.
- Root APY from the infrastructure's net annual percentage. Alpha APY, monthly projections, average purchase price, and profit follow the current iOS interpretation and are marked as estimates.
- Root Claim using on-chain basket previews and an explicit hotkey. The standard Nova confirmation is used because no Claim frame was supplied. Other claimable hotkeys remain separate claims.
- Reported root rewards from the latest infrastructure claim-history page. The UI identifies this as partial reported history rather than a lifetime total.
- Subnet safety information from registration height, actual pool liquidity, and observed monthly price history/range. Missing information remains unavailable.
- Shared account-layer recovery with durable transaction state persisted before broadcast. Finalized block scans distinguish success, failure, and proven expiry. Lost connectivity, cancellation, and incomplete evidence keep the wallet locked against another operation. Proxy execution results are checked separately from outer System success.

## Data references

Infrastructure checkout verified at `369f64a`; the contract is `nova-infrastructure/bittensor/contracts/v1/openapi.json`.

Recommendations use `/recommendations/subnets/{netuid}/validators`; root yield uses `/root/validators/{hotkey}`. Estimates use `/subnets/{netuid}/yields/alpha` and `/operations/search`; reported root rewards use `/root/claims/search`.

Root `stakeYield.h24.apy` is already net of validator take. `payoutTao` is converted from rao. Alpha `reportedRate` is interpreted as an annual percentage following iOS, as explicitly requested, although the backend contract does not yet confirm this unit. Purchase/sale classification and remaining cost basis also follow iOS heuristics over partial provider history. Ambiguous or incomplete histories produce unavailable estimates.

Figma file: [Bittensor staking](https://www.figma.com/design/MV2logXPVWjKJzwayrsetK/Bittensor-staking). Root frames: `513:15776`, `513:15803`, `513:15821`, `513:15833`. Subnet frames: `513:18395`, `513:18405`, `513:18418`, `513:18878`, `513:18901`, `513:18440`, `513:18457`, `513:18827`, `513:18850`.

## Remaining limitations

- Multisig approval/execution lifecycle is not implemented. Bittensor operations are disabled for multisig wallets; read-only positions remain available.
- Legacy pending records containing only a transaction hash cannot safely be expired automatically without the original anchor and mortality context.
- Reported claims do not establish all earned rewards, and alpha profit is an estimate rather than a complete realized-rewards ledger.
- The portfolio overview's existing chart is a TAO market-price chart with its disclaimer, not account portfolio history. Connecting the new infrastructure portfolio-history endpoint would require reconciling its maximum 90-day coverage with the current year/all-time UI periods.
- Device rendering, rotation/process-death restoration, live attested endpoints, signing, and real transaction outcomes still require device verification. JVM tests and APK assembly do not establish these results.

## Verification

- `:app:assembleDebug` passed.
- JVM tests after the refactor: 157 tests across 26 suites, zero failures (127 Bittensor tests and 30 shared recovery tests). These cover recovery/persistence/expiry, nested proxy/multisig evidence, withdrawal constraints, SCALE call encoding, claim decoding, backend recommendation and yield semantics, wallet isolation, and estimate calculations.
- Restricted ktlint check covers the 118 changed Kotlin files.
- No APK installation or financial transaction was performed on the shared device.

## Refactoring review

See [the refactor review](bittensor-refactor-review.md) for the scope, behavior changes, iOS comparison, and integration boundaries.
