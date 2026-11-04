# Transactions and validation

## Confirmation, fees and submission

Operation confirmation does not support switching wallets from another screen. Preserve the explicit-wallet operation contract without introducing UI for that unsupported scenario.

Use `FeeLoaderMixinV2` for transaction fee loading, presentation, and retry handling, following existing `createDefault`/`createDefaultBy` and `connectWith` usages. Do not build a parallel fee-loading state machine in a feature ViewModel. Keep estimates associated with their operation inputs, and use the same loaded fee for display and validation. Retrying a fee estimate must not silently replace a confirmed quote, amount, or price limit.

Fee estimation and submission must use the same call builder, including service fees and batch mode. Do not replace confirmed amounts or protective price limits with a fresh quote just before signing. Bind confirmation data and signing to the explicit operation wallet, not the globally selected wallet. Do not reject an operation merely because the selected meta-account changed. Recheck that operation wallet's available balance and applicable permissions before submission. Do not substitute a root validator for a subnet validator without explicit configuration and eligibility checks in that subnet.

Before extending shared transaction APIs or introducing feature-specific submission tracking, inspect existing transfer, swap, and staking flows. Reuse their established submission and error-handling path. Cross-cutting reliability changes belong in shared infrastructure, not feature-specific signing callbacks or persistent pending locks.

Implement each validation rule as a separate Validation with one responsibility; keep data loading outside rule logic and let ValidationSystem compose the rules. Reuse existing amount and balance validations. Use the existing Validation/ValidationSystem and ValidationExecutor pipeline for expected transaction validation failures and warnings. Map failures through the established validation UI formatters; do not route them through custom exceptions or separate confirmation-dialog mechanisms. Revalidate time-sensitive data when continuing after a warning.

## Financial review gate

Only for Bittensor work, or shared transaction changes affecting its explicit exceptions, read
[Bittensor constraints](bittensor.md). Do not load that domain guide for unrelated transactions or generalize its exceptions.
Keep amount units, rounding, Max, fee source, existential deposit/holds and batch mode consistent
between input, display, validation, call construction and signing. Use actual chain/asset rules;
do not assume every locked balance covers every minimum or reserve ED twice.
A warning acknowledgement is not permission to change the reviewed amount silently.
For bugs, add a regression that fails on the previous behavior and checks the resulting call/decision,
not only formatted text. Include zero, exact boundary, below/above boundary and wallet/input changes.
Test uncertainty after handoff separately from proven non-submission; never blindly retry an unknown transaction.
Use existing Validation, ValidationSystem/DSL and ValidationExecutor rather than ad-hoc dialog branches.

Human review remains required before releasing financial/signing/auth-sensitive changes. Agent self-review,
a mock, a successful build and dry run are different evidence; none grants permission to send a real transaction.
