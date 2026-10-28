# Instruction migration record — 2026-10-28

## Sources and scope

- Rules: RC `rc/11.0.0`, commit `36e0bf33b66a4531e631e1b3a192c7456632886e`.
- Code examples/config inspection: master `2cdfbbc2e2d82c6010046b7aabd872112718547a`.
- Working branch stayed RC. No application code, financial behavior or Bittensor implementation was edited.
- This file records the migration, not additional runtime/product requirements.

## Old AGENTS section → owner

| Previous section | Destination |
|---|---|
| UI reuse; strings; tables | [UI](ui.md) |
| Sectioned lists | UI; manual Bittensor picker in [Bittensor](bittensor.md) |
| Screen navigation arguments | UI |
| Operation confirmation | [Transactions](transactions.md); Bittensor presentation and approved exceptions in Bittensor |
| Independent loading | [Data/state](data-and-state.md); Bittensor presentation in Bittensor |
| Remote API models | Data/state; consent in [analytics](analytics.md); profile-3 in [auth](infrastructure-auth.md) |
| Analytics event declarations | Analytics |
| Runtime response decoding | Data/state |
| Bittensor validator selection; position actions | Bittensor |
| Self-review and refactoring | [Architecture](architecture.md), [code style](code-style.md), data/state |

All original AGENTS content paragraphs are retained in their topic owners; no feature constraint was
promoted to a master-based architectural example. Routing and review/maintenance guidance are new.

## Old CLAUDE consolidation

| Previous content | Treatment |
|---|---|
| Stack and exact tool versions | Keep Views/ViewBinding, Dagger/KSP, Flow and Room conventions; versions owned by build config |
| Commands, CI, setup | [Testing](testing.md); read actual workflows instead of freezing CI claims |
| Module map and hard rules | Architecture; module inventory in settings.gradle, no frozen counts or DB version |
| Feature/screen DI and checklist | Architecture + existing feature-module guide |
| Screen anatomy, arguments, lists | UI; removed misleading lifecycle arrow, inspect the actual delegate |
| Domain validation, fee/amount mixins, Flow, ChainRegistry | Transactions, data/state, architecture + reference examples |
| Commission design | Existing commission guide routed from AGENTS; no rate duplicated in generic rules |
| Code style and package layout | Code style, architecture and executable .editorconfig |
| Eager doc imports | Explicit task routing; only shared AGENTS import remains in CLAUDE |

The old advice to add `X=mock` for every missing secret was narrowed: mocks are compile-only,
explicit and must not overwrite credentials or masquerade as a working integration.
Stale branch-status text in the commission guide is marked historical; product rules are not rewritten here.

## Known limitations / acceptance

Master examples contain deprecated formatting/state patterns; each is scoped with exclusions in
[reference patterns](reference-patterns.md). They are candidates, not user-approved universal templates.
Old product parity rules are preserved, not reverified against current iOS in this documentation task.
The feature checklist's historical counts and misleading binding claim were removed; current build/schema own versions.
A documentation checker does not validate runtime architecture or force agent obedience.
Real Codex/Claude end-to-end pilot on an application task and independent/human review remain separate evidence.

## Read-only pilot performed

- Applied the review checklist to the pinned master SwapRouteViewModel: derived flows and small mapping
  helpers are reusable, while deprecated fiat formatting and state-store navigation must not become templates.
  The reference catalog records both exclusions; no application changes were made.
- Traced an amount-screen task through architecture/style/workflow → UI/data/transactions/testing:
  typed payload, FeeLoaderMixinV2, explicit wallet, cancellation and financial boundary checks are reachable.
- Traced an analytics-event task to event classes and AnalyticsService; auth rules remain a separate route.
- Traced a Bittensor task to its preserved domain exceptions plus general rules, not to Bittensor as an exemplar.
- This is a documentation/review pilot by the author, not an independent review or a fresh Claude session.
