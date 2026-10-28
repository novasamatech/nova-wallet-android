# Architecture and ownership

Read this before implementation or review. Examples are scoped in [reference patterns](reference-patterns.md).

## Module boundaries

| Location | Owns | Does not own |
|---|---|---|
| `app` | Application composition, feature holders, Router implementations and navigation graphs | Feature calculations |
| `feature-*-api` | Cross-feature contracts, domain models, FeatureApi, reusable UI mixin contracts | Dependency on another feature implementation |
| `feature-*-impl/data` | Transport adapters, DTO mapping, data access and caches | Screen state, navigation |
| `feature-*-impl/domain` | Interactors, operation orchestration, financial rules and validation | Views or string formatting |
| `feature-*-impl/presentation` | ViewModels, UI state, formatting, screen interaction | HTTP/SCALE decoding or financial rules |
| `common` | Established shared widgets, validation, base classes, utilities | A speculative abstraction for one feature |
| `runtime` | ChainRegistry, connections, metadata, storage, extrinsics | Feature screen decisions |
| `core-db` | Room entities, DAOs, database and migrations | Presentation |
| `test-shared` | Shared test infrastructure | Production behavior |

A feature must not depend on another feature's `-impl`; use the API contract. `app` composes implementations.
Use `ChainRegistry` for chain configuration, assets, connections and runtime providers.
Room schema edits require a migration and an appropriate database version update; inspect the current version.
Shared Gradle setup is in `allmodules.gradle`; do not duplicate its plugin/SDK configuration in each module.
Package names follow `io.novafoundation.nova.<module_name_with_underscores>`; screen DI lives next to its screen.
Check `settings.gradle` for actual module names rather than relying on old module counts.

## Dependency injection

Use Dagger/KSP and the existing feature/screen graphs. Do not introduce a parallel service locator.
A feature has a FeatureApi, FeatureApiHolder, dependencies and component, registered in app composition.
A screen uses its existing ScreenScope subcomponent; bind Fragment and typed payload at the boundary.
ViewModels need both the IntoMap/ViewModelKey binding and the concrete ViewModelProvider creator.
Use [the feature wiring checklist](../adding-a-feature-module.md); inspect current component factories before editing.

Create named dependencies separately, then constructor-inject them. Do not construct repositories, services,
use cases or collaborators inline inside consumers, even in tests. Creating plain values/models is not DI.
Tests create named fakes/mocks first and pass them into the subject explicitly.

## Ownership review

Keep responsibilities separated when a class spans several workflows: data sources own transport and pagination; repositories own data access and caches; interactors own wallet-context orchestration and financial calculations; presentation owns UI loading states and formatting. Do not move an overloaded repository wholesale into a mixin. Preserve cache keys, invalidation, incomplete-data semantics, and cancellation when extracting responsibilities. Existing shared contracts should be assessed in context rather than replaced solely because of their type names.

Before introducing a class, identify its owner, inputs, output, lifetime and real callers.
Extract when it fixes a concrete responsibility/testability/duplication problem; do not add a
one-method interface or forwarding layer merely to imitate a diagram. Keep an interface when
it is an actual API boundary, substitution point or established project contract.
Do not migrate architecture frameworks (for example Views to Compose) as incidental cleanup.
