# Maintaining project instructions

## One source of truth

AGENTS.md is the small entry point; CLAUDE.md contains only `@AGENTS.md`.
Topic documents are mandatory when routed by the task, not an automatically loaded encyclopedia.
Keep root AGENTS under 100 lines / 8 KiB. When it grows, move detail to an existing topic, not another summary.
Keep general topics under 12 KiB each; domain-heavy Bittensor constraints have a 20 KiB budget.
A budget is a prompt to consolidate, not permission to delete safeguards. Do not duplicate the same rule.

## User feedback → durable rule

When the user corrects a reusable convention (for example “use this status widget”):

Apply the updates below during an authorized change task. A review/diagnosis-only request permits a proposed
rule update, not a file edit, unless the user separately authorizes the update.

1. Inspect the named component/pattern and determine whether the rule is general, feature-specific or one-off.
2. Update the existing owning topic in the same task; replace the old rule instead of appending a competing one.
3. Include the rationale/exception only when it prevents ambiguity; add a verified example where useful.
4. Add a check/test if the rule can be enforced reliably. Do not use broad regexes as proof of architecture.
5. Check links/contradictions and briefly tell the user which instruction changed.

A one-off screen choice belongs to that feature/task, not global UI policy. Ask if scope is ambiguous.
Do not promote an agent preference or a backend accident to a universal rule.
Do not update personal/global agent instructions as part of repository maintenance.

## Sources and conflicts

Current explicit user direction wins over repository conventions. Written current conventions win over
legacy example quirks; record narrow exceptions in the owning domain topic.
If two current written rules disagree and affect behavior, show the conflict and request the missing decision.
A master example is evidence for its stated aspect only, not “copy this file verbatim”.
Reference candidates remain proposed until approved by the user. Record source revision and exclusions.
Do not refresh reference patterns from Bittensor or another experimental feature implicitly.
During this baseline, master is the code-style source and RC is the source of existing explicit rules.

## Evolution

Only add a nested AGENTS when a directory has durable, genuinely local rules; avoid a forest of overrides.
Keep examples outside the root. Remove stale counts, versions and duplicate prose; link to executable config.
When renaming/removing docs, update routes and links, run the checker and review the resulting read paths.
Use [the migration record](migration.md) to audit the initial move; it is history, not a second rulebook.
