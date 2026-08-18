# Nova Wallet Android

Multi-module Android wallet for the Polkadot/Kusama ecosystem. Kotlin 2.1, Dagger 2 (KSP), coroutines/Flow,
View system + ViewBinding (no Compose), Room, substrate-sdk-android. Java 17, AGP 8.9, Gradle 8.11.

## Commands

```bash
./gradlew assembleDevelop                         # main CI build (build type, not a flavor)
./gradlew :feature-swap-impl:compileDebugKotlin   # fast compile check of one module
./gradlew runTest                                 # what CI runs = clean + ktlint + testDebugUnitTest
./gradlew :feature-staking-impl:testDebugUnitTest # fast test loop for one module
./gradlew ktlint                                  # style check (root JavaExec task)
./gradlew ktlintFormat                            # autofix
```

- `ktlint`/`ktlintFormat` are **root-level tasks**, not per-module. `:module:ktlintCheck` does not exist.
- `ktlint` only scans `**/src/main/**` — test sources are unchecked.
- `runTest` starts with `clean`, so it is slow; prefer per-module test tasks while iterating.
- `:module:runModuleTests` exists only in modules that `apply from: '../tests.gradle'`.
- Build types (no product flavors): `debug`, `debugLocal`, `develop`, `releaseTest`, `releaseMarket`,
  `releaseGithub`, `instrumentialTest`. `debug` signs with a shared committed keystore so Google services
  work; use `debugLocal` for a local key.

## Environment gotchas (build fails without these)

- `local.properties` must define every secret key listed in the README, `mock` values are fine.
  Error `Secret X is not found` means the README list is stale — add `X=mock`.
- Rust toolchain + NDK `29.0.14206865` are required: `bindings/hydra-dx-math` and `bindings/metadata_shortener`
  run `cargoBuild` as part of assembling. Add the four Android rustup targets (see README).
- `nova-wallet-dapp-js` / `nova-wallet-metamask-js` are **git submodules** consumed by `feature-dapp-impl`
  (npm build step). Run `git submodule update --init` or that module fails to build.

## Module map

