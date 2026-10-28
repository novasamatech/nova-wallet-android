# Reference patterns

Inspected code source: local `master` at `2cdfbbc2e2d82c6010046b7aabd872112718547a`.
Status: **approved by the user on 2026-10-28 for the listed aspects only**. No Bittensor implementation was used as a style example.
Links below open working-tree files for convenience; authoritative inspected content is the pinned master
version: `git show 2cdfbbc2e2d82c6010046b7aabd872112718547a:<path>`.
Do not assume the working checkout is identical. Use the same read-only approach without switching branches.

<!-- reference-revision: 2cdfbbc2e2d82c6010046b7aabd872112718547a -->

## SwapRouteViewModel.kt

<!-- reference-file: feature-swap-impl/src/main/java/io/novafoundation/nova/feature_swap_impl/presentation/route/SwapRouteViewModel.kt -->
[Working-tree file](../../feature-swap-impl/src/main/java/io/novafoundation/nova/feature_swap_impl/presentation/route/SwapRouteViewModel.kt)

Use for: Derived UI flows and small named mapping functions.
Do not copy blindly: Do not copy deprecated fiat formatting or feature state-store navigation; current explicit-payload and FiatFormatter rules win.

## SwapRouteModule.kt

<!-- reference-file: feature-swap-impl/src/main/java/io/novafoundation/nova/feature_swap_impl/presentation/route/di/SwapRouteModule.kt -->
[Working-tree file](../../feature-swap-impl/src/main/java/io/novafoundation/nova/feature_swap_impl/presentation/route/di/SwapRouteModule.kt)

Use for: Screen Dagger wiring: IntoMap binding and concrete ViewModelProvider creator.
Do not copy blindly: Check current constructor/factory inputs; do not treat older syntax as a formatting rule.

## SwapRouteFragment.kt

<!-- reference-file: feature-swap-impl/src/main/java/io/novafoundation/nova/feature_swap_impl/presentation/route/SwapRouteFragment.kt -->
[Working-tree file](../../feature-swap-impl/src/main/java/io/novafoundation/nova/feature_swap_impl/presentation/route/SwapRouteFragment.kt)

Use for: Binding observers and composing existing list adapters.
Do not copy blindly: Do not copy generic onNotLoaded handling as a full error model or the legacy member order.

## SwapMainSettingsFragment.kt

<!-- reference-file: feature-swap-impl/src/main/java/io/novafoundation/nova/feature_swap_impl/presentation/main/SwapMainSettingsFragment.kt -->
[Working-tree file](../../feature-swap-impl/src/main/java/io/novafoundation/nova/feature_swap_impl/presentation/main/SwapMainSettingsFragment.kt)

Use for: Typed payload/getBundle, standard fee/amount/validation UI bindings.
Do not copy blindly: Do not copy duplicate setup calls or use the screen as a complete state-management template.

## GiftsRepository.kt

<!-- reference-file: feature-gift-impl/src/main/java/io/novafoundation/nova/feature_gift_impl/data/GiftsRepository.kt -->
[Working-tree file](../../feature-gift-impl/src/main/java/io/novafoundation/nova/feature_gift_impl/data/GiftsRepository.kt)

Use for: Repository owns persistence and local/domain mapping.
Do not copy blindly: The reverse status mapper is named toDomain despite returning local data; use accurate mapping names.

## GiftsInteractor.kt

<!-- reference-file: feature-gift-impl/src/main/java/io/novafoundation/nova/feature_gift_impl/domain/GiftsInteractor.kt -->
[Working-tree file](../../feature-gift-impl/src/main/java/io/novafoundation/nova/feature_gift_impl/domain/GiftsInteractor.kt)

Use for: Interactor orchestrates repositories and wallet-context work.
Do not copy blindly: Wallet selection is captured; this is not a reference for wallet-switch-safe reactive subscriptions.

## NovaSwapCommissionTest.kt

<!-- reference-file: feature-swap-impl/src/test/java/io/novafoundation/nova/feature_swap_impl/domain/model/NovaSwapCommissionTest.kt -->
[Working-tree file](../../feature-swap-impl/src/test/java/io/novafoundation/nova/feature_swap_impl/domain/model/NovaSwapCommissionTest.kt)

Use for: Small deterministic arithmetic and protected-output invariant tests.
Do not copy blindly: Do not generalize a configured rate to other chains or take test success as on-chain proof.

## Selecting another reference

Find a mature analogous flow in master; inspect the collaborator/component contract too.
Record the exact aspect being reused and any conflict with current rules. A random nearby file or a
large class is not an architectural standard merely because it already exists.
Ordinary reuse of these approved aspects does not need another approval. Adding or widening baseline
examples requires review and user approval; whole-file copying remains explicitly excluded.
