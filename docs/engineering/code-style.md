# Code style and focused refactoring

Read .editorconfig and the current Gradle lint configuration; they are the executable formatting baseline.
The root ktlint task uses Android rules and scans main sources, not tests. Do not introduce a second formatter.
Do not reorder imports or rename files just to enforce rules disabled by this project.
Context receivers are already used; do not migrate language features as incidental cleanup.

Use existing numeric sign extensions (`isPositive`, `isNegative`, `isZero`, `isNonNegative`, and `isNonPositive`) instead of inline `signum()` comparisons or local duplicates. Respect their existing property/function syntax for each numeric type. Apply the established project conventions proactively when reviewing nearby code, not only to explicitly reported lines.

Give variables, classes, and functions clear, descriptive names that convey their purpose. Balance name length with the context it provides: avoid ambiguous abbreviations, but do not repeat information already clear from the surrounding scope. Use meaningful extension functions for type-related predicates and transformations when they make call sites read in domain terms, for example `value.isKnownAndZero()` instead of repeating a sealed-state check and a numeric check. Keep extensions close to their owning type or feature; do not create wrappers for already clear expressions or hide side effects in predicates.

Make protocol units explicit in names (for example, raw u16 commission versus a decimal commission rate). Use named protocol constants or standard numeric bounds instead of unexplained literals, and keep unit conversions in focused helpers. Preserve wire-field names at the API/storage boundary; use descriptive domain terminology beyond it.

Keep domain invariants in their owning types. Do not repeat constructor range checks in callers; handle construction failures at the input boundary according to the existing error policy, preserving whether invalid entries are skipped or the whole input is rejected.

Separate class property declarations with a blank line, following existing ViewModel references. Apply this to both single-line properties and multi-line flow declarations.

Order class members as properties, init blocks, then functions. Do not interleave methods or init blocks with property declarations. When moving init blocks, check initialization dependencies and side effects first; preserve property initialization order and the relative order of multiple init blocks.

For calls with multiple arguments that include a lambda or are followed by a trailing lambda, put each parenthesized argument on its own line. Keep the closing parenthesis on a separate line; do not compress such calls just because they fit the line-length limit.

Use named arguments for `null`, empty collections/arrays, and other non-self-describing literals or placeholder values so the parameter's role is clear at the call site. Descriptively named variables may remain positional; do not require names for every argument merely because another argument is named. When mixing named and positional arguments, keep declaration order. For APIs that do not support named arguments (such as Java methods), use a descriptive local value when the argument would otherwise be ambiguous.

```kotlin
mapBittensorDashboardDetails(
    positions = null,
    rootYields = emptyMap(),
    subnetYieldsCached,
    recommendedYieldsFiltered
)
```

Put successive transformation calls in a method chain on separate lines when this improves readability. Keep short expressions, simple property access, and short predicate calls together; do not mechanically split expressions such as `finalizedBlock(chainId).number` or `hash.equals(transactionHash, ignoreCase = true)`. Keep a short call and its arguments on one line when they fit naturally. When an `if` condition contains a multi-line collection predicate or nested transformation, extract a descriptively named Boolean before the `if` so the control flow remains immediately visible.

Separate fetching a remote response from transforming its contents. Break large methods that mix source construction, mapping, state transitions, and persistence into focused, descriptively named functions. The entry method should read as orchestration, and a `combine` lambda should assemble results instead of owning whole workflows. Do not merely move the entire large body into another method or introduce wrappers without reducing complexity. Preserve subscription topology, state ownership, emission order, cancellation, and side-effect timing during extraction. Extract nested transformations and error handling into focused functions. Use the existing `mapAsync` helper for concurrent collection mapping instead of manually combining `async` and `awaitAll`; preserve required concurrency limits and cancellation propagation.

Do not repeat filtering already guaranteed by a backend resource contract. Verify that contract before removing a filter; keep client-owned validation, such as address decoding and on-chain eligibility checks.

Model finite UI choices with enums or sealed types instead of magic integers. Keep semantic options such as `MAX` distinct from numeric amounts; do not encode them as a sentinel amount such as zero or rely on enum ordinals.

Refactor to solve a concrete correctness, ownership, consistency, duplication, testability, or complexity problem, not personal stylistic preference. Inspect existing implementations first. Keep authoritative state in one place and derive dependent values; inject dependencies rather than constructing them inside consumers. Distinguish unavailable data from zero or empty data, and do not disguise mocks or incomplete integrations as working functionality.

Prioritize operation correctness, then state and error handling, then architecture and duplication, then readability. For each change, identify the problem, the intended improvement, and the verification. Separate behavior changes from behavior-preserving refactoring. Avoid unrelated cleanup and speculative abstractions in `common`.

Prefer named models for meaningful requests and load results over positional `Pair`/`Triple` values. Use scope functions for short transformations; keep validation, mutation and operation sequencing explicit. Construct standard loading, empty, or error states with clearly named factories rather than passing invented null/empty inputs to a business mapper and patching the result with `copy`. A `mapper(...).copy(...)` chain is a design warning: provide the mapper with all inputs needed to construct its final result instead of building and immediately repairing an intermediate model. Use `copy` deliberately for a real immutable update or focused test-fixture variation, not as a staged builder; do not replace it with a field-by-field constructor that merely hides the same incomplete mapping. Do not manufacture zero-valued data merely to call a formatter and erase the result afterwards.
