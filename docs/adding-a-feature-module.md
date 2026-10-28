# Adding a feature module or a screen

Wiring reference: **`feature-gift-api` / `feature-gift-impl`**. This is not blanket approval of their implementation.
For the current convention baseline, inspect them on the pinned master revision in
[reference patterns](engineering/reference-patterns.md), not RC. This checklist records wiring that is easy to miss;
the [architecture rules](engineering/architecture.md) govern responsibility and dependency choices.

## 1. Create the modules

```
feature-<name>-api/     # domain models, interfaces, <Name>FeatureApi, reusable mixins
feature-<name>-impl/    # data/ domain/ presentation/, di/ at module root
```

- Register both in `settings.gradle`.
- `build.gradle` stays minimal: **no** `plugins`/`compileSdk`/`kotlinOptions` blocks — `allmodules.gradle`
  owns the Android library plugin, KSP, Java configuration and `-Xcontext-receivers` setup.
  Declare only `namespace`, `buildFeatures { viewBinding true }` (impl only) and dependencies.
- Java package is `io.novafoundation.nova.feature_<name>_api|impl` (underscores, not dashes).
- The `-api` module `api`-exposes what dependents need (`api project(":feature-wallet-api")`); the `-impl`
  module uses `implementation` for everything.
- No `strings.xml` here — see §6.

## 2. Feature-level DI (four files)

| File | Role |
|---|---|
| `feature-<name>-api/di/<Name>FeatureApi.kt` | The public surface. **This interface is the DI map key.** |
| `feature-<name>-impl/di/<Name>FeatureComponent.kt` | `@Component @FeatureScope`, implements `<Name>FeatureApi` |
| `feature-<name>-impl/di/<Name>FeatureDependencies.kt` | Everything pulled in from *other* graphs |
| `feature-<name>-impl/di/<Name>FeatureHolder.kt` | Builds the component lazily; `@ApplicationScope` |

`<Name>FeatureComponent` also declares the nested `<Name>FeatureDependenciesComponent`, whose
`dependencies = [...]` list names the foreign graphs (`CommonApi`, `DbApi`, `RuntimeApi`, other
`*FeatureApi`s). The Holder builds `Dagger<Name>FeatureComponent_<Name>FeatureDependenciesComponent`
from `getFeature(XApi::class.java)` calls, then feeds it to `Dagger<Name>FeatureComponent.factory()`.

> **The main time sink.** `<Name>FeatureDependencies` must re-declare *every single binding* the feature
> consumes from a foreign graph — one accessor per binding. Dagger component dependencies do
> not transit: when the compiler says a type cannot be provided, inspect these accessors as well as
> the binding modules instead of duplicating a dependency inside the consumer.

Component factories for screen subcomponents are exposed as methods on `<Name>FeatureComponent`
(`fun giftsComponentFactory(): GiftsComponent.Factory`).

## 3. Register the feature in `app`

`app/di/deps/ComponentHolderModule.kt`:

```kotlin
@ApplicationScope
@Binds
@ClassKey(GiftFeatureApi::class)   // the -api interface, NOT the component
@IntoMap
fun provideGiftFeature(holder: GiftFeatureHolder): FeatureApiHolder
```

`FeatureHolderManager` resolves holders from that map, so a missing entry fails at runtime
(`IllegalStateException` from `FeatureHolderManager`), not at compile time.

## 4. Navigation (always touches `app`)

1. Declare `<Name>Router` in the **impl** module's `presentation` package. Extend `ReturnableRouter`
   (or `SecureRouter` / `DelayedNavigationRouter`) when you need the shared behaviour.
2. Implement it in `app/root/navigation/navigators/<name>/<Name>Navigator.kt`, extending `BaseNavigator`
   and delegating shared actions to the injected `Navigator`.
3. Bind it in `app/di/app/navigation/<Name>NavigationModule.kt` (`@ApplicationScope @Provides`).
4. **Add that module to the `modules = [...]` list in `app/di/app/navigation/NavigationModule.kt`** —
   easy to forget, and the failure is a confusing "cannot be provided" on the Router.
5. The Router instance reaches the feature graph as `@BindsInstance` through the Holder, so the Router is
   constructed by `app` before the feature graph exists.

Nav graphs live in `app/src/main/res/navigation/`. A new graph must be `<include>`d by its
parent: `split_screen_nav_graph.xml` for anything inside the bottom-navigation shell,
`root_nav_graph.xml` for full-screen flows outside it.

## 5. Adding a screen

Per screen, inside `presentation/<screen>/`:

- `<Screen>Fragment.kt` — extends `BaseFragment<VM, Binding>`; implements `createBinding()`, `initViews()`,
  `inject()`, `subscribe(viewModel)`.
- `di/<Screen>Component.kt` — `@Subcomponent @ScreenScope`, factory takes `@BindsInstance fragment: Fragment`.
- `di/<Screen>Module.kt` — `@Module(includes = [ViewModelModule::class])` providing **both**:
  - `@Provides @IntoMap @ViewModelKey(<Screen>ViewModel::class) fun provideViewModel(...): ViewModel`
  - `@Provides fun provideViewModelCreator(fragment, factory): <Screen>ViewModel`

  Verify both bindings and compile the graph; the concrete injected ViewModel needs its creator binding.
- Expose the subcomponent factory on `<Name>FeatureComponent` and call it from `inject()` via
  `FeatureUtils.getFeature<...>(requireContext(), <Name>FeatureApi::class.java)`.

Arguments: a `@Parcelize` `Payload` passed through `companion object fun getBundle(payload): Bundle`
(see `SwapMainSettingsFragment`). Returning a result to a previous screen: `InterScreenCommunicator`
(`common/navigation/InterScreenCommunicator.kt`), implemented in `app`'s navigation package.

## 6. Resources

All user-facing strings go to **`common/src/main/res/values/strings.xml`**, English only.
Do not add feature-local string files. The `values-*` locale files in `common` are produced
by the translation pipeline — never edit them by hand.

Feature modules may own layouts, drawables and `attrs.xml`; shared widgets belong in `common/view`.

## 7. Before opening a PR

```bash
./gradlew :feature-<name>-impl:compileDebugKotlin   # fast feedback
./gradlew ktlint                                   # root task; scans src/main only
./gradlew assembleDevelop                          # what CI builds
```

Check that no `-impl` module ended up in another feature's `build.gradle` — cross-feature access goes
through `-api`, and only `app` may depend on `-impl`.
