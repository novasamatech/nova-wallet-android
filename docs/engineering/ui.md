# UI, navigation and resources

Use Android Views + ViewBinding and established project components. Read each component's behavior before wrapping it.

## Reuse and formatting

Before creating a component or formatting implementation, inspect analogous screens and shared components. Use existing balance formatters rather than raw decimal strings. Match placeholders to the data type: an address identicon is not a fallback for a token or chain logo. For a title-and-search header, check `common/view/TitledSearchToolbar` first.

When applying TextAppearance during view binding, preserve the text's semantic color: use the existing Secondary style variants for captions and fiat values. A base text appearance can overwrite the color declared in layout XML; check the final bound style, not only the XML attributes. Validator labels and Est. APY captions remain secondary while only the percentage uses its positive or negative color.

Use injected `FiatFormatter` for fiat amounts, not deprecated `formatAsCurrency` extensions. Read native-token symbols and precision from the actual chain asset; never hardcode display tickers in Kotlin, runtime XML text, or string resources. This includes rates, amount presets, currency switches, and descriptions; pass symbols as formatting parameters. Subnet-token symbols come from subnet metadata, not the native asset.

Define chart-formatting extension points as named interfaces alongside the shared chart component rather than exposing formatting lambdas. Keep feature-specific formatting implementations outside the shared chart code.

Reuse `ExternalActions.showAddressActions` and `setupExternalActions` for address information: they provide the standard sheet with copying and explorer actions. In selection lists, the information action must not also select the row or confirm the selection.

Use the standard `RadioButton` with `wrap_content` dimensions, following existing selection screens. Fixed bounds copied from a design can clip the theme drawable; do not shrink the control's bounds to match the visible circle.

## String resources and localization

Keep string resources in `strings.xml`, not feature-specific resource files. During feature development, add source strings only to `common/src/main/res/values/strings.xml`. Do not add manual translations to locale-specific resources: translations are handled by the established pre-release pipeline.

## Tables inside cards

When a `TableView` is inside a card with its own background, disable the table background with `app:showBackground="false"`. Setting `android:background` is insufficient because the component overrides it during construction. Do not introduce nested containers absent from the design.

Use `TableView` to own row separators, including expandable row groups, so the final visible row has no trailing divider. A plain `LinearLayout` does not disable `TableCellView` legacy dividers; `shouldDrawDivider` alone does not hide a divider that was already made visible during attribute initialization.

## Sectioned lists

Sections belonging to one selection list must scroll together in one `RecyclerView`, including their headers and rows. Do not pin the first section or its card above the list without an explicit requirement. Reuse `common/list/GroupedListAdapter` and `toListWithHeaders`, with separate view types for distinct row types. A search header may remain pinned.

Show initial list loading in the list content area, not above its controls. Reuse `CustomPlaceholderAdapter` and existing shimmer shapes for skeleton rows; replace placeholders with the real grouped content when loading completes, and stop showing them on failure.

## Navigation

Staking deep links carry an optional chain-asset target in the dashboard Fragment arguments. Wait until that asset is present and its staking sync has settled before deciding between existing-position details and starting staking. Consume the target before navigation and cancel pending automatic navigation when the user chooses a destination manually.

Follow existing screen navigation patterns: expose typed `Parcelable` payloads in router methods, build arguments through the destination Fragment's `getBundle(payload)`, and read the payload in that Fragment before binding it into the screen DI component. Do not scatter argument keys or parse individual Bundle values in navigators and DI modules. Pass operation inputs between screens in fragment payloads, not feature-scoped mutable draft/selection holders. Confirmation must reconstruct its operation from its own arguments, including the explicit wallet, amount, and validator. Pass selector inputs in its fragment arguments as well; keep the established communicator for returning the selection result. Repository caches and screen-local editable/execution state are not navigation payloads.

Preserve navigation XML organization: keep the leading include block together, group global actions with actions and related destinations together, and do not prepend feature nodes ahead of includes. Match neighboring XML indentation, place attributes on separate lines, and separate sibling declarations with blank lines.

## Screen and state binding

Reuse BaseFragment and its binder, injection, initViews and subscribe hooks. Check the base delegate
for actual lifecycle order; do not infer execution order from the order of method declarations.
Use existing observers, BaseListAdapter/GroupedListAdapter, SingleItemAdapter and ConcatAdapter.
Return selections through InterScreenCommunicator; navigation implementations and graphs belong to app.

Within staking, inspect `StakeStatusView` / `StakeStatusModel` and `Widget.Nova.StakingStatus` first;
master uses these in `item_dashboard_has_stake.xml`. Do not replace a status badge with a colored
subtitle just because it is quicker. Outside staking, use an existing accessible counterpart rather
than importing another feature's implementation. Inspect the widget and bound state first.
For loading/error/empty/value rendering also read [data and state](data-and-state.md).
Preserve accessibility, touch targets, font scaling, insets and long/localized content. Use resources
for dimensions/styles where shared; do not shrink RadioButton bounds to imitate a screenshot.
For requested iOS parity, inspect the explicit iOS source/design first and map behavior to existing
Android widgets; do not infer parity from a screenshot or claim it without evidence.
Check the screen after binding: TextAppearance and custom widgets can override XML attributes.
