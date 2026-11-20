# Testing and verification

Read build.gradle, module Gradle configuration and the applicable CI workflow for current commands.
Do not copy toolchain versions or module counts from old prose; README and build files own setup.

## Commands

| Purpose | Command from repository root |
|---|---|
| Fast module compile (example) | `./gradlew :feature-swap-impl:compileDebugKotlin` |
| Targeted unit tests (example) | `./gradlew :feature-staking-impl:testDebugUnitTest` |
| Single regression class | `./gradlew :feature-staking-impl:testDebugUnitTest --tests 'fully.qualified.TestClass'` |
| Main and unit-test source style | `./gradlew ktlint` |
| Style of selected files | `./gradlew ktlint -PktlintFiles=<comma-separated paths>` |
| CI lint + unit tests in one local run | `./gradlew runTest` |
| Develop build | `./gradlew assembleDevelop` |
| Instruction checks | `python3 scripts/check_agent_instructions.py` |
| Checker regression tests | `python3 -m unittest discover -s scripts/tests -p 'test_*.py'` |
| Verify pinned master examples locally | `python3 scripts/check_agent_instructions.py --verify-reference-revision` |

Substitute the affected existing module/test class; these examples do not imply all modules expose each task.
`runTest` runs clean + ktlint + testDebugUnitTest; it is deliberately slower than the targeted loop.
`runModuleTests` exists only where tests.gradle is applied. The root lint task is not `:module:ktlintCheck`.
Lint scans Kotlin under `src/main` and `src/test`. `src/androidTest` is not linted; inspect its style separately.
`ktlintFormat` edits the same sources across the project; do not run it blindly on a dirty checkout.
`ktlintFiles` narrows `ktlint` and `ktlintFormat` to the listed files and ignores paths outside the linted source sets.

## CI layout and the pre-push hook

`reusable_android_build.yml` owns the Android jobs. `Lint (ktlint)` and `Run unit tests` are independent,
so a style failure does not hide test results; both follow the `run-tests` input. The lint job runs
`./gradlew :ktlint --configure-on-demand`, which configures the root project only and therefore needs
no SDK, NDK, Rust or secrets. The test job runs `./gradlew testDebugUnitTest`.
They run on pull requests (`pull_request.yml`), on pushes to `develop` (`push_develop.yml`, which also
builds and distributes) and on pushes to `rc/**` (`push_rc.yml`).

`scripts/git-hooks/pre-push` runs the same ktlint task on the Kotlin files touched by commits the remote
does not have yet and aborts the push on violations; it does nothing when no Kotlin file changed. It reads
the working tree, so it only checks a push of the checked-out commit. `git push --no-verify` skips it once.
A local Gradle run or IDE sync enables it (`scripts/git-hooks.gradle`) by setting `core.hooksPath` to
`scripts/git-hooks`, but only outside CI, while no git config scope defines that key and `.git/hooks` holds
no hooks of its own. An existing value is never changed: `git config core.hooksPath .git/hooks` opts a clone
out, and `git config core.hooksPath scripts/git-hooks` enables the hook by hand where the automatic step did
not apply. The setting is shared by all worktrees of a clone; a worktree on a branch without
`scripts/git-hooks/pre-push` simply has no hook.

## Evidence by risk

- Documentation-only: instruction/link checks and diff review; no APK is required.
- Domain/data bug: regression reproduces the old failure; test successful, missing/empty, failed,
  cancellation/retry, boundaries, stale results and identity changes where applicable.
- UI: compile plus relevant state/mapper tests; inspect layout and binding, loading/error/empty/value,
  long strings, font scale, touch targets and return navigation. Report absent device verification.
- Serialization/runtime: use fixtures matching the real decoder/contract, malformed inputs and range checks.
- Financial/signing: add invariant/call-construction tests, fee/Max/ED and batch boundary checks,
  confirmed input preservation and unknown-outcome handling; see [transactions](transactions.md).
- DB: migration tests and schema/version review. The shared `AppDatabaseMigrationTest` discovers exported
  schemas and validates available forward migration paths; do not add a separate validation script for
  each version. Use Room auto migrations for supported schema changes, keeping old and new schema exports
  in version control; schema files must not be ignored.
  Data transformations also need focused tests that verify the affected records survive correctly.
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
Making a check required on protected branches needs separate approval.
Pinned-master object verification is local/optional because shallow CI may not contain that historical object.
