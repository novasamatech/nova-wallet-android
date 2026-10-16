# Bittensor refactor review

## Scope

Worktree: `android-subnet-waiting`, branch `codex/bittensor-subnet-waiting`, base `8803cf20b`.

This pass refactors the earlier uncommitted implementation in this worktree. The primary `nova-wallet-android` checkout and its ongoing refactor were not modified. iOS was inspected read-only at `7eea9a401`. No commits, pushes, APK installation, or transactions were performed.

The attached Android conventions are recorded in this worktree's `AGENTS.md`.

## Correctness changes

- Amount editing immediately invalidates withdrawal preparation. Debouncing happens inside the request flow, and Continue verifies that the prepared amount and MAX mode still match the current input.
- Claim preview reloads when the selected wallet changes. Execution still checks wallet identity and current claim eligibility, available balance, redeemable amount, and estimated forfeiture before signing.
- Equal-value root Claim targets now use ascending hotkey order, matching iOS.
- Sell uses the shared configuration's commission. Its confirmation caption displays the confirmed fee amount, avoiding a fixed percentage that can disagree with the transaction.
- New position and Sell screens use configured subnet logos with the chain metadata as fallback. Native symbols and precision come from the actual asset; alpha precision is a named protocol constant.
- Withdrawals preserve the confirmed amount, price limit, service-fee transfer, and atomic call builder during execution. Revalidation does not replace the confirmed quote.

## Ownership and structure

- Transaction persistence, finalized-block scanning, expiry proof, nested dispatch evidence, and acknowledgements moved to `feature-account-api` / `feature-account-impl`. Bittensor opts into recovery through `SubmissionOptions.recoverable`; it no longer supplies a feature-specific signing callback.
- Recovery requires a stable explicit wallet and one immediately executable mortal extrinsic. Multi-extrinsic submission rejects this option; approval-only multisig calls are unsupported. Existing preference keys are retained for compatibility with previously persisted records. A legacy hash-only record is not silently cleared.
- New amount, review, position, and Claim screens have their own DI components/modules. Router methods carry Parcelable payloads; Fragments own bundle creation and argument reading.
- Added request/result models replace positional tuples for recovery, chart requests, and position actions. MAX is explicit input state rather than a numeric sentinel.
- Exit quote invariants belong to the quote model. New root/subnet operations use `BittensorNetuid`; dynamic metadata lookup requires its Subnet subtype.
- New Retrofit interfaces, remote DTOs, runtime bindings, operation models, and repositories are separated by responsibility. Raw account signatures use `AccountId`.
- Independent preflight and Claim storage reads run concurrently against one block hash and its metadata.
- Cost-basis refresh follows reactive wallet balance changes. New Exit/Claim flows no longer trigger dashboard refresh callbacks after submission.
- Optional yield/safety reads use error handling without unused loading wrappers. Chart loading is presented through the standard chart model. Fiat formatting uses injected `FiatFormatter`; chart formatting has a named shared interface and a feature-owned implementation.
- New layouts use one attribute per line; new navigation nodes are grouped with related actions and destinations. Existing base-screen layout and navigation refactoring stays in the primary branch.

## iOS comparison

| Area | Source checked | Result |
| --- | --- | --- |
| Root/subnet withdrawal calls | `SubtensorStakingOperation+Extrinsic.swift` | Root partial removal, full removal, subnet protective limits, `allowPartial = false`, and atomic service-fee transfer follow the same semantics. |
| Sell proceeds | `SubtensorTradeQuoteFactory.swift` | Minimum output uses alpha after the pool fee and the protective price. Nova fee is deducted from gross proceeds. |
| Alpha APY | `SubtensorRate.swift` | Reported annual percent is divided by 100 for projections; stale/invalid values remain unavailable. This remains an estimate under the user's accepted iOS interpretation. |
| Average purchase price and profit | `SubtensorCostBasis.swift` | Proportional remaining cost, complete exit, and ambiguous-history behavior remain consistent with the iOS heuristic. |
| Root Claim | `SubtensorRootClaimRule.swift` | Positive redeemable value, rounded-up threshold, largest claim first, and lexicographic hotkey tie-break agree. |
| Shared configuration | `ApplicationConfigs.swift` and Bittensor config model | The canonical configuration URL supplies commission and subnet logos. Fallback commission stays 0.3%; the beneficiary is unchanged. |

## Verification

- Debug APK assembly passed.
- 127 Bittensor JVM tests across 24 suites passed.
- 30 shared recovery JVM tests across 2 suites passed.
- Added tests cover configured commission, invalid configuration fallback, subnet-logo validation, and equal-value Claim ordering. Existing tests continue to cover immutable call encoding, withdrawal constraints, wallet isolation, recovery persistence/cursors/expiry, and nested dispatch evidence.
- Targeted Kotlin style check: 118 changed files.
- New XML files parse successfully; `git diff --check` passes.

## Integration boundaries and remaining verification

The pre-existing Buy quote/configuration, validator catalogue, and root-default selection architecture were intentionally left for the user's parallel baseline refactor. The new config, netuid, and chart-interface definitions match that branch's corresponding definitions and must be reconciled when merging. This review does not claim that every pre-existing screen has been migrated to the new conventions.

Device rendering, rotation/process-death restoration, live attested APIs, signing, and real transaction outcomes were not exercised. Multisig execution remains unsupported. Hash-only legacy recovery records still require additional evidence to resolve. Reported reward history is partial, alpha performance is estimated, and the existing portfolio chart still shows market price rather than account portfolio history.
