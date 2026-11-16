# Delivery workflow

Use these passes within one task; do not ask the user to request a separate refactoring round.
Scale effort to risk. A label change does not need a design document; financial changes do need invariant tests.
Implementation/refactoring applies only to authorized change tasks. For review-only, diagnosis or explanation,
read and check the relevant code, then report findings and proposed fixes; do not edit files.
Feedback-driven instruction maintenance follows the same boundary: propose it during read-only work.

## Before implementation

1. Confirm scope, checkout and uncommitted work; read routed instructions.
2. Inspect a comparable master implementation and the shared APIs/components it actually uses.
   Consult [reference patterns](reference-patterns.md); an example is not blanket approval of every line.
3. Identify ownership and data flow: input/payload → domain operation → repository/source → presentation.
   Specify where state lives, how it resets, and how errors/cancellation propagate.
   For nontrivial classes, outline responsibilities and the main method steps before implementation.
   Name the relevant existing rules and the behavior that must survive decomposition; keep this brief in the task.
4. Write a short acceptance/checklist plan for nontrivial work: desired behavior, invariants, boundaries,
   verification and non-goals. Keep it in the task unless a reusable design record is needed.
5. Resolve conflicting rules or missing product choices before encoding them as new conventions.

## Implementation and verification

Implement an end-to-end slice using existing contracts. Add regression tests for bugs and appropriate
tests for new behavior. See [testing](testing.md). Preserve unrelated work.
Prefer the established solution over a novel framework; avoid both duplicated logic and unnecessary layers.

Review the final changed files and callers, not only the newly added lines, in separate passes in this order:

- **Correctness:** inputs, financial units/rounding, boundary cases, same operation at quote/sign time.
- **State/lifecycle:** one owner, explicit wallet/request identity, stale results, cancellation, retries,
  loading vs empty vs unavailable, screen recreation and navigation where applicable.
- **Architecture/reuse:** each responsibility in the correct layer; DI; existing component/mixin;
  no transport in UI, business rules in mappers, or hidden state mutation in getters.
- **Simplicity/readability:** read entry methods as a sequence of named domain steps. Identify mixed
  responsibilities, nested transformations, business rules hidden in lambdas, and names reused for different
  concepts. Apply [code style](code-style.md) and [flow decomposition](data-and-state.md) with focused methods;
  preserve subscription lifetime, cancellation and ordering. Check for speculative abstractions, redundant
  state, needless wrappers, copied formatters, or checks already owned by a type. Build, test and lint success
  do not replace this pass.

For each finding, name the concrete problem, fix and verification. Refactor within scope before handoff;
separate behavior changes from behavior-preserving edits. Rerun affected checks after the last fix.
Stop when acceptance and review checks pass; do not churn working code to satisfy personal taste.

## Independent review

For substantial or high-risk work, seek a separate review before merge/release. If delegation is authorized,
give the reviewer the task, full diff, relevant rules and test results; ask for correctness AND ownership,
reuse and simplification findings. The reviewer must not treat author's explanations as proof.
Without authorization or another reviewer, perform the passes above and explicitly report that independent
review remains pending. Do not label another pass by the same agent “independent”.

Human review is still needed for financial, signing, security, migrations and changed public contracts.
No instruction file can guarantee that generated code is correct.

## Handoff

State what changed, what was reused, checks actually run (and their outcome), and anything not verified.
Separate JVM/static/build evidence from device, live API, process restoration and on-chain evidence.
Do not say “all tested” when only compilation passed; do not install or submit merely to complete a checklist.
For nontrivial changes, include a short, concrete result of the structural review: which responsibilities
were separated and why, or what was reviewed with no remaining findings. Do not substitute a generic
claim that instructions were followed for this evidence.
If the user corrected a convention, follow [instruction maintenance](instruction-maintenance.md).

## Pilot / acceptance of these instructions

For a small next feature, choose its routes and reference aspects before editing. Evaluate the complete
diff with the review checklist and count concrete remaining findings, not how confidently an agent answered.
Repeat in Codex and Claude when available. Record which instructions were missed or ambiguous, improve the
owning topic, and rerun. A documentation checker alone cannot prove real agent compliance.
