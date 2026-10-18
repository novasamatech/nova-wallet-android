# Bittensor architecture review

Snapshot: 2026-10-18, HEAD `96f0a8a7f`, comparison `bittensor_base` (`e8e779501`) to HEAD, plus the local estimates refactoring.

## Scope and limits

The comparison contains 501 non-deleted changed files, including 372 main-source Kotlin files. It includes merged development work outside Bittensor. All changed Kotlin files were screened for responsibility size and cross-layer dependencies; candidate classes and their consumers were inspected in detail. This is an architecture review, not a claim that every line, runtime scenario, or financial behavior was exhaustively verified. Existing user edits to `RealExtrinsicService.kt` and `feature-staking-impl/build.gradle` were preserved.

Paths below are relative to `feature-staking-impl/src/main/java/io/novafoundation/nova/feature_staking_impl/` unless stated otherwise.

## Fixed: estimates ownership

Removed `data/bittensor/BittensorEstimatesRepository.kt`, which combined transport, paging, caching, wallet observation, cost-basis orchestration, and UI loading states.

- `BittensorEstimatesRemoteSource`: HTTP requests, freshness checks and bounded pagination.
- `BittensorOperationHistoryRepository`: operation-history cache and balance-revision invalidation.
- `BittensorAlphaYieldRepository`: yield cache and mapping.
- `BittensorCostBasisInteractor`: explicit wallet context, balance-triggered requests and cost-basis construction.
- `BittensorEstimatesMixin`: optional-estimate loading/error presentation shared by five screens.

Dependencies are created separately in DI and injected. Preserved page caps, response cache lifetime, independent caches, incomplete-history semantics, and optional estimate failures. No transaction or fee behavior was intentionally changed.

Verification: application Kotlin compilation and 289 Bittensor unit tests passed (zero failures/errors). This does not establish device, live-backend, or transaction behavior.

## Remaining findings

### 1. Root rewards belong outside the validator catalogue

`data/bittensor/BittensorValidatorBackend.kt:54` loads root claim history and sums payouts while holding the same mutex used for validator requests. Its caller is `domain/bittensor/BittensorRootClaimInteractor.kt`.

Extract a root-reward history data source/repository and keep payout aggregation separate from validator catalogue policy. Preserve deduplication, freshness and the current partial-history meaning. This also avoids serializing unrelated rewards and validator requests. Not changed in this pass.

### 2. Root repository owns default-validator selection policy

`data/bittensor/BittensorStakingRepository.kt:109` chooses a default validator using existing stake, cached selection and preferred-validator lookup, alongside wallet/position observation and APY retrieval.

Move default-validator selection and its lifetime into a domain collaborator. Retain data subscriptions in the repository; preserve selection stability and independent failure handling. Data aggregation alone is not a reason to split a repository. Not changed in this pass.

### 3. Subnet details combines chart retrieval and purchase projection

`domain/bittensor/BittensorSubnetDetailsInteractor.kt:39` estimates purchased alpha, while `history` at line 47 loads and converts chart history. These workflows have different inputs and dependencies.

Separate price-history retrieval from purchase projection so each can evolve and be tested independently. Retain established shared chart contracts. Not changed in this pass.

### 4. Amount/review ViewModels duplicate projection and formatting

`presentation/bittensor/subnet/BittensorSubnetAmountViewModel.kt` and `BittensorSubnetReviewViewModel.kt` repeat annual/monthly yield and projected average-price handling. `presentation/bittensor/exit/BittensorExitAmountViewModel.kt` and `BittensorExitReviewViewModel.kt` repeat average-price and estimated-profit presentation.

Extract shared financial projection inputs/results into domain logic and reusable result formatting into presentation. Keep screen orchestration and editable state in ViewModels; avoid replacing them with a generic all-purpose utility. Not changed in this pass.

### 5. Dashboard continues accumulating staking-type branches

`presentation/dashboard/main/StakingDashboardViewModel.kt:180` selects Bittensor-specific statistics and has type-specific navigation. This is an extensibility concern rather than a proven functional defect.

Consider the existing staking-type extension patterns before adding a type-specific presentation/navigation strategy. Lower priority: do not introduce a new registry solely to remove a small conditional. Not changed in this pass.

### 6. Merged development code couples analytics consent to shared attestation

Outside the staking module: `analytics/src/main/java/io/novafoundation/nova/analytics/transport/RealAnalyticsService.kt:49` calls `setClientCreationAllowed(value)` on the shared attestation service. `infrastructure/src/main/java/io/novafoundation/nova/infrastructure/attestation/ClientAttestationService.kt:175` checks that gate before permitting even an existing attested identity. `infrastructure/.../di/AttestationModule.kt` provides this service at application scope and injects it into the attested network client used by Bittensor.

Analytics opt-out therefore controls authentication capability beyond analytics. Separate telemetry consent/deletion policy from permission to authenticate unrelated services. This needs a dedicated behavior/privacy decision and integration test; it was not silently changed as part of the estimates refactor.

## Not sufficient reasons to refactor

Large DI modules, navigation implementations, event variants, and existing shared chart loading-state contracts are not automatically architectural problems. Splitting must isolate a responsibility, dependency or policy, not merely reduce line count.
