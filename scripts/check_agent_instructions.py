"""Small repository guardrails, not a Kotlin/Gradle architecture analyzer."""
import argparse
from pathlib import Path
import re
import subprocess
import sys

LINK = re.compile(r"(?<!!)\[[^\]\n]+\]\(([^)\n]+)\)")
REFERENCE_FILE = re.compile(r"<!-- reference-file: ([^\n]+) -->")
REFERENCE_REVISION = re.compile(r"<!-- reference-revision: ([0-9a-f]{40}) -->")
PROJECT = re.compile(r"""project\(\s*(?:path\s*[:=]\s*)?["'](:[^"']+)["']""")
REQUIRED_TOPICS = {
    "architecture", "code-style", "workflow", "ui", "data-and-state",
    "transactions", "analytics", "infrastructure-auth", "bittensor",
    "testing", "instruction-maintenance", "reference-patterns", "migration",
}


def check(root, verify_reference_revision=False):
    errors = []
    entry = root / "AGENTS.md"
    bridge = root / "CLAUDE.md"
    topics = root / "docs/engineering"
    for path in (entry, bridge):
        if not path.is_file():
            errors.append(f"Missing {path.name}")
    if errors:
        return errors

    entry_text = entry.read_text(encoding="utf-8")
    if len(entry_text.splitlines()) > 100 or len(entry_text.encode("utf-8")) > 8192:
        errors.append("AGENTS.md exceeds the 100-line / 8 KiB routing budget")
    if bridge.read_text(encoding="utf-8").strip() != "@AGENTS.md":
        errors.append("CLAUDE.md must contain only @AGENTS.md")

    discovered_topics = {path.stem for path in topics.glob("*.md")}
    for name in sorted(REQUIRED_TOPICS | discovered_topics):
        relative = f"docs/engineering/{name}.md"
        if not (root / relative).is_file():
            errors.append(f"Missing topic: {relative}")
        if relative not in entry_text:
            errors.append(f"Unrouted topic: {relative}")

    documents = [entry, *sorted(topics.glob("*.md"))]
    for path in documents:
        content = path.read_text(encoding="utf-8")
        budget = 20480 if path.name == "bittensor.md" else 12288
        if path != entry and len(content.encode("utf-8")) > budget:
            errors.append(f"Topic exceeds {budget} byte budget: {path.relative_to(root)}")
        # Check document links, not sample code. Anchors are not interpreted.
        prose = re.sub(r"```[^\n]*\n.*?\n```", "", content, flags=re.S)
        for link in LINK.findall(prose):
            target = link.split("#", 1)[0]
            if not target or re.match(r"[a-zA-Z][a-zA-Z0-9+.-]*:", target):
                continue
            resolved = (path.parent / target).resolve()
            if not resolved.is_relative_to(root.resolve()) or not resolved.exists():
                errors.append(f"Broken/local-outside link in {path.relative_to(root)}: {link}")

    references = topics / "reference-patterns.md"
    if references.is_file():
        content = references.read_text(encoding="utf-8")
        revision = REFERENCE_REVISION.search(content)
        paths = REFERENCE_FILE.findall(content)
        if not revision or not paths:
            errors.append("Reference catalog needs a pinned revision and reference-file markers")
        elif verify_reference_revision:
            for path in paths:
                result = subprocess.run(
                    ["git", "-C", str(root), "cat-file", "-t", f"{revision[1]}:{path}"],
                    capture_output=True, text=True, check=False,
                )
                if result.returncode or result.stdout.strip() != "blob":
                    errors.append(f"Pinned reference unavailable: {path} at {revision[1]}")

    # Deliberately limited to literal project(...) dependencies. Dynamic Gradle
    # expressions and source-level ownership still require compilation and review.
    for pattern in ("feature-*/**/build.gradle", "feature-*/**/build.gradle.kts"):
        for path in sorted(root.glob(pattern)):
            content = path.read_text(encoding="utf-8")
            content = re.sub(r"/\*.*?\*/", "", content, flags=re.S)
            content = re.sub(r"//[^\n]*", "", content)
            for dependency in PROJECT.findall(content):
                if dependency.startswith(":feature-") and dependency.endswith("-impl"):
                    errors.append(f"Feature implementation dependency in {path.relative_to(root)}: {dependency}")
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--verify-reference-revision", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    errors = check(root, args.verify_reference_revision)
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("PASS: instruction routing, local links, budgets, Claude bridge and literal feature boundaries")
    if args.verify_reference_revision:
        print("PASS: pinned reference objects exist locally")
    return 0


if __name__ == "__main__":
    sys.exit(main())
