# Android: established conventions

## UI reuse

Before creating a component or formatting implementation, inspect analogous screens and shared components. Use existing balance formatters rather than raw decimal strings. Match placeholders to the data type: an address identicon is not a fallback for a token or chain logo. For a title-and-search header, check `common/view/TitledSearchToolbar` first.

Reuse `ExternalActions.showAddressActions` and `setupExternalActions` for address information: they provide the standard sheet with copying and explorer actions. In selection lists, the information action must not also select the row or confirm the selection.

Use the standard `RadioButton` with `wrap_content` dimensions, following existing selection screens. Fixed bounds copied from a design can clip the theme drawable; do not shrink the control's bounds to match the visible circle.

## Tables inside cards

When a `TableView` is inside a card with its own background, disable the table background with `app:showBackground="false"`. Setting `android:background` is insufficient because the component overrides it during construction. Do not introduce nested containers absent from the design.

## Sectioned lists

Sections belonging to one selection list must scroll together in one `RecyclerView`, including their headers and rows. Do not pin the first section or its card above the list without an explicit requirement. Reuse `common/list/GroupedListAdapter` and `toListWithHeaders`, with separate view types for distinct row types. A search header may remain pinned.

## Operation confirmation

Fee estimation and submission must use the same call builder, including service fees and batch mode. Do not replace confirmed amounts or protective price limits with a fresh quote just before signing. Recheck the selected wallet, available balance, and applicable permissions before submission. Do not substitute a root validator for a subnet validator without explicit configuration and eligibility checks in that subnet.

## Independent loading on one screen

Independent data, such as wallet positions and a market chart, must have independent sources, error states, and retries. Failure of one load must not hide another load's successful data. When converting an exception into an error or stale state, log diagnostic context and the cause without secrets or personal data.

## Runtime response decoding

For named metadata structures, the SDK normalizes snake_case fields to camelCase. Bindings must read normalized names. Tests must use the structure returned by the decoder rather than the original Rust field names.

Respect Android minSdk when converting numbers: a passing JVM test does not prove a JDK method exists on the device. Do not use `BigInteger.intValueExact()` on older Android versions without confirmed support. Check the range first, then use `toInt()` to prevent overflow.

## Bittensor validator selection

Load the validator directory and filtering policy through attested infrastructure endpoints, not directly from tao.app. Do not invent local `maxTake` values or fallback eligibility policies when the backend is unavailable. Check UID, permit, take, and activity on-chain at one block hash using batched queries; revalidate the selected validator before submission. Backend names and optional statistics do not replace on-chain eligibility checks.

Generate a validator's icon from its hotkey with `AddressIconGenerator`, as for other addresses in the app. A chain logo or generic placeholder does not replace an address identicon. Explicitly pass the validator selected before amount entry into confirmation. Eligibility revalidation must not silently substitute another validator.

## Self-review and refactoring

Refactor to solve a concrete correctness, ownership, consistency, duplication, testability, or complexity problem, not personal stylistic preference. Inspect existing implementations first. Keep authoritative state in one place and derive dependent values; inject dependencies rather than constructing them inside consumers. Distinguish unavailable data from zero or empty data, and do not disguise mocks or incomplete integrations as working functionality.

Prioritize operation correctness, then state and error handling, then architecture and duplication, then readability. For each change, identify the problem, the intended improvement, and the verification. Separate behavior changes from behavior-preserving refactoring. Avoid unrelated cleanup and speculative abstractions in `common`.
