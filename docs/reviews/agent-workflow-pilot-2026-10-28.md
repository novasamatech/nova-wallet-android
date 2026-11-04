# Agent workflow pilot — 2026-10-28

## Scope and evidence

Instruction baseline: `cf284ca9b` on `feature/agents`; follow-up changes remain uncommitted.
Code baseline: master `2cdfbbc2e2d82c6010046b7aabd872112718547a`.
The user approved the reference catalog for its listed aspects, not whole-file copying.
Claude testing was explicitly deferred by the user. No claim of Claude compliance is made.

Application changes are isolated in `/private/tmp/nova-agent-pilot.miHvRs/worktree` and are not
merged into the working branch. This directory is temporary, not a durable release artifact.
The main checkout contains only instruction/checker improvements and this report.

## Independent instruction review

A separate reviewer examined the instructions, checker, regression tests and workflow without editing files.
The first pass found five actionable issues; all were fixed:

1. Review-only requests could be interpreted as permission to refactor. The work contract now explicitly
   separates inspection from authorized changes, including instruction maintenance.
2. The feature checklist omitted the module-local Parcelize plugin. The conditional setup step now
   matches the pinned master gift modules; no build configuration was changed in the main checkout.
3. A commented-out topic path counted as a route. Only prose Markdown links now count.
4. The routed feature/commission guides were omitted from link checking. Both are checked now.
5. Generic transaction work was directed into all Bittensor rules. That route is now conditional.

The second pass found two parser issues: code-formatted link labels disappeared, and longer closing
fences/unclosed code blocks leaked sample links into routing. Focused fixes and regressions cover both.
The reviewer rechecked these fixes and reported no remaining significant findings in that scope.
This is not a claim that the checker parses every Markdown/Gradle construct or enforces architecture.

## Verification of instruction changes

- The first new regression set reproduced seven failing subcases before the fixes.
- The second regression set reproduced four failing subcases before the fixes.
- All 18 checker tests pass after the fixes; the reviewer independently confirmed them.
- Instruction checking with pinned-reference verification passes; `git diff --check` passes.
- Review/implementation roles were separate; the code author did not certify their own work as independent review.

## Implementation pilot

Task: migrate route fee presentation from the deprecated formatter to injected `FiatFormatter`,
preserving the original currency, rounding, abbreviation and label. Do not change trading calculations,
navigation or state ownership. This deliberately tests a real, small UI/DI change, not Bittensor behavior.

A fresh Codex worker read the routed instructions, implemented dependency wiring through the existing
feature/screen graph, and added two regression tests for the public presentation flow.
The pilot changes three production Kotlin files, one test file and the test dependency declaration.
No new formatter, service locator, state holder or financial calculation was introduced.

Independent code review caught a real type mismatch in the test: `chainsById` returns `SharedFlow`,
but the test supplied a plain `Flow`. The author replaced it with a named `MutableStateFlow`, and the
reviewer verified the correction. The reviewer found no other actionable issue in the scoped change.
This is evidence that the separate review gate adds value; the author's first self-review missed this issue.

Initial Gradle attempts stopped during setup, not application verification. The isolated build was then
configured with literal compile-only MOCK values and the existing SDK/cache; no real secrets were copied.
After the test correction, verification succeeded:

- Offline `:feature-swap-impl:compileDebugKotlin` plus targeted `:feature-swap-impl:testDebugUnitTest`:
  `BUILD SUCCESSFUL in 12s`; the production module/Dagger tasks and test compilation succeeded.
- JUnit XML: 2 tests, 0 failures, 0 errors, 0 skipped.
- Cached project ktlint 0.47.0: no findings on the three modified production Kotlin files and new test.
- `git diff --check`: passed.

Artifacts under the temporary worktree:

- `.gradle/pilot-verification/targeted-build-fixed.log` — successful build log.
- `.gradle/pilot-verification/build.sh` and `lint.sh` — exact verification commands and isolated environment.
- `.gradle/pilot-verification/ktlint.xml` — lint report.
- `feature-swap-impl/build/test-results/testDebugUnitTest/TEST-io.novafoundation.nova.feature_swap_impl.presentation.route.SwapRouteViewModelTest.xml` — test results.

The primary agent and reviewer inspected these artifacts. The reviewer accepted the formatter migration
after the fix; neither claims device verification. No dependencies were downloaded and no APK was installed.
The pilot used the initial instruction overlay while documentation fixes were reviewed separately;
a stale standalone commission guide left in the master fixture was a setup mismatch, already fixed in the main instructions.

## Remote CI

Read-only GitHub queries found no PR or workflow runs for `feature/agents` at the time of this check.
The workflow has not been remotely verified. No commit, push, PR creation or workflow dispatch was performed.
Publication and the remote CI run remain a separate next step requiring authorization.

## What this pilot does not prove

It does not prove whole-project architectural quality, real-device rendering, production transaction safety,
Claude behavior, or success of GitHub Actions. A small pilot can expose instruction gaps, not certify all future changes.

## Outcome

The local instruction/review/implementation pilot is complete within its stated scope. Reference aspects
are user-approved, seven instruction/checker findings and one pilot test finding are resolved, and local
verification passes. Only the GitHub CI execution remains pending from the active plan; Claude testing
was explicitly removed from scope. Publishing the pilot code into the application is not part of this task.
