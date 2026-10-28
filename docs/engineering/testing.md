# Testing and verification

Read build.gradle, module Gradle configuration and the applicable CI workflow for current commands.
Do not copy toolchain versions or module counts from old prose; README and build files own setup.

## Commands

| Purpose | Command from repository root |
|---|---|
| Fast module compile (example) | `./gradlew :feature-swap-impl:compileDebugKotlin` |
| Targeted unit tests (example) | `./gradlew :feature-staking-impl:testDebugUnitTest` |
| Single regression class | `./gradlew :feature-staking-impl:testDebugUnitTest --tests 'fully.qualified.TestClass'` |
| Main-source style | `./gradlew ktlint` |
| Existing CI test suite | `./gradlew runTest` |
| Develop build | `./gradlew assembleDevelop` |
| Instruction checks | `python3 scripts/check_agent_instructions.py` |
| Checker regression tests | `python3 -m unittest discover -s scripts/tests -p 'test_*.py'` |
| Verify pinned master examples locally | `python3 scripts/check_agent_instructions.py --verify-reference-revision` |

Substitute the affected existing module/test class; these examples do not imply all modules expose each task.
`runTest` runs clean + ktlint + testDebugUnitTest; it is deliberately slower than the targeted loop.
`runModuleTests` exists only where tests.gradle is applied. The root lint task is not `:module:ktlintCheck`.
Lint currently scans `**/src/main/**/*.kt`, so inspect test-source style separately.
`ktlintFormat` edits main sources across the project; do not run it blindly on a dirty checkout.

## Evidence by risk

- Documentation-only: instruction/link checks and diff review; no APK is required.
- Domain/data bug: regression reproduces the old failure; test successful, missing/empty, failed,
  cancellation/retry, boundaries, stale results and identity changes where applicable.
- UI: compile plus relevant state/mapper tests; inspect layout and binding, loading/error/empty/value,
  long strings, font scale, touch targets and return navigation. Report absent device verification.
- Serialization/runtime: use fixtures matching the real decoder/contract, malformed inputs and range checks.
- Financial/signing: add invariant/call-construction tests, fee/Max/ED and batch boundary checks,
  confirmed input preservation and unknown-outcome handling; see [transactions](transactions.md).
- DB: migration tests and schema/version review.
- Auth: no credential logging; expiry, cancellation/coalescing, rejected generation and replay tests.

Use existing test-shared helpers and neighboring test patterns; dependencies are named and injected.
Do not use wall-clock sleeps as synchronization when a deterministic scheduler/flow assertion is available.
A mock is not live integration evidence. Keep failed commands visible in the handoff.

## Setup constraints

Use the README and current Gradle scripts for required Java/Gradle/SDK/NDK/Rust versions.
Native bindings (hydra-dx-math, metadata_shortener) require Rust/Android targets and the configured NDK.
Dapp JS dependencies are git submodules; inspect their state before initializing them as a needed build step.
This project uses build types rather than product flavors; inspect app/build.gradle for current variants.
Debug and debugLocal differ in signing; do not replace signing material or install a build without authorization.

Missing local secrets are a setup problem, not proof of a source failure. Compile-only mocks may be used
only when safe for that task and clearly labeled; never silently insert mock credentials for a live test,
overwrite real values, print secrets, or claim integrations work because a mock-backed build succeeded.

## Automation limits

The new agent-instructions workflow runs without Android SDK or secrets: it validates instruction links,
entry-point size, the Claude bridge, literal feature-to-implementation Gradle dependencies and checker tests.
The dependency check is not a Gradle parser: dynamic dependencies and ownership still need review/compilation.
Route/link checks cover inline Markdown prose links in AGENTS, engineering topics and the feature/commission
guides, excluding comments and code. They do not validate Markdown anchors, remote URLs or every Markdown dialect.
These checks do not prove UI quality.
Existing Android CI remains unchanged. Making a check required on protected branches needs separate approval.
Pinned-master object verification is local/optional because shallow CI may not contain that historical object.