Everything is an Android library except `app`. Common Gradle config is injected by `allmodules.gradle`
(no per-module plugin/compileSdk blocks — don't add them).

| Module | Purpose |
|---|---|
| `app` | Application class, root DI graph, **all navigation** (53 nav graphs, all `Router` impls), only module allowed to depend on `-impl` |
| `common` | Base classes, mixins, validation framework, utils, **all app strings/resources** |
| `core-api` | Tiny cross-cutting contracts (updaters, storage, ethereum) |
| `core-db` | Room `AppDatabase` (version 73), DAOs, migrations |
| `runtime` | Chain registry, connections, runtime metadata, extrinsics, storage access |
| `bindings/*` | Rust FFI: `hydra-dx-math` (swap math), `metadata_shortener` (Ledger) |
| `caip` | CAIP-2/CAIP-10 chain & account ID parsing (WalletConnect) |
| `web3names` | Web3 name resolution for addresses |
| `test-shared` | Shared unit-test helpers (`CoroutineTest`, Mockito/assert helpers) |

Feature modules follow `feature-<name>-api` / `feature-<name>-impl`:
**api** = domain models, interfaces, DI `FeatureApi`, reusable UI mixins; **impl** = data/domain/presentation.
Pairs: account, ahm, banners, buy, cloud-backup, crowdloan, currency, dapp, external-sign, gift, governance,
ledger, nft, onboarding, proxy, settings, staking, swap, versions, wallet, wallet-connect, xcm.

Single-module / non-obvious features:

| Module | Purpose |
|---|---|
| `feature-assets` | The Assets tab: balances, asset list, send/receive, operation history |
| `feature-vote` | Thin host tab that embeds governance + crowdloan screens |
| `feature-ahm-*` | Asset Hub Migration (chain migration flows) |
| `feature-swap-core` | Quoting primitives shared by swap api + impl (path/graph/direction types) |
| `feature-ledger-core` | Ledger transport shared by api + impl |
| `feature-multisig:operations` | Multisig operation flows |
| `feature-account-migration` | Legacy account migration |
| `feature-deep-linking` | Deep link parsing and dispatch |
| `feature-splash`, `feature-push-notifications` | Self-explanatory, no api/impl split |

## Hard architectural rules

1. **No feature module may depend on another feature's `-impl`.** Cross-feature access goes through `-api`.
   Only `app` wires `-impl` modules together (verified: 0 violations today).
2. **All user-facing strings live in `common/src/main/res/values/strings.xml`.** Feature modules have no
   `strings.xml` (only `app` and `feature-ahm-impl` are exceptions). Add the **English** string only —
   the 13 `values-*` locale files in `common` come from the translation pipeline, never hand-edit them.
3. **Navigation lives in `app`.** A feature declares a `Router` interface in its own `presentation` package;
   `app/root/navigation/navigators/Navigator.kt` implements it and a `*NavigationModule` binds it. Adding a
   screen means touching `app` (nav graph XML + navigator).
4. **Room schema changes need a migration** in `core-db/.../migrations/` plus a version bump in
   `core-db/.../AppDatabase.kt`.

## DI: two-level Dagger graph

Feature graph — lazily created and released per feature, keyed by the `-api` module's `FeatureApi` interface:

- Holder: `feature-swap-impl/.../di/SwapFeatureHolder.kt` (registered in `app`'s `ComponentHolderModule`)
- Component + `Dependencies` component: `feature-swap-impl/.../di/SwapFeatureComponent.kt`
- Base: `common/.../di/FeatureApiHolder.kt`, lookup helper `common/.../di/FeatureUtils.kt`

Screen graph — a `@Subcomponent` with `@ScreenScope`, created from the Fragment:

- `feature-swap-impl/.../presentation/route/di/SwapRouteComponent.kt` (factory takes `@BindsInstance Fragment`)
- `.../di/SwapRouteModule.kt` — must provide **both** the `@IntoMap @ViewModelKey` binding **and** a concrete
  `provideViewModelCreator` that resolves the VM through `ViewModelProvider`. Omitting either breaks injection.
- Fragment calls it from `inject()`: `feature-swap-impl/.../presentation/route/SwapRouteFragment.kt`

Dagger runs through **KSP**, not kapt.

Adding a whole feature module (or a screen) touches ~8 files across `app` and the feature — the exact
checklist and the traps live in @docs/adding-a-feature-module.md

## Screen anatomy

`BaseFragment<VM, Binding>` (`common/.../base/BaseFragment.kt`) + `BaseScreenMixin`
(`common/.../base/BaseScreenMixin.kt`) define the contract every screen implements:
`createBinding()` → `initViews()` → `inject()` → `subscribe(viewModel)`. The inflated binding is exposed as
`binder`; the ViewModel is `@Inject lateinit var`, so it is only valid after `inject()`.

- ViewBinding is enabled per module via `buildFeatures { viewBinding true }`.
- Arguments are passed as a `@Parcelize` `Payload` through a `companion object getBundle(...)` —
  see `feature-swap-impl/.../presentation/main/SwapMainSettingsFragment.kt`.
- Returning a result to a previous screen uses `InterScreenCommunicator`
  (`common/.../navigation/InterScreenCommunicator.kt`), implemented in `app`'s navigation package.
- Lists: `common/list/BaseListAdapter.kt`, `GroupedListAdapter.kt`, `SingleItemAdapter.kt` (+ `ConcatAdapter`).

## Domain patterns

**Validation framework** — `common/.../validation/Validation.kt`. Validations are `suspend` predicates
returning `ValidationStatus.Valid` / `NotValid(level, reason)`; a `ValidationSystem` composes them and reports
the *most severe* failure (exceptions are swallowed if any validation already failed). Build systems with the
DSL in `common/.../validation/Builder.kt`; per-validation files live in `<feature>/domain/validation/validations/`
(reference: `feature-swap-impl/.../domain/validation/validations/`). `BaseViewModel.requireValid(...)` runs a
system and displays failures.

**Reusable UI mixins** — cross-feature UI logic ships as mixins in `-api` modules, injected into ViewModels:
`feature-wallet-api/.../presentation/mixin/` (fee, amountChooser, assetSelector, maxAction),
`common/.../mixin/actionAwaitable/` (await a dialog result from a ViewModel).

**Flow conventions** — `common/.../utils/WithCoroutineScopeExtensions.kt` gives ViewModels
`Flow.shareInBackground()`; `withSafeLoading()` (`common/.../utils/FlowExt.kt`) wraps a flow into
`ExtendedLoadingState` (`common/.../domain/ExtendedLoadingState.kt`) so screens render loading/error without
try/catch. `BaseViewModel` *is* a `CoroutineScope` backed by `viewModelScope`.

**Chain access** — never construct chain config by hand; go through `ChainRegistry`
(`runtime/.../multiNetwork/ChainRegistry.kt`) for chains, assets, connections and runtime providers.

Swaps carry a 0.85 % Nova commission on Hydration routes, with non-obvious gross/net bookkeeping shared by
display and validations — see @docs/swap-service-commission.md before touching swap amounts.

## Code style

- ktlint (Pinterest 0.47, `--android`), `max_line_length = 160`.
- **Disabled rules** (`.editorconfig`): `import-ordering`, `package-name`, `trailing-comma-*`, `filename`.
  Don't reorder imports or "fix" filenames to appease a linter — those checks are intentionally off.
- Kotlin **context receivers** are enabled project-wide (`-Xcontext-receivers`) and used in ~50 files,
  notably in the validation and fee DSLs. `context(...)` declarations are expected, not legacy.
- Package layout inside a feature: `data/` → `domain/` → `presentation/`, with `di/` at the module root and a
  nested `di/` next to each screen package.
- Modules use Java package `io.novafoundation.nova.<module_name_with_underscores>`.
