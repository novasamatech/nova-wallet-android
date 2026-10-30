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
ADDITIONAL_GUIDES = ("docs/adding-a-feature-module.md", "docs/swap-service-commission.md")


def without_fenced_code(content):
    prose = []
    opening = None
    for line in content.splitlines():
        fence = re.match(r"^ {0,3}(`{3,}|~{3,})(.*)$", line)
        if opening is not None:
            if (fence and fence[1][0] == opening[0] and len(fence[1]) >= len(opening)
                    and not fence[2].strip()):
                opening = None
        elif fence and not (fence[1][0] == "`" and "`" in fence[2]):
            opening = fence[1]
        else:
            prose.append(line)
    return "\n".join(prose)


def document_links(content):
    # Only prose links are reading routes. This supports the inline Markdown
    # links used by these docs, not every Markdown extension or reference syntax.
    prose = without_fenced_code(content)
    prose = re.sub(r"<!--.*?-->", "", prose, flags=re.S)
    # Preserve a nonempty label in [`code`](target), but mask links inside code spans.
    prose = re.sub(r"`+[^`\n]*`+", "code", prose)
    return LINK.findall(prose)


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

    routes = {link.split("#", 1)[0] for link in document_links(entry_text)}
    discovered_topics = {path.stem for path in topics.glob("*.md")}
    for name in sorted(REQUIRED_TOPICS | discovered_topics):
        relative = f"docs/engineering/{name}.md"
        if not (root / relative).is_file():
            errors.append(f"Missing topic: {relative}")
        if relative not in routes:
            errors.append(f"Unrouted topic: {relative}")

    documents = [entry, *sorted(topics.glob("*.md"))]
    for relative in ADDITIONAL_GUIDES:
        if relative not in routes:
            errors.append(f"Unrouted guide: {relative}")
        if not (root / relative).is_file():
            errors.append(f"Missing guide: {relative}")
        else:
            documents.append(root / relative)
    for path in documents:
        content = path.read_text(encoding="utf-8")
        budget = 20480 if path.name == "bittensor.md" else 12288
        if path != entry and len(content.encode("utf-8")) > budget:
            errors.append(f"Topic exceeds {budget} byte budget: {path.relative_to(root)}")
        # Anchors are not interpreted.
        for link in document_links(content):
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
