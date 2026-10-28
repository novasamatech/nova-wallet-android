# Nova Wallet Android

Shared instructions for Codex and Claude. Keep this entry point under 100 lines.
Read linked documents explicitly; links are routing, not automatic imports.

## Scope and sources

- Work in this repository only unless the user expands scope. Respect workspace instructions.
- Before editing, inspect the branch, status, relevant diff and any nested instructions. Preserve user changes.
- Do not commit, push, switch branches, install builds or submit transactions without authorization.
- For this convention baseline, inspect code on `master` with `git show` / `git grep`; do not switch the working checkout.
- Current rules were migrated from RC. Bittensor is a domain constraint, not an architectural reference.
- Explicit current user requirements take precedence over these project conventions.
- Written rules take precedence over conflicting legacy examples. Surface unresolved conflicts instead of guessing.

## Required reading

Always read [architecture](docs/engineering/architecture.md), [code style](docs/engineering/code-style.md),
and [delivery workflow](docs/engineering/workflow.md) before implementation or review.
Then read every topic touched by the task; do not load unrelated topic files.

| Task | Additional instructions |
|---|---|
| Screens, layouts, navigation, widgets, formatting, strings | [UI](docs/engineering/ui.md) |
| Flows, repositories, API, cache, runtime decoding | [Data and state](docs/engineering/data-and-state.md) |
| Amounts, Max, fees, validation, signing, submission | [Transactions](docs/engineering/transactions.md) |
| Analytics events, consent | [Analytics](docs/engineering/analytics.md) |
| Infrastructure authentication, attestation, tokens | [Infrastructure auth](docs/engineering/infrastructure-auth.md) |
| Any Bittensor behavior or presentation | [Bittensor constraints](docs/engineering/bittensor.md), plus the applicable general topics |
| Build, tests, CI, verification | [Testing](docs/engineering/testing.md) |
| New module or screen wiring | [Feature checklist](docs/adding-a-feature-module.md), architecture and UI |
| Swap commission/amounts | [Commission design](docs/swap-service-commission.md), transactions |
| Select an implementation reference | [Reference patterns](docs/engineering/reference-patterns.md) |
| User corrects a convention; edit instructions | [Instruction maintenance](docs/engineering/instruction-maintenance.md) |
| Audit this migration | [Migration record](docs/engineering/migration.md) |

## Work contract

1. Identify the requested behavior, affected layers, invariants and verification before editing.
2. Inspect analogous master code and the actual shared component/contract. State what will be reused.
3. Implement the smallest complete vertical change. Keep business decisions out of UI and transport.
4. Run relevant checks; then review correctness, ownership/state, reuse, and readability separately.
5. Refactor concrete issues within scope, rerun affected checks, and inspect the entire final diff.
6. Report the outcome, checks actually run, and remaining device/live-system gaps. Do not stop at “compiles”.

For a small change these are short passes, not separate user approvals. Stop for missing authority,
a product decision or a material scope expansion. Do not silently implement speculative redesigns.
Separate self-review passes are not an independent review; only claim the latter when another reviewer ran.
Delegate only when the user or other applicable instructions explicitly authorize delegation.

## Non-negotiables

- Constructor-inject named dependencies; assemble them in DI, not in consumers (including test fixtures).
- One authoritative state owner; derive dependent values and invalidate stale asynchronous results.
- Reuse existing widgets, mixins, validation, navigation and submission infrastructure.
- Loading, unavailable, empty, zero and error are different states.
- Preserve coroutine cancellation, explicit wallet identity, transaction terms and unknown-outcome safety.
- Cross-feature dependencies use API modules; application composition wires implementations.
- No invented live data, swallowed financial failures, hidden mutation in reads, or speculative common abstractions.
- Add English source strings centrally; translations belong to the localization pipeline.
- Do not treat static checks, a build or agent review as proof of device or on-chain correctness.

## Keep instructions sustainable

When a user corrects a reusable convention, update its owning topic in the same task and mention it.
Keep one source of truth; `CLAUDE.md` imports this file and contains no parallel rules.
Record one-off requirements locally, not as universal policy. Ask when the intended scope is ambiguous.
Use the maintenance guide for conflicts, examples, size limits and verification.
